package br.com.fiap.library.application.port.out;

import br.com.fiap.library.application.model.AccessToken;
import br.com.fiap.library.domain.model.LibraryUser;

public interface AccessTokenIssuer {
    AccessToken issue(LibraryUser user);
}
