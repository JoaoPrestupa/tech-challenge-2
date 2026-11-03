package br.com.fiap.library.adapter.out.persistence;

import br.com.fiap.library.application.model.CopyCount;
import br.com.fiap.library.application.port.out.BookCopyRepository;
import br.com.fiap.library.domain.model.BookCopy;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class BookCopyPersistenceAdapter implements BookCopyRepository {
    private final BookCopyJpaRepository repository;

    BookCopyPersistenceAdapter(BookCopyJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public BookCopy save(BookCopy copy) {
        return repository.save(new BookCopyEntity(copy)).toDomain();
    }

    @Override
    public List<BookCopy> saveAll(List<BookCopy> copies) {
        return repository.saveAll(copies.stream().map(BookCopyEntity::new).toList())
                .stream().map(BookCopyEntity::toDomain).toList();
    }

    @Override
    public Optional<BookCopy> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id).map(BookCopyEntity::toDomain);
    }

    @Override
    public List<BookCopy> findByBookId(Long bookId) {
        return repository.findByBookIdOrderByAssetCode(bookId).stream().map(BookCopyEntity::toDomain).toList();
    }

    @Override
    public Map<Long, CopyCount> countByBookIds(Collection<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        return repository.countByBookIds(bookIds).stream().collect(Collectors.toMap(
                BookCopyJpaRepository.CountRow::bookId,
                row -> new CopyCount(row.total(), row.available())));
    }

    @Override
    public Set<String> findExistingAssetCodes(Collection<String> assetCodes) {
        return new HashSet<>(repository.findExistingAssetCodes(assetCodes));
    }

    @Override
    public void inactivateAllByBookId(Long bookId) {
        repository.inactivateAllByBookId(bookId);
    }
}
