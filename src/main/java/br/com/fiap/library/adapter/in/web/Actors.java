package br.com.fiap.library.adapter.in.web;

import br.com.fiap.library.application.model.Actor;
import br.com.fiap.library.domain.model.Role;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jwt.Jwt;

final class Actors {
    private Actors() {
    }

    static Actor from(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        Set<Role> parsed = roles == null ? Set.of() : roles.stream().map(Role::valueOf).collect(Collectors.toSet());
        return new Actor(Long.valueOf(jwt.getSubject()), parsed);
    }
}
