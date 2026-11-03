package br.com.fiap.library.adapter.in.web;

import br.com.fiap.library.application.model.MostBorrowedBook;
import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.application.port.in.ReportQueries;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@Tag(name = "Relatórios")
class ReportController {
    private final ReportQueries reports;
    private final Responses responses;

    ReportController(ReportQueries reports, Responses responses) {
        this.reports = reports;
        this.responses = responses;
    }

    @GetMapping("/most-borrowed")
    @Operation(summary = "Os 20 livros mais emprestados")
    List<MostBorrowedBook> mostBorrowed() {
        return reports.mostBorrowed();
    }

    @GetMapping("/active-loans")
    @Operation(summary = "Empréstimos ativos com previsão de devolução")
    PageResult<Responses.LoanResponse> activeLoans(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return reports.activeLoans(new PageQuery(page, size, "dueAt", true)).map(responses::loan);
    }
}
