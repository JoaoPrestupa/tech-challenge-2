package br.com.fiap.library.infrastructure.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("library")
public record LibraryProperties(Policy policy, Jwt jwt, Security security, Bootstrap bootstrap) {

    public record Policy(int loanDays, int maxActiveLoans, Duration reservationHold, String timezone,
                         Duration expiryCheckInterval) {
    }

    public record Jwt(String secret, String issuer, Duration accessTtl, Duration refreshTtl) {
    }

    public record Security(int bcryptStrength) {
    }

    public record Bootstrap(String name, String email, String password) {
        public boolean isConfigured() {
            return email != null && !email.isBlank() && password != null && !password.isBlank();
        }
    }
}
