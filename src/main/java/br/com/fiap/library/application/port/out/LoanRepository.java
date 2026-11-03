package br.com.fiap.library.application.port.out;

import br.com.fiap.library.application.model.LoanDetails;
import br.com.fiap.library.application.model.LoanFilter;
import br.com.fiap.library.application.model.MostBorrowedBook;
import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.domain.model.Loan;
import java.util.List;
import java.util.Optional;

public interface LoanRepository {
    Loan save(Loan loan);

    Optional<Loan> findByIdForUpdate(Long id);

    Optional<LoanDetails> findDetailsById(Long id);

    PageResult<LoanDetails> search(LoanFilter filter, PageQuery page);

    long countActiveByUser(Long userId);

    boolean existsActiveByBook(Long bookId);

    boolean existsByBook(Long bookId);

    List<MostBorrowedBook> mostBorrowed(int limit);
}
