package br.com.fiap.library.application.port.in;

import br.com.fiap.library.application.model.LoanDetails;
import br.com.fiap.library.application.model.MostBorrowedBook;
import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import java.util.List;

public interface ReportQueries {
    List<MostBorrowedBook> mostBorrowed();

    PageResult<LoanDetails> activeLoans(PageQuery page);
}
