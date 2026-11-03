package br.com.fiap.library.adapter.out.persistence;

import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.application.port.out.UserRepository;
import br.com.fiap.library.domain.model.LibraryUser;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class UserPersistenceAdapter implements UserRepository {
    private static final Map<String, String> SORTABLE = Map.of("name", "name", "email", "email");

    private final UserJpaRepository repository;

    UserPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public LibraryUser save(LibraryUser user) {
        return repository.save(new UserEntity(user)).toDomain();
    }

    @Override
    public Optional<LibraryUser> findById(Long id) {
        return repository.findById(id).map(UserEntity::toDomain);
    }

    @Override
    public Optional<LibraryUser> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id).map(UserEntity::toDomain);
    }

    @Override
    public Optional<LibraryUser> findByEmail(String email) {
        return repository.findByEmail(email).map(UserEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, Long id) {
        return repository.existsByEmailAndIdNot(email, id);
    }

    @Override
    public PageResult<LibraryUser> findAll(PageQuery page) {
        return Pages.result(repository.findAll(Pages.of(page, SORTABLE, "name")).map(UserEntity::toDomain));
    }

    @Override
    public long countActiveLibrarians() {
        return repository.countActiveLibrarians();
    }
}
