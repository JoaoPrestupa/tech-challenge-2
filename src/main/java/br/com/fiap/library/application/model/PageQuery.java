package br.com.fiap.library.application.model;

import br.com.fiap.library.domain.exception.InvalidDataException;

public record PageQuery(int page, int size, String sort, boolean ascending) {
    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new InvalidDataException("A página não pode ser negativa");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidDataException("O tamanho da página deve estar entre 1 e " + MAX_SIZE);
        }
    }
}
