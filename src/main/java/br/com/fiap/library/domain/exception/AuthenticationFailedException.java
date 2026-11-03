package br.com.fiap.library.domain.exception;

public class AuthenticationFailedException extends DomainException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}
