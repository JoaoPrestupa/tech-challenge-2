package br.com.fiap.library.domain.model;

import br.com.fiap.library.domain.exception.InvalidDataException;

public final class Isbn {
    private Isbn() {
    }

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidDataException("ISBN é obrigatório");
        }
        String isbn = raw.replaceAll("[\\s-]", "").toUpperCase();
        boolean valid = switch (isbn.length()) {
            case 10 -> isValid10(isbn);
            case 13 -> isValid13(isbn);
            default -> false;
        };
        if (!valid) {
            throw new InvalidDataException("ISBN inválido: " + raw);
        }
        return isbn;
    }

    private static boolean isValid10(String isbn) {
        if (!isbn.matches("\\d{9}[\\dX]")) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            char c = isbn.charAt(i);
            sum += (10 - i) * (c == 'X' ? 10 : c - '0');
        }
        return sum % 11 == 0;
    }

    private static boolean isValid13(String isbn) {
        if (!isbn.matches("\\d{13}")) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 13; i++) {
            sum += (isbn.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return sum % 10 == 0;
    }
}
