package br.com.fiap.library.adapter.in.web;

import br.com.fiap.library.application.model.NewBook;
import br.com.fiap.library.domain.model.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

final class Requests {
    private Requests() {
    }

    record RegisterRequest(@NotBlank @Size(max = 150) String name,
                           @NotBlank @Email @Size(max = 254) String email,
                           @NotBlank @Size(min = 8, max = 72) String password) {
    }

    record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    record RefreshRequest(@NotBlank String refreshToken) {
    }

    record ChangePasswordRequest(@NotBlank String currentPassword,
                                 @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    record CreateUserRequest(@NotBlank @Size(max = 150) String name,
                             @NotBlank @Email @Size(max = 254) String email,
                             @NotBlank @Size(min = 8, max = 72) String password,
                             Set<Role> roles) {
    }

    /** roles é opcional e só pode ser enviado por bibliotecários. */
    record UpdateUserRequest(@NotBlank @Size(max = 150) String name,
                             @NotBlank @Email @Size(max = 254) String email,
                             Set<Role> roles) {
    }

    record BookRequest(@NotBlank @Size(max = 255) String title,
                       @NotBlank @Size(max = 255) String author,
                       @NotBlank @Size(max = 20) String isbn,
                       @Size(max = 5000) String description,
                       List<@NotBlank @Size(max = 60) String> assetCodes) {
        NewBook toNewBook() {
            return new NewBook(title, author, isbn, description, assetCodes);
        }
    }

    record BookBatchRequest(@NotEmpty @Size(max = 500) List<@Valid BookRequest> books) {
    }

    record CopiesRequest(@NotEmpty @Size(max = 500) List<@NotBlank @Size(max = 60) String> assetCodes) {
    }

    record LoanRequest(@NotNull Long userId, @NotNull Long bookCopyId) {
    }

    /** userId é opcional: sem ele, a reserva é do próprio usuário autenticado. */
    record ReservationRequest(Long userId, @NotNull Long bookId) {
    }
}
