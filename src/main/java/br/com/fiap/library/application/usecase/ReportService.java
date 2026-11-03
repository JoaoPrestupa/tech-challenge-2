package br.com.fiap.library.application.usecase;

import br.com.fiap.library.application.model.LoanDetails;
import br.com.fiap.library.application.model.LoanFilter;
import br.com.fiap.library.application.model.MostBorrowedBook;
import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.application.port.in.ReportQueries;
import br.com.fiap.library.application.port.out.LoanRepository;
import java.util.List;

public class ReportService implements ReportQueries {
    private static final int TOP_LIMIT = 20;

    private final LoanRepository loans;

    public ReportService(LoanRepository loans) {
        this.loans = loans;
    }

    @Override
    public List<MostBorrowedBook> mostBorrowed() {
        return loans.mostBorrowed(TOP_LIMIT);
    }

    @Override
    public PageResult<LoanDetails> activeLoans(PageQuery page) {
        return loans.search(new LoanFilter(null, true), page);
    }
}
