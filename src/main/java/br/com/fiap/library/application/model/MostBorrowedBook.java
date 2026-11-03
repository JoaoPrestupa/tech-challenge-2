package br.com.fiap.library.application.model;

public record MostBorrowedBook(Long bookId, String title, String author, String isbn, long loans) {
}
