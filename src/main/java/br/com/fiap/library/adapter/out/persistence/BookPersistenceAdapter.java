package br.com.fiap.library.adapter.out.persistence;

import br.com.fiap.library.application.model.BookFilter;
import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.application.port.out.BookRepository;
import br.com.fiap.library.domain.model.Book;
import br.com.fiap.library.domain.model.CopyStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
class BookPersistenceAdapter implements BookRepository {
    private static final Map<String, String> SORTABLE = Map.of("title", "title", "author", "author", "isbn", "isbn");

    private final BookJpaRepository repository;

    BookPersistenceAdapter(BookJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Book save(Book book) {
        return repository.save(new BookEntity(book)).toDomain();
    }

    @Override
    public List<Book> saveAll(List<Book> books) {
        return repository.saveAll(books.stream().map(BookEntity::new).toList())
                .stream().map(BookEntity::toDomain).toList();
    }

    @Override
    public Optional<Book> findActiveById(Long id) {
        return repository.findByIdAndActiveTrue(id).map(BookEntity::toDomain);
    }

    @Override
    public boolean existsByIsbn(String isbn) {
        return repository.existsByIsbn(isbn);
    }

    @Override
    public boolean existsByIsbnAndIdNot(String isbn, Long id) {
        return repository.existsByIsbnAndIdNot(isbn, id);
    }

    @Override
    public Set<String> findExistingIsbns(Collection<String> isbns) {
        return new HashSet<>(repository.findExistingIsbns(isbns));
    }

    @Override
    public PageResult<Book> search(BookFilter filter, PageQuery page) {
        return Pages.result(repository.findAll(specification(filter), Pages.of(page, SORTABLE, "title"))
                .map(BookEntity::toDomain));
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private static Specification<BookEntity> specification(BookFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("active")));
            if (hasText(filter.title())) {
                predicates.add(cb.like(cb.lower(root.get("title")), contains(filter.title()), '\\'));
            }
            if (hasText(filter.author())) {
                predicates.add(cb.like(cb.lower(root.get("author")), contains(filter.author()), '\\'));
            }
            if (hasText(filter.isbn())) {
                predicates.add(cb.equal(root.get("isbn"), filter.isbn().replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT)));
            }
            if (filter.available() != null) {
                Subquery<Long> availableCopy = query.subquery(Long.class);
                Root<BookCopyEntity> copy = availableCopy.from(BookCopyEntity.class);
                availableCopy.select(copy.get("id")).where(
                        cb.equal(copy.get("bookId"), root.get("id")),
                        cb.equal(copy.get("status"), CopyStatus.AVAILABLE));
                predicates.add(filter.available() ? cb.exists(availableCopy) : cb.not(cb.exists(availableCopy)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** Escapa os curingas do LIKE, para que "%" digitado pelo cliente seja tratado como texto. */
    private static String contains(String term) {
        String escaped = term.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
