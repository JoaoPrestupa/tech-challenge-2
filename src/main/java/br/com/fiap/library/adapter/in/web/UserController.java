package br.com.fiap.library.adapter.in.web;

import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.application.port.in.LoanManagement;
import br.com.fiap.library.application.port.in.ReservationManagement;
import br.com.fiap.library.application.port.in.UserManagement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@Tag(name = "Usuários")
class UserController {
    private final UserManagement users;
    private final LoanManagement loans;
    private final ReservationManagement reservations;
    private final Responses responses;

    UserController(UserManagement users, LoanManagement loans, ReservationManagement reservations,
                   Responses responses) {
        this.users = users;
        this.loans = loans;
        this.reservations = reservations;
        this.responses = responses;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria usuário, inclusive bibliotecário")
    Responses.UserResponse create(@Valid @RequestBody Requests.CreateUserRequest request) {
        return responses.user(users.create(request.name(), request.email(), request.password(), request.roles()));
    }

    @GetMapping
    PageResult<Responses.UserResponse> list(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            @RequestParam(defaultValue = "name") String sort,
                                            @RequestParam(defaultValue = "asc") String direction) {
        return users.list(new PageQuery(page, size, sort, !"desc".equalsIgnoreCase(direction))).map(responses::user);
    }

    @GetMapping("/{id}")
    Responses.UserResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return responses.user(users.get(Actors.from(jwt), id));
    }

    @PutMapping("/{id}")
    Responses.UserResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                  @Valid @RequestBody Requests.UpdateUserRequest request) {
        return responses.user(users.update(Actors.from(jwt), id, request.name(), request.email(), request.roles()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativa o usuário", description = "O registro é mantido para preservar o histórico.")
    void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        users.deactivate(Actors.from(jwt), id);
    }

    @GetMapping("/{id}/loans")
    @Operation(summary = "Histórico de empréstimos do usuário")
    PageResult<Responses.LoanResponse> loans(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(defaultValue = "borrowedAt") String sort,
                                             @RequestParam(defaultValue = "desc") String direction) {
        return loans.historyOf(Actors.from(jwt), id, new PageQuery(page, size, sort, !"desc".equalsIgnoreCase(direction)))
                .map(responses::loan);
    }

    @GetMapping("/{id}/reservations")
    PageResult<Responses.ReservationResponse> reservations(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size,
                                                           @RequestParam(defaultValue = "createdAt") String sort,
                                                           @RequestParam(defaultValue = "desc") String direction) {
        return reservations.listByUser(Actors.from(jwt), id,
                        new PageQuery(page, size, sort, !"desc".equalsIgnoreCase(direction)))
                .map(responses::reservation);
    }
}
