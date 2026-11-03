package br.com.fiap.library.adapter.in.web;

import br.com.fiap.library.application.port.in.ReservationManagement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reservations")
@Tag(name = "Reservas")
class ReservationController {
    private final ReservationManagement reservations;
    private final Responses responses;

    ReservationController(ReservationManagement reservations, Responses responses) {
        this.reservations = reservations;
        this.responses = responses;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Responses.ReservationResponse create(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody Requests.ReservationRequest request) {
        return responses.reservation(reservations.create(Actors.from(jwt), request.userId(), request.bookId()));
    }

    @GetMapping("/{id}")
    Responses.ReservationResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return responses.reservation(reservations.get(Actors.from(jwt), id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        reservations.cancel(Actors.from(jwt), id);
    }
}
