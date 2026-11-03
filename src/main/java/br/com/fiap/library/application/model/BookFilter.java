package br.com.fiap.library.application.model;

public record BookFilter(String title, String author, String isbn, Boolean available) {
}
