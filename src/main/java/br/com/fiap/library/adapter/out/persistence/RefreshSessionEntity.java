package br.com.fiap.library.adapter.out.persistence;

import br.com.fiap.library.domain.model.RefreshSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "refresh_sessions")
class RefreshSessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "user_id", nullable = false)
    Long userId;
    @Column(name = "token_hash", nullable = false, length = 64)
    String tokenHash;
    @Column(name = "issued_at", nullable = false)
    Instant issuedAt;
    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;
    @Column(name = "revoked_at")
    Instant revokedAt;
    @Column(name = "replaced_by")
    Long replacedBy;

    protected RefreshSessionEntity() {
    }

    RefreshSessionEntity(RefreshSession session) {
        id = session.id();
        userId = session.userId();
        tokenHash = session.tokenHash();
        issuedAt = session.issuedAt();
        expiresAt = session.expiresAt();
        revokedAt = session.revokedAt();
        replacedBy = session.replacedBy();
    }

    RefreshSession toDomain() {
        return new RefreshSession(id, userId, tokenHash, issuedAt, expiresAt, revokedAt, replacedBy);
    }
}
