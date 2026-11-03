package br.com.fiap.library.application.model;

import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {
}
