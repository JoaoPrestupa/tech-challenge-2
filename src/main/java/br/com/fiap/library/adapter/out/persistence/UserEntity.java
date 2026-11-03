package br.com.fiap.library.adapter.out.persistence;

import br.com.fiap.library.domain.model.LibraryUser;
import br.com.fiap.library.domain.model.Role;
import br.com.fiap.library.domain.model.UserStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "library_users")
class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, length = 150)
    String name;
    @Column(name = "normalized_email", nullable = false, length = 254)
    String email;
    @Column(name = "password_hash", nullable = false, length = 100)
    String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    UserStatus status;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", length = 30)
    Set<Role> roles = new HashSet<>();

    protected UserEntity() {
    }

    UserEntity(LibraryUser user) {
        id = user.id();
        name = user.name();
        email = user.email();
        passwordHash = user.passwordHash();
        status = user.status();
        roles = new HashSet<>(user.roles());
    }

    LibraryUser toDomain() {
        return new LibraryUser(id, name, email, passwordHash, status, roles);
    }
}
