package br.com.fiap.library.adapter.out.persistence;

import br.com.fiap.library.domain.model.Book;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "books")
class BookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "books_seq")
    @SequenceGenerator(name = "books_seq", sequenceName = "books_seq", allocationSize = 50)
    Long id;
    @Column(nullable = false)
    String title;
    @Column(nullable = false)
    String author;
    @Column(nullable = false, length = 13)
    String isbn;
    @Column(columnDefinition = "text")
    String description;
    @Column(nullable = false)
    boolean active;

    protected BookEntity() {
    }

    BookEntity(Book book) {
        id = book.id();
        title = book.title();
        author = book.author();
        isbn = book.isbn();
        description = book.description();
        active = book.active();
    }

    Book toDomain() {
        return new Book(id, title, author, isbn, description, active);
    }
}
