package br.com.fiap.library.adapter.out.persistence;

import br.com.fiap.library.application.model.LoanDetails;
import br.com.fiap.library.application.model.MostBorrowedBook;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface LoanJpaRepository extends JpaRepository<LoanEntity, Long> {
    String DETAILS_SELECT = """
            select new br.com.fiap.library.application.model.LoanDetails(
                l.id, l.userId, u.name, l.bookCopyId, c.assetCode, b.id, b.title, l.borrowedAt, l.dueAt, l.returnedAt)
            from LoanEntity l
            join UserEntity u on u.id = l.userId
            join BookCopyEntity c on c.id = l.bookCopyId
            join BookEntity b on b.id = c.bookId
            """;

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LoanEntity l where l.id = :id")
    Optional<LoanEntity> findByIdForUpdate(@Param("id") Long id);

    @Query(DETAILS_SELECT + " where l.id = :id")
    Optional<LoanDetails> findDetailsById(@Param("id") Long id);

    @Query(value = DETAILS_SELECT + " where l.userId = :userId",
            countQuery = "select count(l) from LoanEntity l where l.userId = :userId")
    Page<LoanDetails> findDetailsByUser(@Param("userId") Long userId, Pageable pageable);

    @Query(value = DETAILS_SELECT + " where l.userId = :userId and l.returnedAt is null",
            countQuery = "select count(l) from LoanEntity l where l.userId = :userId and l.returnedAt is null")
    Page<LoanDetails> findActiveDetailsByUser(@Param("userId") Long userId, Pageable pageable);

    @Query(value = DETAILS_SELECT + " where l.userId = :userId and l.returnedAt is not null",
            countQuery = "select count(l) from LoanEntity l where l.userId = :userId and l.returnedAt is not null")
    Page<LoanDetails> findReturnedDetailsByUser(@Param("userId") Long userId, Pageable pageable);

    @Query(value = DETAILS_SELECT, countQuery = "select count(l) from LoanEntity l")
    Page<LoanDetails> findAllDetails(Pageable pageable);

    @Query(value = DETAILS_SELECT + " where l.returnedAt is null",
            countQuery = "select count(l) from LoanEntity l where l.returnedAt is null")
    Page<LoanDetails> findActiveDetails(Pageable pageable);

    @Query(value = DETAILS_SELECT + " where l.returnedAt is not null",
            countQuery = "select count(l) from LoanEntity l where l.returnedAt is not null")
    Page<LoanDetails> findReturnedDetails(Pageable pageable);

    long countByUserIdAndReturnedAtIsNull(Long userId);

    @Query("""
            select count(l) > 0 from LoanEntity l, BookCopyEntity c
            where c.id = l.bookCopyId and c.bookId = :bookId and l.returnedAt is null
            """)
    boolean existsActiveByBook(@Param("bookId") Long bookId);

    @Query("""
            select count(l) > 0 from LoanEntity l, BookCopyEntity c
            where c.id = l.bookCopyId and c.bookId = :bookId
            """)
    boolean existsByBook(@Param("bookId") Long bookId);

    /** A agregação fica no banco: nada é carregado em memória para contar. */
    @Query("""
            select new br.com.fiap.library.application.model.MostBorrowedBook(b.id, b.title, b.author, b.isbn, count(l))
            from LoanEntity l
            join BookCopyEntity c on c.id = l.bookCopyId
            join BookEntity b on b.id = c.bookId
            group by b.id, b.title, b.author, b.isbn
            order by count(l) desc, b.title asc
            """)
    List<MostBorrowedBook> mostBorrowed(Pageable pageable);
}
