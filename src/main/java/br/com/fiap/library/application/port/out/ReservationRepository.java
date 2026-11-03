package br.com.fiap.library.application.port.out;

import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.domain.model.Reservation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);

    Optional<Reservation> findById(Long id);

    Optional<Reservation> findByIdForUpdate(Long id);

    Optional<Reservation> findOpenByUserAndBook(Long userId, Long bookId);

    /** Primeiro da fila FIFO do livro, com a linha bloqueada. */
    Optional<Reservation> findNextWaitingForUpdate(Long bookId);

    long queuePosition(Reservation reservation);

    boolean existsOpenByBook(Long bookId);

    boolean existsByBook(Long bookId);

    PageResult<Reservation> findByUser(Long userId, PageQuery page);

    List<Reservation> findOpenByUser(Long userId);

    /** Usa SKIP LOCKED: instâncias em paralelo não processam a mesma reserva. */
    List<Reservation> findExpiredForUpdate(Instant now, int limit);
}
