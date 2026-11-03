package br.com.fiap.library.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface BookCopyJpaRepository extends JpaRepository<BookCopyEntity, Long> {
    record CountRow(Long bookId, Long total, Long available) {
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from BookCopyEntity c where c.id = :id")
    Optional<BookCopyEntity> findByIdForUpdate(@Param("id") Long id);

    List<BookCopyEntity> findByBookIdOrderByAssetCode(Long bookId);

    @Query("""
            select new br.com.fiap.library.adapter.out.persistence.BookCopyJpaRepository$CountRow(
                c.bookId,
                count(c),
                coalesce(sum(case when c.status = br.com.fiap.library.domain.model.CopyStatus.AVAILABLE then 1L else 0L end), 0L))
            from BookCopyEntity c
            where c.bookId in :bookIds
              and c.status <> br.com.fiap.library.domain.model.CopyStatus.INACTIVE
            group by c.bookId
            """)
    List<CountRow> countByBookIds(@Param("bookIds") Collection<Long> bookIds);

    @Query("select c.assetCode from BookCopyEntity c where c.assetCode in :codes")
    List<String> findExistingAssetCodes(@Param("codes") Collection<String> codes);

    @Modifying(flushAutomatically = true)
    @Query("""
            update BookCopyEntity c
            set c.status = br.com.fiap.library.domain.model.CopyStatus.INACTIVE
            where c.bookId = :bookId
            """)
    void inactivateAllByBookId(@Param("bookId") Long bookId);
}
