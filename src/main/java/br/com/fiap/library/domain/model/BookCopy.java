package br.com.fiap.library.domain.model;

import br.com.fiap.library.domain.exception.ConflictException;

public record BookCopy(Long id, Long bookId, String assetCode, CopyStatus status) {

    public static BookCopy create(Long bookId, String assetCode) {
        return new BookCopy(null, bookId, Text.required(assetCode, "Código do exemplar", 60), CopyStatus.AVAILABLE);
    }

    public BookCopy loan() {
        if (status != CopyStatus.AVAILABLE && status != CopyStatus.RESERVED) {
            throw new ConflictException("O exemplar " + assetCode + " não está disponível para empréstimo");
        }
        return with(CopyStatus.LOANED);
    }

    public BookCopy reserve() {
        if (status != CopyStatus.AVAILABLE) {
            throw new ConflictException("O exemplar " + assetCode + " não pode ser separado para reserva");
        }
        return with(CopyStatus.RESERVED);
    }

    public BookCopy release() {
        return with(CopyStatus.AVAILABLE);
    }

    public BookCopy inactivate() {
        if (status != CopyStatus.AVAILABLE) {
            throw new ConflictException("Somente um exemplar disponível pode ser inativado");
        }
        return with(CopyStatus.INACTIVE);
    }

    private BookCopy with(CopyStatus newStatus) {
        return new BookCopy(id, bookId, assetCode, newStatus);
    }
}
