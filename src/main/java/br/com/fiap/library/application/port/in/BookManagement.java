package br.com.fiap.library.application.port.in;

import br.com.fiap.library.application.model.BookFilter;
import br.com.fiap.library.application.model.BookView;
import br.com.fiap.library.application.model.NewBook;
import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.domain.model.BookCopy;
import java.util.List;

public interface BookManagement {
    BookView create(NewBook book);

    List<BookView> createBatch(List<NewBook> books);

    BookView get(Long id);

    PageResult<BookView> search(BookFilter filter, PageQuery page);

    BookView update(Long id, String title, String author, String isbn, String description);

    void delete(Long id);

    List<BookCopy> addCopies(Long bookId, List<String> assetCodes);

    List<BookCopy> listCopies(Long bookId);

    BookCopy inactivateCopy(Long bookId, Long copyId);
}
