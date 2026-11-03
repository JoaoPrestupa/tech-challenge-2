package br.com.fiap.library.domain.model;

import br.com.fiap.library.domain.exception.ConflictException;
import java.time.Instant;


public record Reservation(Long id, Long userId, Long bookId, Long bookCopyId, ReservationStatus status,
                          Instant createdAt, Instant readyUntil) {

    public static Reservation open(Long userId, Long bookId, Instant at) {
        return new Reservation(null, userId, bookId, null, ReservationStatus.WAITING, at, null);
    }

    public Reservation markReady(Long copyId, Instant until) {
        if (status != ReservationStatus.WAITING) {
            throw new ConflictException("Somente uma reserva em espera pode ser atendida");
        }
        return new Reservation(id, userId, bookId, copyId, ReservationStatus.READY, createdAt, until);
    }

    public Reservation fulfill() {
        requireOpen();
        return with(ReservationStatus.FULFILLED);
    }

    public Reservation cancel() {
        requireOpen();
        return with(ReservationStatus.CANCELLED);
    }

    public Reservation expire() {
        if (status != ReservationStatus.READY) {
            throw new ConflictException("Somente uma reserva pronta pode expirar");
        }
        return with(ReservationStatus.EXPIRED);
    }

    public boolean isOpen() {
        return status == ReservationStatus.WAITING || status == ReservationStatus.READY;
    }

    public boolean isReady() {
        return status == ReservationStatus.READY;
    }

    private void requireOpen() {
        if (!isOpen()) {
            throw new ConflictException("Esta reserva já foi encerrada");
        }
    }

    private Reservation with(ReservationStatus newStatus) {
        return new Reservation(id, userId, bookId, bookCopyId, newStatus, createdAt, readyUntil);
    }
}
