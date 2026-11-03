package br.com.fiap.library.application.usecase;

import br.com.fiap.library.application.model.Actor;
import br.com.fiap.library.application.model.CopyCount;
import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.application.model.ReservationView;
import br.com.fiap.library.application.port.in.ReservationManagement;
import br.com.fiap.library.application.port.out.BookCopyRepository;
import br.com.fiap.library.application.port.out.BookRepository;
import br.com.fiap.library.application.port.out.ReservationRepository;
import br.com.fiap.library.application.port.out.UnitOfWork;
import br.com.fiap.library.application.port.out.UserRepository;
import br.com.fiap.library.domain.exception.BusinessRuleException;
import br.com.fiap.library.domain.exception.ConflictException;
import br.com.fiap.library.domain.exception.NotFoundException;
import br.com.fiap.library.domain.model.Reservation;
import br.com.fiap.library.domain.model.ReservationStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

public class ReservationService implements ReservationManagement {
    private static final int EXPIRY_BATCH = 100;

    private final ReservationRepository reservations;
    private final BookRepository books;
    private final BookCopyRepository copies;
    private final UserRepository users;
    private final ReservationQueue queue;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public ReservationService(ReservationRepository reservations, BookRepository books, BookCopyRepository copies,
                              UserRepository users, ReservationQueue queue, UnitOfWork unitOfWork, Clock clock) {
        this.reservations = reservations;
        this.books = books;
        this.copies = copies;
        this.users = users;
        this.queue = queue;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public ReservationView create(Actor actor, Long userId, Long bookId) {
        Long ownerId = userId == null ? actor.userId() : userId;
        actor.requireSelfOrLibrarian(ownerId);

        return unitOfWork.inTransaction(() -> {
            if (!users.findById(ownerId).orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + ownerId))
                    .isActive()) {
                throw new BusinessRuleException("Usuário inativo não pode reservar livros");
            }
            books.findActiveById(bookId).orElseThrow(() -> new NotFoundException("Livro não encontrado: " + bookId));
            if (reservations.findOpenByUserAndBook(ownerId, bookId).isPresent()) {
                throw new ConflictException("O usuário já possui uma reserva em aberto para este livro");
            }
            CopyCount count = copies.countByBookIds(List.of(bookId)).getOrDefault(bookId, CopyCount.NONE);
            if (count.available() > 0) {
                throw new BusinessRuleException("Há exemplar disponível: a reserva só é necessária quando não há nenhum");
            }
            return withPosition(reservations.save(Reservation.open(ownerId, bookId, clock.instant())));
        });
    }

    @Override
    public ReservationView get(Actor actor, Long id) {
        Reservation reservation = reservations.findById(id)
                .orElseThrow(() -> new NotFoundException("Reserva não encontrada: " + id));
        actor.requireSelfOrLibrarian(reservation.userId());
        return withPosition(reservation);
    }

    @Override
    public void cancel(Actor actor, Long id) {
        unitOfWork.run(() -> {
            Reservation reservation = reservations.findByIdForUpdate(id)
                    .orElseThrow(() -> new NotFoundException("Reserva não encontrada: " + id));
            actor.requireSelfOrLibrarian(reservation.userId());
            queue.close(reservation.cancel(), clock.instant());
        });
    }

    @Override
    public PageResult<ReservationView> listByUser(Actor actor, Long userId, PageQuery page) {
        actor.requireSelfOrLibrarian(userId);
        users.findById(userId).orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + userId));
        return reservations.findByUser(userId, page).map(reservation -> new ReservationView(reservation, null));
    }

    @Override
    public int expireOverdue() {
        return unitOfWork.inTransaction(() -> {
            Instant now = clock.instant();
            List<Reservation> overdue = reservations.findExpiredForUpdate(now, EXPIRY_BATCH);
            overdue.forEach(reservation -> queue.close(reservation.expire(), now));
            return overdue.size();
        });
    }

    private ReservationView withPosition(Reservation reservation) {
        Long position = reservation.status() == ReservationStatus.WAITING
                ? reservations.queuePosition(reservation)
                : null;
        return new ReservationView(reservation, position);
    }
}
