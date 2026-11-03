package br.com.fiap.library.application.port.in;

import br.com.fiap.library.application.model.Actor;
import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.domain.model.LibraryUser;
import br.com.fiap.library.domain.model.Role;
import java.util.Set;

public interface UserManagement {
    LibraryUser create(String name, String email, String password, Set<Role> roles);

    LibraryUser get(Actor actor, Long id);

    PageResult<LibraryUser> list(PageQuery page);

    LibraryUser update(Actor actor, Long id, String name, String email, Set<Role> roles);

    void deactivate(Actor actor, Long id);

    boolean bootstrapLibrarian(String name, String email, String password);
}
