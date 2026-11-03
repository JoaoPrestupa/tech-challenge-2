package br.com.fiap.library.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface BookJpaRepository extends JpaRepository<BookEntity, Long>, JpaSpecificationExecutor<BookEntity> {
    Optional<BookEntity> findByIdAndActiveTrue(Long id);

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    @Query("select b.isbn from BookEntity b where b.isbn in :isbns")
    List<String> findExistingIsbns(@Param("isbns") Collection<String> isbns);
}
