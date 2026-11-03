package br.com.fiap.library.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.library.application.model.Actor;
import br.com.fiap.library.application.model.CopyCount;
import br.com.fiap.library.application.port.out.BookCopyRepository;
import br.com.fiap.library.application.port.out.BookRepository;
import br.com.fiap.library.application.port.out.ReservationRepository;
import br.com.fiap.library.application.port.out.UserRepository;
import br.com.fiap.library.application.usecase.ReservationQueue;
import br.com.fiap.library.application.usecase.ReservationService;
import br.com.fiap.library.domain.exception.BusinessRuleException;
import br.com.fiap.library.domain.exception.ConflictException;
import br.com.fiap.library.domain.exception.ForbiddenException;
import br.com.fiap.library.domain.model.Book;
import br.com.fiap.library.domain.model.BookCopy;
import br.com.fiap.library.domain.model.CopyStatus;
import br.com.fiap.library.domain.model.LibraryUser;
import br.com.fiap.library.domain.model.Reservation;
import br.com.fiap.library.domain.model.ReservationStatus;
import br.com.fiap.library.domain.model.Role;
import br.com.fiap.library.domain.model.UserStatus;
import br.com.fiap.library.domain.service.LibraryPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReservationServiceTest {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Instant NOW = Instant.parse("2026-11-02T13:00:00Z");
    private static final Long MEMBER = 1L;
    private static final Long OTHER = 2L;
    private static final Long BOOK = 20L;
    private static final Long COPY = 10L;

    private final ReservationRepository reservations = mock();
    private final BookRepository books = mock();
    private final BookCopyRepository copies = mock();
    private final UserRepository users = mock();
    private final Actor member = new Actor(MEMBER, Set.of(Role.ROLE_MEMBER));
    private final Actor librarian = new Actor(99L, Set.of(Role.ROLE_LIBRARIAN));
    private ReservationService service;

    @BeforeEach
    void setUp() {
        LibraryPolicy policy = new LibraryPolicy(14, 3, Duration.ofHours(48), ZONE);
        service = new ReservationService(reservations, books, copies, users,
                new ReservationQueue(reservations, copies, policy), new DirectUnitOfWork(), Clock.fixed(NOW, ZONE));
        lenient().when(users.findById(any())).thenReturn(Optional.of(new LibraryUser(MEMBER, "Maria", "m@example.com",
                "hash", UserStatus.ACTIVE, Set.of(Role.ROLE_MEMBER))));
        lenient().when(books.findActiveById(BOOK)).thenReturn(Optional.of(
                new Book(BOOK, "Dom Casmurro", "Machado de Assis", "9788535902778", null, true)));
        lenient().when(reservations.save(any())).thenAnswer(call -> {
            Reservation r = call.getArgument(0);
            return new Reservation(r.id() == null ? 5L : r.id(), r.userId(), r.bookId(), r.bookCopyId(), r.status(),
                    r.createdAt(), r.readyUntil());
        });
        lenient().when(reservations.findOpenByUserAndBook(any(), any())).thenReturn(Optional.empty());
        lenient().when(reservations.findNextWaitingForUpdate(any())).thenReturn(Optional.empty());
        lenient().when(copies.save(any())).thenAnswer(call -> call.getArgument(0));
        lenient().when(copies.countByBookIds(any())).thenReturn(Map.of());
    }

    @Test
    void member_creates_own_reservation_and_sees_the_queue_position() {
        when(reservations.queuePosition(any())).thenReturn(3L);

        var view = service.create(member, null, BOOK);

        assertThat(view.reservation().userId()).isEqualTo(MEMBER);
        assertThat(view.reservation().status()).isEqualTo(ReservationStatus.WAITING);
        assertThat(view.queuePosition()).isEqualTo(3L);
    }

    @Test
    void member_cannot_reserve_for_another_member() {
        assertThatThrownBy(() -> service.create(member, OTHER, BOOK)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void librarian_can_reserve_for_any_member() {
        assertThat(service.create(librarian, OTHER, BOOK).reservation().userId()).isEqualTo(OTHER);
    }

    @Test
    void reservation_is_rejected_while_a_copy_is_available() {
        when(copies.countByBookIds(List.of(BOOK))).thenReturn(Map.of(BOOK, new CopyCount(2, 1)));

        assertThatThrownBy(() -> service.create(member, null, BOOK)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void second_open_reservation_for_the_same_book_is_rejected() {
        when(reservations.findOpenByUserAndBook(MEMBER, BOOK))
                .thenReturn(Optional.of(Reservation.open(MEMBER, BOOK, NOW)));

        assertThatThrownBy(() -> service.create(member, null, BOOK)).isInstanceOf(ConflictException.class);
    }

    @Test
    void member_cannot_cancel_a_reservation_from_another_member() {
        when(reservations.findByIdForUpdate(5L)).thenReturn(Optional.of(
                new Reservation(5L, OTHER, BOOK, null, ReservationStatus.WAITING, NOW, null)));

        assertThatThrownBy(() -> service.cancel(member, 5L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancelling_a_ready_reservation_passes_the_copy_to_the_next_in_line() {
        Reservation ready = new Reservation(5L, MEMBER, BOOK, COPY, ReservationStatus.READY, NOW, NOW.plusSeconds(60));
        Reservation next = new Reservation(6L, OTHER, BOOK, null, ReservationStatus.WAITING, NOW, null);
        when(reservations.findByIdForUpdate(5L)).thenReturn(Optional.of(ready));
        when(reservations.findNextWaitingForUpdate(BOOK)).thenReturn(Optional.of(next));
        when(copies.findByIdForUpdate(COPY)).thenReturn(Optional.of(new BookCopy(COPY, BOOK, "EX-1", CopyStatus.RESERVED)));

        service.cancel(member, 5L);

        ArgumentCaptor<Reservation> saved = ArgumentCaptor.forClass(Reservation.class);
        verify(reservations, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0).status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(saved.getAllValues().get(1).id()).isEqualTo(6L);
        assertThat(saved.getAllValues().get(1).status()).isEqualTo(ReservationStatus.READY);
        assertThat(saved.getAllValues().get(1).bookCopyId()).isEqualTo(COPY);
    }

    @Test
    void expiring_a_reservation_without_queue_frees_the_copy() {
        Reservation ready = new Reservation(5L, MEMBER, BOOK, COPY, ReservationStatus.READY, NOW, NOW.minusSeconds(1));
        when(reservations.findExpiredForUpdate(any(), anyInt())).thenReturn(List.of(ready));
        when(copies.findByIdForUpdate(COPY)).thenReturn(Optional.of(new BookCopy(COPY, BOOK, "EX-1", CopyStatus.RESERVED)));

        assertThat(service.expireOverdue()).isEqualTo(1);

        ArgumentCaptor<BookCopy> copy = ArgumentCaptor.forClass(BookCopy.class);
        verify(copies).save(copy.capture());
        assertThat(copy.getValue().status()).isEqualTo(CopyStatus.AVAILABLE);
        ArgumentCaptor<Reservation> saved = ArgumentCaptor.forClass(Reservation.class);
        verify(reservations).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(ReservationStatus.EXPIRED);
    }
}
