package br.com.fiap.library.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.library.application.model.Actor;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.application.port.out.BookCopyRepository;
import br.com.fiap.library.application.port.out.LoanRepository;
import br.com.fiap.library.application.port.out.PasswordHasher;
import br.com.fiap.library.application.port.out.RefreshSessionRepository;
import br.com.fiap.library.application.port.out.ReservationRepository;
import br.com.fiap.library.application.port.out.UserRepository;
import br.com.fiap.library.application.usecase.ReservationQueue;
import br.com.fiap.library.application.usecase.UserService;
import br.com.fiap.library.domain.exception.BusinessRuleException;
import br.com.fiap.library.domain.exception.ConflictException;
import br.com.fiap.library.domain.exception.ForbiddenException;
import br.com.fiap.library.domain.exception.InvalidDataException;
import br.com.fiap.library.domain.model.LibraryUser;
import br.com.fiap.library.domain.model.Role;
import br.com.fiap.library.domain.model.UserStatus;
import br.com.fiap.library.domain.service.LibraryPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserServiceTest {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final UserRepository users = mock();
    private final LoanRepository loans = mock();
    private final ReservationRepository reservations = mock();
    private final RefreshSessionRepository sessions = mock();
    private final BookCopyRepository copies = mock();
    private final PasswordHasher hasher = mock();
    private final Actor member = new Actor(1L, Set.of(Role.ROLE_MEMBER));
    private final Actor librarian = new Actor(9L, Set.of(Role.ROLE_LIBRARIAN));
    private UserService service;

    @BeforeEach
    void setUp() {
        LibraryPolicy policy = new LibraryPolicy(14, 3, Duration.ofHours(48), ZONE);
        service = new UserService(users, loans, reservations, sessions,
                new ReservationQueue(reservations, copies, policy), hasher, new DirectUnitOfWork(),
                Clock.fixed(Instant.parse("2026-11-02T13:00:00Z"), ZONE));
        lenient().when(hasher.hash(any())).thenReturn("bcrypt-hash");
        lenient().when(users.save(any())).thenAnswer(call -> call.getArgument(0));
        lenient().when(loans.search(any(), any())).thenReturn(new PageResult<>(List.of(), 0, 1, 0, 0));
        lenient().when(reservations.findOpenByUser(any())).thenReturn(List.of());
    }

    private static LibraryUser user(Long id, Role role) {
        return new LibraryUser(id, "Maria", "maria@example.com", "hash", UserStatus.ACTIVE, Set.of(role));
    }

    @Test
    void stores_only_the_password_hash_and_normalizes_the_email() {
        LibraryUser created = service.create("Maria", " MARIA@Example.com ", "senha-segura", null);

        assertThat(created.passwordHash()).isEqualTo("bcrypt-hash");
        assertThat(created.email()).isEqualTo("maria@example.com");
        assertThat(created.roles()).containsExactly(Role.ROLE_MEMBER);
    }

    @Test
    void duplicate_email_is_a_conflict_after_normalization() {
        when(users.existsByEmail("maria@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create("Maria", "MARIA@example.com", "senha-segura", null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void weak_password_is_rejected() {
        assertThatThrownBy(() -> service.create("Maria", "maria@example.com", "curta", null))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void member_cannot_read_another_member() {
        assertThatThrownBy(() -> service.get(member, 2L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void member_cannot_change_own_roles() {
        assertThatThrownBy(() -> service.update(member, 1L, "Maria", "maria@example.com", Set.of(Role.ROLE_LIBRARIAN)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void member_can_update_own_profile() {
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user(1L, Role.ROLE_MEMBER)));

        assertThat(service.update(member, 1L, "Maria José", "maria@example.com", null).name()).isEqualTo("Maria José");
    }

    @Test
    void last_librarian_cannot_lose_the_role() {
        when(users.findByIdForUpdate(9L)).thenReturn(Optional.of(user(9L, Role.ROLE_LIBRARIAN)));
        when(users.countActiveLibrarians()).thenReturn(1L);

        assertThatThrownBy(() -> service.update(librarian, 9L, "Ana", "ana@example.com", Set.of(Role.ROLE_MEMBER)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void last_librarian_cannot_be_deactivated() {
        when(users.findByIdForUpdate(9L)).thenReturn(Optional.of(user(9L, Role.ROLE_LIBRARIAN)));
        when(users.countActiveLibrarians()).thenReturn(1L);

        assertThatThrownBy(() -> service.deactivate(librarian, 9L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void user_with_active_loans_cannot_be_deactivated() {
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user(1L, Role.ROLE_MEMBER)));
        when(loans.search(any(), any())).thenReturn(new PageResult<>(List.of(), 0, 1, 1, 1));

        assertThatThrownBy(() -> service.deactivate(librarian, 1L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void deactivating_a_user_revokes_the_sessions() {
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user(1L, Role.ROLE_MEMBER)));

        service.deactivate(librarian, 1L);

        verify(sessions).revokeAllByUser(any(), any());
        verify(users).save(org.mockito.ArgumentMatchers.argThat(u -> u.status() == UserStatus.INACTIVE));
    }

    @Test
    void bootstrap_does_nothing_when_a_librarian_already_exists() {
        when(users.countActiveLibrarians()).thenReturn(1L);

        assertThat(service.bootstrapLibrarian("Ana", "ana@example.com", "senha-segura")).isFalse();
    }

    @Test
    void bootstrap_creates_the_first_librarian() {
        when(users.countActiveLibrarians()).thenReturn(0L);

        assertThat(service.bootstrapLibrarian("Ana", "ana@example.com", "senha-segura")).isTrue();
        verify(users).save(org.mockito.ArgumentMatchers.argThat(LibraryUser::isLibrarian));
    }
}
