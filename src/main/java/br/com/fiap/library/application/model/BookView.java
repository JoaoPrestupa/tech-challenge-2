package br.com.fiap.library.application.model;

import br.com.fiap.library.domain.model.Book;

public record BookView(Book book, long totalCopies, long availableCopies) {
    public boolean available() {
        return availableCopies > 0;
    }
}
