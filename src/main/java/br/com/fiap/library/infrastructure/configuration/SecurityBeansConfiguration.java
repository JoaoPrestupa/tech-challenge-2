package br.com.fiap.library.infrastructure.configuration;

import br.com.fiap.library.adapter.out.security.BCryptPasswordHasher;
import br.com.fiap.library.adapter.out.security.JwtAccessTokenIssuer;
import br.com.fiap.library.application.port.out.AccessTokenIssuer;
import br.com.fiap.library.application.port.out.PasswordHasher;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class SecurityBeansConfiguration {
    private static final int MIN_SECRET_BYTES = 32;

    @Bean
    SecretKey jwtSecretKey(LibraryProperties properties) {
        byte[] secret = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET deve ter ao menos " + MIN_SECRET_BYTES + " bytes");
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key, LibraryProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.jwt().issuer()));
        return decoder;
    }

    @Bean
    AccessTokenIssuer accessTokenIssuer(SecretKey key, LibraryProperties properties, Clock clock) {
        return new JwtAccessTokenIssuer(new NimbusJwtEncoder(new ImmutableSecret<>(key)),
                properties.jwt().issuer(), properties.jwt().accessTtl(), clock);
    }

    @Bean
    PasswordHasher passwordHasher(LibraryProperties properties) {
        return new BCryptPasswordHasher(new BCryptPasswordEncoder(properties.security().bcryptStrength()));
    }
}
