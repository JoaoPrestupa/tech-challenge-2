package br.com.fiap.library.adapter.in.web;

import br.com.fiap.library.application.port.in.Authentication;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação")
class AuthController {
    private final Authentication authentication;
    private final Responses responses;

    AuthController(Authentication authentication, Responses responses) {
        this.authentication = authentication;
        this.responses = responses;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    Responses.UserResponse register(@Valid @RequestBody Requests.RegisterRequest request) {
        return responses.user(authentication.register(request.name(), request.email(), request.password()));
    }

    @PostMapping("/login")
    Responses.TokenResponse login(@Valid @RequestBody Requests.LoginRequest request) {
        return responses.tokens(authentication.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    Responses.TokenResponse refresh(@Valid @RequestBody Requests.RefreshRequest request) {
        return responses.tokens(authentication.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody Requests.RefreshRequest request) {
        authentication.logout(request.refreshToken());
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Requests.ChangePasswordRequest request) {
        authentication.changePassword(Actors.from(jwt).userId(), request.currentPassword(), request.newPassword());
    }

    @GetMapping("/me")
    Responses.UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return responses.user(authentication.me(Actors.from(jwt).userId()));
    }
}
