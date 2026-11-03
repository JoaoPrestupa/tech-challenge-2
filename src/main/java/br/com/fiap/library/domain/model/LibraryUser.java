package br.com.fiap.library.domain.model;

import br.com.fiap.library.domain.exception.InvalidDataException;
import java.util.Set;

public record LibraryUser(Long id, String name, String email, String passwordHash, UserStatus status, Set<Role> roles) {

    public LibraryUser {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public static LibraryUser create(String name, String email, String passwordHash, Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new InvalidDataException("O usuário precisa de ao menos uma role");
        }
        return new LibraryUser(null, Text.required(name, "Nome", 150), EmailAddress.normalize(email),
                passwordHash, UserStatus.ACTIVE, roles);
    }

    public LibraryUser updateProfile(String newName, String newEmail) {
        return new LibraryUser(id, Text.required(newName, "Nome", 150), EmailAddress.normalize(newEmail),
                passwordHash, status, roles);
    }

    public LibraryUser withRoles(Set<Role> newRoles) {
        if (newRoles == null || newRoles.isEmpty()) {
            throw new InvalidDataException("O usuário precisa de ao menos uma role");
        }
        return new LibraryUser(id, name, email, passwordHash, status, newRoles);
    }

    public LibraryUser withPasswordHash(String newHash) {
        return new LibraryUser(id, name, email, newHash, status, roles);
    }

    public LibraryUser deactivate() {
        return new LibraryUser(id, name, email, passwordHash, UserStatus.INACTIVE, roles);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isLibrarian() {
        return roles.contains(Role.ROLE_LIBRARIAN);
    }
}
