package br.com.fiap.library.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.library.domain.exception.ConflictException;
import br.com.fiap.library.domain.model.BookCopy;
import br.com.fiap.library.domain.model.CopyStatus;
import br.com.fiap.library.domain.model.Loan;
import br.com.fiap.library.domain.model.Reservation;
import br.com.fiap.library.domain.model.ReservationStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DomainStateTest {
    private static final Instant NOW = Instant.parse("2026-11-02T13:00:00Z");

    private static BookCopy copy(CopyStatus status) {
        return new BookCopy(1L, 2L, "EX-1", status);
    }

    @Test
    void available_or_reserved_copy_can_be_loaned() {
        assertThat(copy(CopyStatus.AVAILABLE).loan().status()).isEqualTo(CopyStatus.LOANED);
        assertThat(copy(CopyStatus.RESERVED).loan().status()).isEqualTo(CopyStatus.LOANED);
    }

    @Test
    void loaned_or_inactive_copy_cannot_be_loaned() {
        assertThatThrownBy(() -> copy(CopyStatus.LOANED).loan()).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> copy(CopyStatus.INACTIVE).loan()).isInstanceOf(ConflictException.class);
    }

    @Test
    void only_available_copy_can_be_inactivated() {
        assertThat(copy(CopyStatus.AVAILABLE).inactivate().status()).isEqualTo(CopyStatus.INACTIVE);
        assertThatThrownBy(() -> copy(CopyStatus.LOANED).inactivate()).isInstanceOf(ConflictException.class);
    }

    @Test
    void loan_cannot_be_returned_twice() {
        Loan returned = Loan.open(1L, 2L, NOW, NOW.plusSeconds(60)).close(NOW.plusSeconds(30));

        assertThat(returned.isActive()).isFalse();
        assertThatThrownBy(() -> returned.close(NOW)).isInstanceOf(ConflictException.class);
    }

    @Test
    void reservation_follows_its_life_cycle() {
        Reservation waiting = Reservation.open(1L, 2L, NOW);
        Reservation ready = waiting.markReady(9L, NOW.plusSeconds(60));

        assertThat(ready.status()).isEqualTo(ReservationStatus.READY);
        assertThat(ready.bookCopyId()).isEqualTo(9L);
        assertThat(ready.fulfill().status()).isEqualTo(ReservationStatus.FULFILLED);
        assertThat(ready.expire().status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(waiting.cancel().status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void closed_reservation_cannot_change_again() {
        Reservation cancelled = Reservation.open(1L, 2L, NOW).cancel();

        assertThatThrownBy(cancelled::cancel).isInstanceOf(ConflictException.class);
        assertThatThrownBy(cancelled::fulfill).isInstanceOf(ConflictException.class);
        assertThatThrownBy(cancelled::expire).isInstanceOf(ConflictException.class);
    }

    @Test
    void waiting_reservation_cannot_expire() {
        assertThatThrownBy(() -> Reservation.open(1L, 2L, NOW).expire()).isInstanceOf(ConflictException.class);
    }
}
