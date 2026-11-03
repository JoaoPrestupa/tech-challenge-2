package br.com.fiap.library.application.model;

import br.com.fiap.library.domain.exception.ForbiddenException;
import br.com.fiap.library.domain.model.Role;
import java.util.Set;

public record Actor(Long userId, Set<Role> roles) {

    public boolean isLibrarian() {
        return roles.contains(Role.ROLE_LIBRARIAN);
    }

    public void requireLibrarian() {
        if (!isLibrarian()) {
            throw new ForbiddenException("Operação restrita a bibliotecários");
        }
    }

    public void requireSelfOrLibrarian(Long ownerId) {
        if (!isLibrarian() && !userId.equals(ownerId)) {
            throw new ForbiddenException("Você não tem acesso aos dados de outro usuário");
        }
    }
}
