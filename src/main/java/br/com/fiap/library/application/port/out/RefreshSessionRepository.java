package br.com.fiap.library.application.port.out;

import br.com.fiap.library.domain.model.RefreshSession;
import java.time.Instant;
import java.util.Optional;

public interface RefreshSessionRepository {
    RefreshSession save(RefreshSession session);

    Optional<RefreshSession> findByTokenHashForUpdate(String tokenHash);

    void revokeAllByUser(Long userId, Instant at);
}
