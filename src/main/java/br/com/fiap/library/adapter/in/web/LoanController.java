package br.com.fiap.library.adapter.in.web;

import br.com.fiap.library.application.model.LoanFilter;
import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.application.port.in.LoanManagement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/loans")
@Tag(name = "Empréstimos e devoluções")
class LoanController {
    private final LoanManagement loans;
    private final Responses responses;

    LoanController(LoanManagement loans, Responses responses) {
        this.loans = loans;
        this.responses = responses;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Responses.LoanResponse borrow(@Valid @RequestBody Requests.LoanRequest request) {
        return responses.loan(loans.borrow(request.userId(), request.bookCopyId()));
    }

    @GetMapping
    PageResult<Responses.LoanResponse> search(@RequestParam(required = false) Long userId,
                                              @RequestParam(required = false) Boolean active,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestParam(defaultValue = "dueAt") String sort,
                                              @RequestParam(defaultValue = "asc") String direction) {
        return loans.search(new LoanFilter(userId, active),
                        new PageQuery(page, size, sort, !"desc".equalsIgnoreCase(direction)))
                .map(responses::loan);
    }

    @GetMapping("/{id}")
    Responses.LoanResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return responses.loan(loans.get(Actors.from(jwt), id));
    }

    @PostMapping("/{id}/return")
    Responses.LoanResponse giveBack(@PathVariable Long id) {
        return responses.loan(loans.giveBack(id));
    }
}
