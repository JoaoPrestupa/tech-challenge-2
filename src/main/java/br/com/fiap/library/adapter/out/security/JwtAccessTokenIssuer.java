package br.com.fiap.library.adapter.out.security;

import br.com.fiap.library.application.model.AccessToken;
import br.com.fiap.library.application.port.out.AccessTokenIssuer;
import br.com.fiap.library.domain.model.LibraryUser;
import br.com.fiap.library.domain.model.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

public class JwtAccessTokenIssuer implements AccessTokenIssuer {
    private final JwtEncoder encoder;
    private final String issuer;
    private final Duration ttl;
    private final Clock clock;

    public JwtAccessTokenIssuer(JwtEncoder encoder, String issuer, Duration ttl, Clock clock) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.ttl = ttl;
        this.clock = clock;
    }

    @Override
    public AccessToken issue(LibraryUser user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(ttl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.id().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("roles", user.roles().stream().map(Role::name).sorted().toList())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return new AccessToken(encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(), expiresAt);
    }
}
