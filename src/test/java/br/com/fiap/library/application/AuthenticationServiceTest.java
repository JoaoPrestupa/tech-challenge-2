package br.com.fiap.library.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.library.application.model.AccessToken;
import br.com.fiap.library.application.model.AuthTokens;
import br.com.fiap.library.application.port.in.UserManagement;
import br.com.fiap.library.application.port.out.AccessTokenIssuer;
import br.com.fiap.library.application.port.out.OpaqueTokenGenerator;
import br.com.fiap.library.application.port.out.PasswordHasher;
import br.com.fiap.library.application.port.out.RefreshSessionRepository;
import br.com.fiap.library.application.port.out.UserRepository;
import br.com.fiap.library.application.usecase.AuthenticationService;
import br.com.fiap.library.domain.exception.AuthenticationFailedException;
import br.com.fiap.library.domain.model.LibraryUser;
import br.com.fiap.library.domain.model.RefreshSession;
import br.com.fiap.library.domain.model.Role;
import br.com.fiap.library.domain.model.UserStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuthenticationServiceTest {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Instant NOW = Instant.parse("2026-11-02T13:00:00Z");

    private final UserRepository users = mock();
    private final UserManagement userManagement = mock();
    private final RefreshSessionRepository sessions = mock();
    private final PasswordHasher hasher = mock();
    private final AccessTokenIssuer accessTokens = mock();
    private final OpaqueTokenGenerator refreshTokens = mock();
    private AuthenticationService service;

    private final LibraryUser user = new LibraryUser(1L, "Maria", "maria@example.com", "stored-hash",
            UserStatus.ACTIVE, Set.of(Role.ROLE_MEMBER));

    @BeforeEach
    void setUp() {
        lenient().when(hasher.hash(any())).thenReturn("dummy-hash");
        lenient().when(refreshTokens.generate()).thenReturn("new-refresh");
        lenient().when(refreshTokens.hash(any())).thenAnswer(call -> "hash:" + call.getArgument(0));
        lenient().when(accessTokens.issue(any())).thenReturn(new AccessToken("jwt", NOW.plusSeconds(900)));
        lenient().when(sessions.save(any())).thenAnswer(call -> {
            RefreshSession s = call.getArgument(0);
            return new RefreshSession(s.id() == null ? 50L : s.id(), s.userId(), s.tokenHash(), s.issuedAt(),
                    s.expiresAt(), s.revokedAt(), s.replacedBy());
        });
        service = new AuthenticationService(users, userManagement, sessions, hasher, accessTokens, refreshTokens,
                new DirectUnitOfWork(), Clock.fixed(NOW, ZONE), Duration.ofDays(7));
    }

    private RefreshSession session(Instant expiresAt, Instant revokedAt) {
        return new RefreshSession(40L, 1L, "hash:old", NOW.minusSeconds(60), expiresAt, revokedAt, null);
    }

    @Test
    void register_always_creates_a_member() {
        service.register("Maria", "maria@example.com", "senha-segura");

        verify(userManagement).create("Maria", "maria@example.com", "senha-segura", Set.of(Role.ROLE_MEMBER));
    }

    @Test
    void login_returns_tokens_and_stores_only_the_refresh_hash() {
        when(users.findByEmail("maria@example.com")).thenReturn(Optional.of(user));
        when(hasher.matches("senha-segura", "stored-hash")).thenReturn(true);

        AuthTokens tokens = service.login("MARIA@example.com", "senha-segura");

        assertThat(tokens.accessToken()).isEqualTo("jwt");
        assertThat(tokens.refreshToken()).isEqualTo("new-refresh");
        ArgumentCaptor<RefreshSession> saved = ArgumentCaptor.forClass(RefreshSession.class);
        verify(sessions).save(saved.capture());
        assertThat(saved.getValue().tokenHash()).isEqualTo("hash:new-refresh");
    }

    @Test
    void wrong_password_fails() {
        when(users.findByEmail("maria@example.com")).thenReturn(Optional.of(user));
        when(hasher.matches("senha-errada", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login("maria@example.com", "senha-errada"))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void unknown_email_still_runs_the_hash_comparison() {
        when(users.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("ninguem@example.com", "senha-segura"))
                .isInstanceOf(AuthenticationFailedException.class);
        verify(hasher).matches("senha-segura", "dummy-hash");
    }

    @Test
    void inactive_user_cannot_log_in() {
        LibraryUser inactive = user.deactivate();
        when(users.findByEmail("maria@example.com")).thenReturn(Optional.of(inactive));
        when(hasher.matches("senha-segura", "stored-hash")).thenReturn(true);

        assertThatThrownBy(() -> service.login("maria@example.com", "senha-segura"))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void refresh_rotates_the_token() {
        when(sessions.findByTokenHashForUpdate("hash:old")).thenReturn(Optional.of(session(NOW.plusSeconds(60), null)));
        when(users.findById(1L)).thenReturn(Optional.of(user));

        AuthTokens tokens = service.refresh("old");

        assertThat(tokens.refreshToken()).isEqualTo("new-refresh");
        ArgumentCaptor<RefreshSession> saved = ArgumentCaptor.forClass(RefreshSession.class);
        verify(sessions, org.mockito.Mockito.times(2)).save(saved.capture());
        RefreshSession old = saved.getAllValues().get(1);
        assertThat(old.id()).isEqualTo(40L);
        assertThat(old.revokedAt()).isEqualTo(NOW);
        assertThat(old.replacedBy()).isEqualTo(50L);
    }

    @Test
    void reusing_a_revoked_token_ends_all_sessions_of_the_user() {
        when(sessions.findByTokenHashForUpdate("hash:old"))
                .thenReturn(Optional.of(session(NOW.plusSeconds(60), NOW.minusSeconds(1))));

        assertThatThrownBy(() -> service.refresh("old")).isInstanceOf(AuthenticationFailedException.class);
        verify(sessions).revokeAllByUser(1L, NOW);
    }

    @Test
    void expired_token_is_rejected() {
        when(sessions.findByTokenHashForUpdate("hash:old")).thenReturn(Optional.of(session(NOW, null)));
        when(users.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.refresh("old")).isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void unknown_token_is_rejected() {
        when(sessions.findByTokenHashForUpdate(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("x")).isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void logout_revokes_the_session() {
        when(sessions.findByTokenHashForUpdate("hash:old")).thenReturn(Optional.of(session(NOW.plusSeconds(60), null)));

        service.logout("old");

        verify(sessions).save(org.mockito.ArgumentMatchers.argThat(RefreshSession::isRevoked));
    }

    @Test
    void change_password_requires_the_current_one() {
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(hasher.matches("senha-errada", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(1L, "senha-errada", "nova-senha-segura"))
                .isInstanceOf(AuthenticationFailedException.class);
        verify(users, never()).save(any());
    }

    @Test
    void change_password_revokes_all_sessions() {
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(hasher.matches("senha-segura", "stored-hash")).thenReturn(true);

        service.changePassword(1L, "senha-segura", "nova-senha-segura");

        verify(users).save(org.mockito.ArgumentMatchers.argThat(u -> u.passwordHash().equals("dummy-hash")));
        verify(sessions).revokeAllByUser(1L, NOW);
    }
}
