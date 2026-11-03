package br.com.fiap.library.domain.model;

public record Book(Long id, String title, String author, String isbn, String description, boolean active) {

    public static Book create(String title, String author, String isbn, String description) {
        return new Book(null,
                Text.required(title, "Título", 255),
                Text.required(author, "Autor", 255),
                Isbn.normalize(isbn),
                Text.optional(description, 5000, "Descrição"),
                true);
    }

    public Book update(String newTitle, String newAuthor, String newIsbn, String newDescription) {
        Book changed = create(newTitle, newAuthor, newIsbn, newDescription);
        return new Book(id, changed.title, changed.author, changed.isbn, changed.description, active);
    }

    public Book deactivate() {
        return new Book(id, title, author, isbn, description, false);
    }
}
