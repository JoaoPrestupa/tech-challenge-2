package br.com.fiap.library.adapter.out.persistence;

import br.com.fiap.library.application.model.LoanDetails;
import br.com.fiap.library.application.model.LoanFilter;
import br.com.fiap.library.application.model.MostBorrowedBook;
import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.application.port.out.LoanRepository;
import br.com.fiap.library.domain.model.Loan;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
class LoanPersistenceAdapter implements LoanRepository {
    private static final Map<String, String> SORTABLE = Map.of(
            "borrowedAt", "l.borrowedAt", "dueAt", "l.dueAt", "returnedAt", "l.returnedAt");

    private final LoanJpaRepository repository;

    LoanPersistenceAdapter(LoanJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Loan save(Loan loan) {
        return repository.save(new LoanEntity(loan)).toDomain();
    }

    @Override
    public Optional<Loan> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id).map(LoanEntity::toDomain);
    }

    @Override
    public Optional<LoanDetails> findDetailsById(Long id) {
        return repository.findDetailsById(id);
    }

    @Override
    public PageResult<LoanDetails> search(LoanFilter filter, PageQuery page) {
        Pageable pageable = Pages.of(page, SORTABLE, "dueAt");
        Page<LoanDetails> result;
        if (filter.userId() != null) {
            result = filter.active() == null
                    ? repository.findDetailsByUser(filter.userId(), pageable)
                    : filter.active()
                    ? repository.findActiveDetailsByUser(filter.userId(), pageable)
                    : repository.findReturnedDetailsByUser(filter.userId(), pageable);
        } else {
            result = filter.active() == null
                    ? repository.findAllDetails(pageable)
                    : filter.active()
                    ? repository.findActiveDetails(pageable)
                    : repository.findReturnedDetails(pageable);
        }
        return Pages.result(result);
    }

    @Override
    public long countActiveByUser(Long userId) {
        return repository.countByUserIdAndReturnedAtIsNull(userId);
    }

    @Override
    public boolean existsActiveByBook(Long bookId) {
        return repository.existsActiveByBook(bookId);
    }

    @Override
    public boolean existsByBook(Long bookId) {
        return repository.existsByBook(bookId);
    }

    @Override
    public List<MostBorrowedBook> mostBorrowed(int limit) {
        return repository.mostBorrowed(PageRequest.of(0, limit));
    }
}
