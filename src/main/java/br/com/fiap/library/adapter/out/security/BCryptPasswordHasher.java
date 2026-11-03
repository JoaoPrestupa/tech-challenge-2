package br.com.fiap.library.adapter.out.security;

import br.com.fiap.library.application.port.out.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;

public class BCryptPasswordHasher implements PasswordHasher {
    private final PasswordEncoder encoder;

    public BCryptPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }
}
