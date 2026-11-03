package br.com.fiap.library.adapter.out.persistence;

import br.com.fiap.library.domain.model.ReservationStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ReservationJpaRepository extends JpaRepository<ReservationEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReservationEntity r where r.id = :id")
    Optional<ReservationEntity> findByIdForUpdate(@Param("id") Long id);

    Optional<ReservationEntity> findFirstByUserIdAndBookIdAndStatusIn(Long userId, Long bookId,
                                                                      Collection<ReservationStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from ReservationEntity r
            where r.bookId = :bookId and r.status = br.com.fiap.library.domain.model.ReservationStatus.WAITING
            order by r.createdAt asc, r.id asc
            """)
    List<ReservationEntity> findWaitingForUpdate(@Param("bookId") Long bookId, Pageable pageable);

    @Query("""
            select count(r) + 1 from ReservationEntity r
            where r.bookId = :bookId
              and r.status = br.com.fiap.library.domain.model.ReservationStatus.WAITING
              and (r.createdAt < :createdAt or (r.createdAt = :createdAt and r.id < :id))
            """)
    long queuePosition(@Param("bookId") Long bookId, @Param("createdAt") Instant createdAt, @Param("id") Long id);

    boolean existsByBookIdAndStatusIn(Long bookId, Collection<ReservationStatus> statuses);

    boolean existsByBookId(Long bookId);

    Page<ReservationEntity> findByUserId(Long userId, Pageable pageable);

    List<ReservationEntity> findByUserIdAndStatusIn(Long userId, Collection<ReservationStatus> statuses);

    @Query(value = """
            select * from reservations
            where status = 'READY' and ready_until < :now
            order by ready_until
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<ReservationEntity> findExpiredForUpdate(@Param("now") Instant now, @Param("limit") int limit);
}
