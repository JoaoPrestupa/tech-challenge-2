package br.com.fiap.library.application.usecase;

import br.com.fiap.library.application.port.out.BookCopyRepository;
import br.com.fiap.library.application.port.out.ReservationRepository;
import br.com.fiap.library.domain.exception.NotFoundException;
import br.com.fiap.library.domain.model.BookCopy;
import br.com.fiap.library.domain.model.Reservation;
import br.com.fiap.library.domain.service.LibraryPolicy;
import java.time.Instant;
import java.util.Optional;

/** Destina um exemplar liberado ao primeiro da fila FIFO. Deve rodar dentro de uma transação. */
public class ReservationQueue {
    private final ReservationRepository reservations;
    private final BookCopyRepository copies;
    private final LibraryPolicy policy;

    public ReservationQueue(ReservationRepository reservations, BookCopyRepository copies, LibraryPolicy policy) {
        this.reservations = reservations;
        this.copies = copies;
        this.policy = policy;
    }

    public BookCopy offer(BookCopy copy, Instant now) {
        BookCopy released = copy.release();
        Optional<Reservation> next = reservations.findNextWaitingForUpdate(copy.bookId());
        if (next.isEmpty()) {
            return copies.save(released);
        }
        BookCopy held = copies.save(released.reserve());
        reservations.save(next.get().markReady(held.id(), policy.holdUntil(now)));
        return held;
    }

    /** Encerra uma reserva aberta. Se ela segurava um exemplar, ele passa para o próximo da fila. */
    public void close(Reservation closed, Instant now) {
        boolean wasHoldingCopy = closed.bookCopyId() != null;
        reservations.save(closed);
        if (wasHoldingCopy) {
            BookCopy copy = copies.findByIdForUpdate(closed.bookCopyId())
                    .orElseThrow(() -> new NotFoundException("Exemplar não encontrado: " + closed.bookCopyId()));
            offer(copy, now);
        }
    }
}
