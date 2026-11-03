package br.com.fiap.library.adapter.out.persistence;

import br.com.fiap.library.application.port.out.RefreshSessionRepository;
import br.com.fiap.library.domain.model.RefreshSession;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class RefreshSessionPersistenceAdapter implements RefreshSessionRepository {
    private final RefreshSessionJpaRepository repository;

    RefreshSessionPersistenceAdapter(RefreshSessionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public RefreshSession save(RefreshSession session) {
        return repository.save(new RefreshSessionEntity(session)).toDomain();
    }

    @Override
    public Optional<RefreshSession> findByTokenHashForUpdate(String tokenHash) {
        return repository.findByTokenHashForUpdate(tokenHash).map(RefreshSessionEntity::toDomain);
    }

    @Override
    public void revokeAllByUser(Long userId, Instant at) {
        repository.revokeAllByUser(userId, at);
    }
}
