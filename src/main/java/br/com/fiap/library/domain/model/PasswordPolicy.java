package br.com.fiap.library.domain.model;

import br.com.fiap.library.domain.exception.InvalidDataException;
import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {
    private static final int MIN_CHARS = 8;
    private static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static boolean accepts(String password) {
        return password != null
                && password.length() >= MIN_CHARS
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }

    public static void validate(String password) {
        if (!accepts(password)) {
            throw new InvalidDataException("A senha deve ter ao menos 8 caracteres e no máximo 72 bytes");
        }
    }
}
