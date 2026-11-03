package br.com.fiap.library.adapter.out.persistence;

import br.com.fiap.library.domain.model.BookCopy;
import br.com.fiap.library.domain.model.CopyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "book_copies")
class BookCopyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "book_copies_seq")
    @SequenceGenerator(name = "book_copies_seq", sequenceName = "book_copies_seq", allocationSize = 50)
    Long id;
    @Column(name = "book_id", nullable = false)
    Long bookId;
    @Column(name = "asset_code", nullable = false, length = 60)
    String assetCode;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    CopyStatus status;

    protected BookCopyEntity() {
    }

    BookCopyEntity(BookCopy copy) {
        id = copy.id();
        bookId = copy.bookId();
        assetCode = copy.assetCode();
        status = copy.status();
    }

    BookCopy toDomain() {
        return new BookCopy(id, bookId, assetCode, status);
    }
}
