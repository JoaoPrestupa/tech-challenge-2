package br.com.fiap.library.application.port.out;

import br.com.fiap.library.application.model.CopyCount;
import br.com.fiap.library.domain.model.BookCopy;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface BookCopyRepository {
    BookCopy save(BookCopy copy);

    List<BookCopy> saveAll(List<BookCopy> copies);

    Optional<BookCopy> findByIdForUpdate(Long id);

    List<BookCopy> findByBookId(Long bookId);

    Map<Long, CopyCount> countByBookIds(Collection<Long> bookIds);

    Set<String> findExistingAssetCodes(Collection<String> assetCodes);

    void inactivateAllByBookId(Long bookId);
}
