package br.com.fiap.library.application.port.out;

public interface OpaqueTokenGenerator {
    String generate();

    String hash(String token);
}
