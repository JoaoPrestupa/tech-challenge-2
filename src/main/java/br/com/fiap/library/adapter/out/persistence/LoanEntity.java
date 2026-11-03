package br.com.fiap.library.adapter.out.persistence;

import br.com.fiap.library.domain.model.Loan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "loans")
class LoanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "user_id", nullable = false)
    Long userId;
    @Column(name = "book_copy_id", nullable = false)
    Long bookCopyId;
    @Column(name = "borrowed_at", nullable = false)
    Instant borrowedAt;
    @Column(name = "due_at", nullable = false)
    Instant dueAt;
    @Column(name = "returned_at")
    Instant returnedAt;

    protected LoanEntity() {
    }

    LoanEntity(Loan loan) {
        id = loan.id();
        userId = loan.userId();
        bookCopyId = loan.bookCopyId();
        borrowedAt = loan.borrowedAt();
        dueAt = loan.dueAt();
        returnedAt = loan.returnedAt();
    }

    Loan toDomain() {
        return new Loan(id, userId, bookCopyId, borrowedAt, dueAt, returnedAt);
    }
}
