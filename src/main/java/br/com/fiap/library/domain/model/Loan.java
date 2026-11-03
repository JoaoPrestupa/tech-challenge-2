package br.com.fiap.library.domain.model;

import br.com.fiap.library.domain.exception.ConflictException;
import java.time.Instant;

public record Loan(Long id, Long userId, Long bookCopyId, Instant borrowedAt, Instant dueAt, Instant returnedAt) {

    public static Loan open(Long userId, Long bookCopyId, Instant borrowedAt, Instant dueAt) {
        return new Loan(null, userId, bookCopyId, borrowedAt, dueAt, null);
    }

    public Loan close(Instant at) {
        if (!isActive()) {
            throw new ConflictException("Este empréstimo já foi devolvido");
        }
        return new Loan(id, userId, bookCopyId, borrowedAt, dueAt, at);
    }

    public boolean isActive() {
        return returnedAt == null;
    }
}
