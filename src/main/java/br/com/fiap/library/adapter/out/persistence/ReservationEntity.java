package br.com.fiap.library.adapter.out.persistence;

import br.com.fiap.library.domain.model.Reservation;
import br.com.fiap.library.domain.model.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "reservations")
class ReservationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "user_id", nullable = false)
    Long userId;
    @Column(name = "book_id", nullable = false)
    Long bookId;
    @Column(name = "book_copy_id")
    Long bookCopyId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    ReservationStatus status;
    @Column(name = "created_at", nullable = false)
    Instant createdAt;
    @Column(name = "ready_until")
    Instant readyUntil;

    protected ReservationEntity() {
    }

    ReservationEntity(Reservation reservation) {
        id = reservation.id();
        userId = reservation.userId();
        bookId = reservation.bookId();
        bookCopyId = reservation.bookCopyId();
        status = reservation.status();
        createdAt = reservation.createdAt();
        readyUntil = reservation.readyUntil();
    }

    Reservation toDomain() {
        return new Reservation(id, userId, bookId, bookCopyId, status, createdAt, readyUntil);
    }
}
