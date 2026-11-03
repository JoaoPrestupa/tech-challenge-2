package br.com.fiap.library.domain.model;

import br.com.fiap.library.domain.exception.InvalidDataException;

final class Text {
    private Text() {
    }

    static String required(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new InvalidDataException(field + " é obrigatório");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new InvalidDataException(field + " deve ter no máximo " + maxLength + " caracteres");
        }
        return trimmed;
    }

    static String optional(String value, int maxLength, String field) {
        return value == null || value.isBlank() ? null : required(value, field, maxLength);
    }
}
