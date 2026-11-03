package br.com.fiap.library.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select count(distinct u.id) from UserEntity u join u.roles r
            where r = br.com.fiap.library.domain.model.Role.ROLE_LIBRARIAN
              and u.status = br.com.fiap.library.domain.model.UserStatus.ACTIVE
            """)
    long countActiveLibrarians();
}
