package br.com.fiap.library.domain.exception;

public class NotFoundException extends DomainException {
    public NotFoundException(String message) {
        super(message);
    }
}
