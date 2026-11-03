package br.com.fiap.library.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RefreshSessionJpaRepository extends JpaRepository<RefreshSessionEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from RefreshSessionEntity s where s.tokenHash = :hash")
    Optional<RefreshSessionEntity> findByTokenHashForUpdate(@Param("hash") String hash);

    @Modifying(flushAutomatically = true)
    @Query("update RefreshSessionEntity s set s.revokedAt = :at where s.userId = :userId and s.revokedAt is null")
    void revokeAllByUser(@Param("userId") Long userId, @Param("at") Instant at);
}
