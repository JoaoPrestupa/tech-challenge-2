package br.com.fiap.library.application.port.in;

import br.com.fiap.library.application.model.AuthTokens;
import br.com.fiap.library.domain.model.LibraryUser;

public interface Authentication {
    LibraryUser register(String name, String email, String password);

    AuthTokens login(String email, String password);

    AuthTokens refresh(String refreshToken);

    void logout(String refreshToken);

    void changePassword(Long userId, String currentPassword, String newPassword);

    LibraryUser me(Long userId);
}
