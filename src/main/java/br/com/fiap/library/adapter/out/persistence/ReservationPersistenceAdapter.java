package br.com.fiap.library.adapter.out.persistence;

import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.application.port.out.ReservationRepository;
import br.com.fiap.library.domain.model.Reservation;
import br.com.fiap.library.domain.model.ReservationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
class ReservationPersistenceAdapter implements ReservationRepository {
    private static final List<ReservationStatus> OPEN = List.of(ReservationStatus.WAITING, ReservationStatus.READY);
    private static final Map<String, String> SORTABLE = Map.of("createdAt", "createdAt", "status", "status");

    private final ReservationJpaRepository repository;

    ReservationPersistenceAdapter(ReservationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Reservation save(Reservation reservation) {
        return repository.save(new ReservationEntity(reservation)).toDomain();
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        return repository.findById(id).map(ReservationEntity::toDomain);
    }

    @Override
    public Optional<Reservation> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id).map(ReservationEntity::toDomain);
    }

    @Override
    public Optional<Reservation> findOpenByUserAndBook(Long userId, Long bookId) {
        return repository.findFirstByUserIdAndBookIdAndStatusIn(userId, bookId, OPEN).map(ReservationEntity::toDomain);
    }

    @Override
    public Optional<Reservation> findNextWaitingForUpdate(Long bookId) {
        return repository.findWaitingForUpdate(bookId, PageRequest.of(0, 1)).stream()
                .findFirst().map(ReservationEntity::toDomain);
    }

    @Override
    public long queuePosition(Reservation reservation) {
        return repository.queuePosition(reservation.bookId(), reservation.createdAt(), reservation.id());
    }

    @Override
    public boolean existsOpenByBook(Long bookId) {
        return repository.existsByBookIdAndStatusIn(bookId, OPEN);
    }

    @Override
    public boolean existsByBook(Long bookId) {
        return repository.existsByBookId(bookId);
    }

    @Override
    public PageResult<Reservation> findByUser(Long userId, PageQuery page) {
        return Pages.result(repository.findByUserId(userId, Pages.of(page, SORTABLE, "createdAt"))
                .map(ReservationEntity::toDomain));
    }

    @Override
    public List<Reservation> findOpenByUser(Long userId) {
        return repository.findByUserIdAndStatusIn(userId, OPEN).stream().map(ReservationEntity::toDomain).toList();
    }

    @Override
    public List<Reservation> findExpiredForUpdate(Instant now, int limit) {
        return repository.findExpiredForUpdate(now, limit).stream().map(ReservationEntity::toDomain).toList();
    }
}
