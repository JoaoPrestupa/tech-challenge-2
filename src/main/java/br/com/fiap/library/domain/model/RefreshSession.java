package br.com.fiap.library.domain.model;

import java.time.Instant;

public record RefreshSession(Long id, Long userId, String tokenHash, Instant issuedAt, Instant expiresAt,
                             Instant revokedAt, Long replacedBy) {

    public static RefreshSession issue(Long userId, String tokenHash, Instant issuedAt, Instant expiresAt) {
        return new RefreshSession(null, userId, tokenHash, issuedAt, expiresAt, null, null);
    }

    public RefreshSession revoke(Instant at, Long replacement) {
        return new RefreshSession(id, userId, tokenHash, issuedAt, expiresAt, at, replacement);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
