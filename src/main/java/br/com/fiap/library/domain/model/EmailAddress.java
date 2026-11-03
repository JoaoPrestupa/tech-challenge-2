package br.com.fiap.library.domain.model;

import br.com.fiap.library.domain.exception.InvalidDataException;
import java.util.Locale;
import java.util.regex.Pattern;

public final class EmailAddress {
    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private EmailAddress() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidDataException("E-mail é obrigatório");
        }
        String email = raw.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 254 || !FORMAT.matcher(email).matches()) {
            throw new InvalidDataException("E-mail inválido");
        }
        return email;
    }
}
