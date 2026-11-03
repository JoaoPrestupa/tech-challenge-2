package br.com.fiap.library.application.port.out;

import br.com.fiap.library.application.model.BookFilter;
import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.domain.model.Book;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BookRepository {
    Book save(Book book);

    List<Book> saveAll(List<Book> books);

    Optional<Book> findActiveById(Long id);

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    Set<String> findExistingIsbns(Collection<String> isbns);

    PageResult<Book> search(BookFilter filter, PageQuery page);

    void delete(Long id);
}
