package br.com.fiap.library.application.usecase;

import br.com.fiap.library.application.model.Actor;
import br.com.fiap.library.application.model.LoanFilter;
import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.application.port.in.UserManagement;
import br.com.fiap.library.application.port.out.LoanRepository;
import br.com.fiap.library.application.port.out.PasswordHasher;
import br.com.fiap.library.application.port.out.RefreshSessionRepository;
import br.com.fiap.library.application.port.out.ReservationRepository;
import br.com.fiap.library.application.port.out.UnitOfWork;
import br.com.fiap.library.application.port.out.UserRepository;
import br.com.fiap.library.domain.exception.BusinessRuleException;
import br.com.fiap.library.domain.exception.ConflictException;
import br.com.fiap.library.domain.exception.ForbiddenException;
import br.com.fiap.library.domain.exception.NotFoundException;
import br.com.fiap.library.domain.model.EmailAddress;
import br.com.fiap.library.domain.model.LibraryUser;
import br.com.fiap.library.domain.model.PasswordPolicy;
import br.com.fiap.library.domain.model.Reservation;
import br.com.fiap.library.domain.model.Role;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;

public class UserService implements UserManagement {
    private final UserRepository users;
    private final LoanRepository loans;
    private final ReservationRepository reservations;
    private final RefreshSessionRepository sessions;
    private final ReservationQueue queue;
    private final PasswordHasher hasher;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public UserService(UserRepository users, LoanRepository loans, ReservationRepository reservations,
                       RefreshSessionRepository sessions, ReservationQueue queue, PasswordHasher hasher,
                       UnitOfWork unitOfWork, Clock clock) {
        this.users = users;
        this.loans = loans;
        this.reservations = reservations;
        this.sessions = sessions;
        this.queue = queue;
        this.hasher = hasher;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public LibraryUser create(String name, String email, String password, Set<Role> roles) {
        PasswordPolicy.validate(password);
        Set<Role> effective = roles == null || roles.isEmpty() ? Set.of(Role.ROLE_MEMBER) : roles;
        String hash = hasher.hash(password);
        return unitOfWork.inTransaction(() -> {
            if (users.existsByEmail(EmailAddress.normalize(email))) {
                throw new ConflictException("E-mail já cadastrado");
            }
            return users.save(LibraryUser.create(name, email, hash, effective));
        });
    }

    @Override
    public LibraryUser get(Actor actor, Long id) {
        actor.requireSelfOrLibrarian(id);
        return require(id);
    }

    @Override
    public PageResult<LibraryUser> list(PageQuery page) {
        return users.findAll(page);
    }

    @Override
    public LibraryUser update(Actor actor, Long id, String name, String email, Set<Role> roles) {
        actor.requireSelfOrLibrarian(id);
        if (roles != null && !actor.isLibrarian()) {
            throw new ForbiddenException("Somente bibliotecários podem alterar roles");
        }
        return unitOfWork.inTransaction(() -> {
            LibraryUser current = users.findByIdForUpdate(id)
                    .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + id));
            LibraryUser updated = current.updateProfile(name, email);
            if (users.existsByEmailAndIdNot(updated.email(), id)) {
                throw new ConflictException("E-mail já cadastrado");
            }
            if (roles != null) {
                updated = updated.withRoles(roles);
                if (current.isLibrarian() && !updated.isLibrarian()) {
                    requireAnotherLibrarian();
                }
            }
            return users.save(updated);
        });
    }

    /** Usuários nunca são apagados fisicamente, para preservar o histórico de empréstimos. */
    @Override
    public void deactivate(Actor actor, Long id) {
        actor.requireLibrarian();
        unitOfWork.run(() -> {
            LibraryUser user = users.findByIdForUpdate(id)
                    .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + id));
            if (user.isLibrarian() && user.isActive()) {
                requireAnotherLibrarian();
            }
            if (loans.search(new LoanFilter(id, true), new PageQuery(0, 1, null, true)).totalElements() > 0) {
                throw new BusinessRuleException("O usuário possui empréstimos ativos");
            }
            Instant now = clock.instant();
            for (Reservation reservation : reservations.findOpenByUser(id)) {
                queue.close(reservation.cancel(), now);
            }
            sessions.revokeAllByUser(id, now);
            users.save(user.deactivate());
        });
    }

    @Override
    public boolean bootstrapLibrarian(String name, String email, String password) {
        if (users.countActiveLibrarians() > 0) {
            return false;
        }
        create(name, email, password, Set.of(Role.ROLE_LIBRARIAN));
        return true;
    }

    private void requireAnotherLibrarian() {
        if (users.countActiveLibrarians() <= 1) {
            throw new BusinessRuleException("A biblioteca precisa de ao menos um bibliotecário ativo");
        }
    }

    private LibraryUser require(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + id));
    }
}
