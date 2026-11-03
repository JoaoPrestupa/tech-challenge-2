package br.com.fiap.library.application.port.out;

import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.domain.model.LibraryUser;
import java.util.Optional;

public interface UserRepository {
    LibraryUser save(LibraryUser user);

    Optional<LibraryUser> findById(Long id);

    Optional<LibraryUser> findByIdForUpdate(Long id);

    Optional<LibraryUser> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    PageResult<LibraryUser> findAll(PageQuery page);

    long countActiveLibrarians();
}
