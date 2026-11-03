package br.com.fiap.library.application.usecase;

import br.com.fiap.library.application.model.AccessToken;
import br.com.fiap.library.application.model.AuthTokens;
import br.com.fiap.library.application.port.in.Authentication;
import br.com.fiap.library.application.port.in.UserManagement;
import br.com.fiap.library.application.port.out.AccessTokenIssuer;
import br.com.fiap.library.application.port.out.OpaqueTokenGenerator;
import br.com.fiap.library.application.port.out.PasswordHasher;
import br.com.fiap.library.application.port.out.RefreshSessionRepository;
import br.com.fiap.library.application.port.out.UnitOfWork;
import br.com.fiap.library.application.port.out.UserRepository;
import br.com.fiap.library.domain.exception.AuthenticationFailedException;
import br.com.fiap.library.domain.exception.NotFoundException;
import br.com.fiap.library.domain.model.EmailAddress;
import br.com.fiap.library.domain.model.LibraryUser;
import br.com.fiap.library.domain.model.PasswordPolicy;
import br.com.fiap.library.domain.model.RefreshSession;
import br.com.fiap.library.domain.model.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public class AuthenticationService implements Authentication {
    private static final String INVALID_CREDENTIALS = "E-mail ou senha inválidos";
    private static final String INVALID_REFRESH = "Refresh token inválido ou expirado";

    private final UserRepository users;
    private final UserManagement userManagement;
    private final RefreshSessionRepository sessions;
    private final PasswordHasher hasher;
    private final AccessTokenIssuer accessTokens;
    private final OpaqueTokenGenerator refreshTokens;
    private final UnitOfWork unitOfWork;
    private final Clock clock;
    private final Duration refreshTtl;
    /** Mantém o tempo de resposta parecido quando o e-mail não existe, dificultando enumerar contas. */
    private final String dummyHash;

    public AuthenticationService(UserRepository users, UserManagement userManagement,
                                 RefreshSessionRepository sessions, PasswordHasher hasher,
                                 AccessTokenIssuer accessTokens, OpaqueTokenGenerator refreshTokens,
                                 UnitOfWork unitOfWork, Clock clock, Duration refreshTtl) {
        this.users = users;
        this.userManagement = userManagement;
        this.sessions = sessions;
        this.hasher = hasher;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
        this.refreshTtl = refreshTtl;
        this.dummyHash = hasher.hash("senha-ficticia-para-equalizar-tempo");
    }

    /** O registro público sempre cria um membro: a role não vem do cliente. */
    @Override
    public LibraryUser register(String name, String email, String password) {
        return userManagement.create(name, email, password, Set.of(Role.ROLE_MEMBER));
    }

    @Override
    public AuthTokens login(String email, String password) {
        String normalized;
        try {
            normalized = EmailAddress.normalize(email);
        } catch (RuntimeException invalidFormat) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        if (!PasswordPolicy.accepts(password)) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        Optional<LibraryUser> found = users.findByEmail(normalized);
        boolean matches = hasher.matches(password, found.map(LibraryUser::passwordHash).orElse(dummyHash));
        LibraryUser user = found.filter(u -> matches && u.isActive())
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_CREDENTIALS));
        return unitOfWork.inTransaction(() -> startSession(user, clock.instant()).tokens());
    }

    /**
     * Rotação: cada refresh token vale uma vez. Reapresentar um token já revogado indica possível
     * roubo, então todas as sessões do usuário são encerradas. A falha é lançada fora da transação
     * para que essa revogação não sofra rollback.
     */
    @Override
    public AuthTokens refresh(String refreshToken) {
        String hash = refreshTokens.hash(refreshToken == null ? "" : refreshToken);
        Optional<AuthTokens> tokens = unitOfWork.inTransaction(() -> {
            Instant now = clock.instant();
            Optional<RefreshSession> found = sessions.findByTokenHashForUpdate(hash);
            if (found.isEmpty()) {
                return Optional.empty();
            }
            RefreshSession session = found.get();
            if (session.isRevoked()) {
                sessions.revokeAllByUser(session.userId(), now);
                return Optional.empty();
            }
            Optional<LibraryUser> user = users.findById(session.userId()).filter(LibraryUser::isActive);
            if (session.isExpired(now) || user.isEmpty()) {
                return Optional.empty();
            }
            StartedSession started = startSession(user.get(), now);
            sessions.save(session.revoke(now, started.sessionId()));
            return Optional.of(started.tokens());
        });
        return tokens.orElseThrow(() -> new AuthenticationFailedException(INVALID_REFRESH));
    }

    @Override
    public void logout(String refreshToken) {
        String hash = refreshTokens.hash(refreshToken == null ? "" : refreshToken);
        unitOfWork.run(() -> sessions.findByTokenHashForUpdate(hash)
                .filter(session -> !session.isRevoked())
                .ifPresent(session -> sessions.save(session.revoke(clock.instant(), null))));
    }

    @Override
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        PasswordPolicy.validate(newPassword);
        LibraryUser user = me(userId);
        if (!PasswordPolicy.accepts(currentPassword) || !hasher.matches(currentPassword, user.passwordHash())) {
            throw new AuthenticationFailedException("Senha atual incorreta");
        }
        String newHash = hasher.hash(newPassword);
        unitOfWork.run(() -> {
            users.save(user.withPasswordHash(newHash));
            sessions.revokeAllByUser(userId, clock.instant());
        });
    }

    @Override
    public LibraryUser me(Long userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + userId));
    }

    private StartedSession startSession(LibraryUser user, Instant now) {
        String rawRefresh = refreshTokens.generate();
        Instant refreshExpiresAt = now.plus(refreshTtl);
        RefreshSession session = sessions.save(
                RefreshSession.issue(user.id(), refreshTokens.hash(rawRefresh), now, refreshExpiresAt));
        AccessToken access = accessTokens.issue(user);
        return new StartedSession(session.id(),
                new AuthTokens(access.value(), access.expiresAt(), rawRefresh, refreshExpiresAt));
    }

    private record StartedSession(Long sessionId, AuthTokens tokens) {
    }
}
