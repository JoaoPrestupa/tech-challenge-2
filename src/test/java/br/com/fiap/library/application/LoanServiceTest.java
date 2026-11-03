package br.com.fiap.library.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.library.application.model.Actor;
import br.com.fiap.library.application.model.LoanDetails;
import br.com.fiap.library.application.port.out.BookCopyRepository;
import br.com.fiap.library.application.port.out.LoanRepository;
import br.com.fiap.library.application.port.out.ReservationRepository;
import br.com.fiap.library.application.port.out.UserRepository;
import br.com.fiap.library.application.usecase.LoanService;
import br.com.fiap.library.application.usecase.ReservationQueue;
import br.com.fiap.library.domain.exception.BusinessRuleException;
import br.com.fiap.library.domain.exception.ConflictException;
import br.com.fiap.library.domain.exception.ForbiddenException;
import br.com.fiap.library.domain.exception.NotFoundException;
import br.com.fiap.library.domain.model.BookCopy;
import br.com.fiap.library.domain.model.CopyStatus;
import br.com.fiap.library.domain.model.LibraryUser;
import br.com.fiap.library.domain.model.Loan;
import br.com.fiap.library.domain.model.Reservation;
import br.com.fiap.library.domain.model.ReservationStatus;
import br.com.fiap.library.domain.model.Role;
import br.com.fiap.library.domain.model.UserStatus;
import br.com.fiap.library.domain.service.LibraryPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LoanServiceTest {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Instant NOW = Instant.parse("2026-11-02T13:00:00Z");
    private static final Long USER = 1L;
    private static final Long OTHER_USER = 2L;
    private static final Long COPY = 10L;
    private static final Long BOOK = 20L;

    private final LoanRepository loans = mock();
    private final BookCopyRepository copies = mock();
    private final UserRepository users = mock();
    private final ReservationRepository reservations = mock();
    private final LibraryPolicy policy = new LibraryPolicy(14, 3, Duration.ofHours(48), ZONE);
    private LoanService service;

    @BeforeEach
    void setUp() {
        service = new LoanService(loans, copies, users, reservations, new ReservationQueue(reservations, copies, policy),
                policy, new DirectUnitOfWork(), Clock.fixed(NOW, ZONE));
        lenient().when(loans.save(any())).thenAnswer(call -> {
            Loan loan = call.getArgument(0);
            return new Loan(99L, loan.userId(), loan.bookCopyId(), loan.borrowedAt(), loan.dueAt(), loan.returnedAt());
        });
        lenient().when(loans.findDetailsById(99L)).thenReturn(Optional.of(details(USER)));
        lenient().when(copies.save(any())).thenAnswer(call -> call.getArgument(0));
        lenient().when(users.findByIdForUpdate(USER)).thenReturn(Optional.of(user(UserStatus.ACTIVE)));
        lenient().when(reservations.findOpenByUserAndBook(any(), any())).thenReturn(Optional.empty());
        lenient().when(reservations.findNextWaitingForUpdate(any())).thenReturn(Optional.empty());
    }

    private static LibraryUser user(UserStatus status) {
        return new LibraryUser(USER, "Maria", "maria@example.com", "hash", status, Set.of(Role.ROLE_MEMBER));
    }

    private static LoanDetails details(Long owner) {
        return new LoanDetails(99L, owner, "Maria", COPY, "EX-1", BOOK, "Dom Casmurro", NOW, NOW.plusSeconds(60), null);
    }

    private void copyIs(CopyStatus status) {
        when(copies.findByIdForUpdate(COPY)).thenReturn(Optional.of(new BookCopy(COPY, BOOK, "EX-1", status)));
    }

    @Test
    void borrow_marks_copy_as_loaned_and_sets_the_due_date() {
        copyIs(CopyStatus.AVAILABLE);

        service.borrow(USER, COPY);

        ArgumentCaptor<Loan> loan = ArgumentCaptor.forClass(Loan.class);
        verify(loans).save(loan.capture());
        assertThat(loan.getValue().dueAt()).isEqualTo(Instant.parse("2026-11-17T02:59:59Z"));
        ArgumentCaptor<BookCopy> copy = ArgumentCaptor.forClass(BookCopy.class);
        verify(copies).save(copy.capture());
        assertThat(copy.getValue().status()).isEqualTo(CopyStatus.LOANED);
    }

    @Test
    void borrow_is_rejected_at_the_active_loan_limit() {
        when(loans.countActiveByUser(USER)).thenReturn(3L);

        assertThatThrownBy(() -> service.borrow(USER, COPY)).isInstanceOf(BusinessRuleException.class);
        verify(loans, never()).save(any());
    }

    @Test
    void borrow_is_allowed_just_below_the_limit() {
        when(loans.countActiveByUser(USER)).thenReturn(2L);
        copyIs(CopyStatus.AVAILABLE);

        assertThat(service.borrow(USER, COPY).id()).isEqualTo(99L);
    }

    @Test
    void inactive_user_cannot_borrow() {
        when(users.findByIdForUpdate(USER)).thenReturn(Optional.of(user(UserStatus.INACTIVE)));

        assertThatThrownBy(() -> service.borrow(USER, COPY)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void loaned_copy_cannot_be_borrowed_again() {
        copyIs(CopyStatus.LOANED);

        assertThatThrownBy(() -> service.borrow(USER, COPY)).isInstanceOf(ConflictException.class);
        verify(loans, never()).save(any());
    }

    @Test
    void missing_copy_returns_not_found() {
        when(copies.findByIdForUpdate(COPY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.borrow(USER, COPY)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void copy_held_for_another_user_cannot_be_borrowed() {
        copyIs(CopyStatus.RESERVED);

        assertThatThrownBy(() -> service.borrow(USER, COPY)).isInstanceOf(ConflictException.class);
        verify(loans, never()).save(any());
    }

    @Test
    void borrowing_the_held_copy_fulfills_the_reservation() {
        copyIs(CopyStatus.RESERVED);
        Reservation ready = Reservation.open(USER, BOOK, NOW).markReady(COPY, NOW.plusSeconds(60));
        when(reservations.findOpenByUserAndBook(USER, BOOK)).thenReturn(Optional.of(ready));

        service.borrow(USER, COPY);

        ArgumentCaptor<Reservation> saved = ArgumentCaptor.forClass(Reservation.class);
        verify(reservations).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(ReservationStatus.FULFILLED);
    }

    @Test
    void user_with_a_held_copy_must_take_that_copy() {
        copyIs(CopyStatus.AVAILABLE);
        Reservation ready = Reservation.open(USER, BOOK, NOW).markReady(777L, NOW.plusSeconds(60));
        when(reservations.findOpenByUserAndBook(USER, BOOK)).thenReturn(Optional.of(ready));

        assertThatThrownBy(() -> service.borrow(USER, COPY)).isInstanceOf(ConflictException.class);
    }

    @Test
    void return_without_queue_makes_the_copy_available() {
        when(loans.findByIdForUpdate(99L)).thenReturn(Optional.of(new Loan(99L, USER, COPY, NOW, NOW.plusSeconds(60), null)));
        copyIs(CopyStatus.LOANED);

        service.giveBack(99L);

        ArgumentCaptor<Loan> loan = ArgumentCaptor.forClass(Loan.class);
        verify(loans).save(loan.capture());
        assertThat(loan.getValue().returnedAt()).isEqualTo(NOW);
        ArgumentCaptor<BookCopy> copy = ArgumentCaptor.forClass(BookCopy.class);
        verify(copies).save(copy.capture());
        assertThat(copy.getValue().status()).isEqualTo(CopyStatus.AVAILABLE);
    }

    @Test
    void return_with_queue_holds_the_copy_for_the_first_in_line() {
        when(loans.findByIdForUpdate(99L)).thenReturn(Optional.of(new Loan(99L, USER, COPY, NOW, NOW.plusSeconds(60), null)));
        copyIs(CopyStatus.LOANED);
        Reservation first = new Reservation(5L, OTHER_USER, BOOK, null, ReservationStatus.WAITING, NOW, null);
        when(reservations.findNextWaitingForUpdate(BOOK)).thenReturn(Optional.of(first));

        service.giveBack(99L);

        ArgumentCaptor<BookCopy> copy = ArgumentCaptor.forClass(BookCopy.class);
        verify(copies).save(copy.capture());
        assertThat(copy.getValue().status()).isEqualTo(CopyStatus.RESERVED);
        ArgumentCaptor<Reservation> reservation = ArgumentCaptor.forClass(Reservation.class);
        verify(reservations).save(reservation.capture());
        assertThat(reservation.getValue().status()).isEqualTo(ReservationStatus.READY);
        assertThat(reservation.getValue().readyUntil()).isEqualTo(NOW.plus(Duration.ofHours(48)));
    }

    @Test
    void returning_twice_is_a_conflict() {
        when(loans.findByIdForUpdate(99L)).thenReturn(Optional.of(new Loan(99L, USER, COPY, NOW, NOW.plusSeconds(60), NOW)));

        assertThatThrownBy(() -> service.giveBack(99L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void member_cannot_read_a_loan_from_another_member() {
        Actor other = new Actor(OTHER_USER, Set.of(Role.ROLE_MEMBER));

        assertThatThrownBy(() -> service.get(other, 99L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void librarian_can_read_any_loan() {
        Actor librarian = new Actor(OTHER_USER, Set.of(Role.ROLE_LIBRARIAN));

        assertThat(service.get(librarian, 99L).id()).isEqualTo(99L);
    }
}
