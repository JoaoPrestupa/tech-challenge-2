package br.com.fiap.library.application.usecase;

import br.com.fiap.library.application.model.BookFilter;
import br.com.fiap.library.application.model.BookView;
import br.com.fiap.library.application.model.CopyCount;
import br.com.fiap.library.application.model.NewBook;
import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.application.port.in.BookManagement;
import br.com.fiap.library.application.port.out.BookCopyRepository;
import br.com.fiap.library.application.port.out.BookRepository;
import br.com.fiap.library.application.port.out.LoanRepository;
import br.com.fiap.library.application.port.out.ReservationRepository;
import br.com.fiap.library.application.port.out.UnitOfWork;
import br.com.fiap.library.domain.exception.ConflictException;
import br.com.fiap.library.domain.exception.InvalidDataException;
import br.com.fiap.library.domain.exception.NotFoundException;
import br.com.fiap.library.domain.model.Book;
import br.com.fiap.library.domain.model.BookCopy;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BookService implements BookManagement {
    public static final int MAX_BATCH_SIZE = 500;

    private final BookRepository books;
    private final BookCopyRepository copies;
    private final LoanRepository loans;
    private final ReservationRepository reservations;
    private final ReservationQueue queue;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public BookService(BookRepository books, BookCopyRepository copies, LoanRepository loans,
                       ReservationRepository reservations, ReservationQueue queue,
                       UnitOfWork unitOfWork, Clock clock) {
        this.books = books;
        this.copies = copies;
        this.loans = loans;
        this.reservations = reservations;
        this.queue = queue;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public BookView create(NewBook book) {
        return createBatch(List.of(book)).getFirst();
    }

    /** Operação em lote do tipo tudo ou nada: um item inválido cancela o lote inteiro. */
    @Override
    public List<BookView> createBatch(List<NewBook> newBooks) {
        if (newBooks == null || newBooks.isEmpty() || newBooks.size() > MAX_BATCH_SIZE) {
            throw new InvalidDataException("O lote deve ter entre 1 e " + MAX_BATCH_SIZE + " livros");
        }
        List<Book> candidates = newBooks.stream()
                .map(b -> Book.create(b.title(), b.author(), b.isbn(), b.description()))
                .toList();
        List<String> assetCodes = newBooks.stream().flatMap(b -> codesOf(b).stream()).map(String::trim).toList();

        requireNoDuplicates(candidates.stream().map(Book::isbn).toList(), "ISBN");
        requireNoDuplicates(assetCodes, "Código de exemplar");

        return unitOfWork.inTransaction(() -> {
            Set<String> existingIsbns = books.findExistingIsbns(candidates.stream().map(Book::isbn).toList());
            if (!existingIsbns.isEmpty()) {
                throw new ConflictException("ISBN já cadastrado: " + String.join(", ", existingIsbns));
            }
            requireNewAssetCodes(assetCodes);

            List<Book> saved = books.saveAll(candidates);
            List<BookCopy> newCopies = new java.util.ArrayList<>();
            for (int i = 0; i < saved.size(); i++) {
                Long bookId = saved.get(i).id();
                codesOf(newBooks.get(i)).forEach(code -> newCopies.add(BookCopy.create(bookId, code)));
            }
            copies.saveAll(newCopies);
            return toViews(saved);
        });
    }

    @Override
    public BookView get(Long id) {
        return toViews(List.of(requireBook(id))).getFirst();
    }

    @Override
    public PageResult<BookView> search(BookFilter filter, PageQuery page) {
        PageResult<Book> result = books.search(filter, page);
        Map<Long, CopyCount> counts = copies.countByBookIds(result.content().stream().map(Book::id).toList());
        return result.map(book -> viewOf(book, counts));
    }

    @Override
    public BookView update(Long id, String title, String author, String isbn, String description) {
        return unitOfWork.inTransaction(() -> {
            Book updated = requireBook(id).update(title, author, isbn, description);
            if (books.existsByIsbnAndIdNot(updated.isbn(), id)) {
                throw new ConflictException("ISBN já cadastrado: " + updated.isbn());
            }
            return toViews(List.of(books.save(updated))).getFirst();
        });
    }

    /** Sem histórico, o livro é excluído. Com histórico, é desativado para preservar os registros. */
    @Override
    public void delete(Long id) {
        unitOfWork.run(() -> {
            Book book = requireBook(id);
            if (loans.existsActiveByBook(id)) {
                throw new ConflictException("O livro possui empréstimo ativo");
            }
            if (reservations.existsOpenByBook(id)) {
                throw new ConflictException("O livro possui reserva em aberto");
            }
            if (loans.existsByBook(id) || reservations.existsByBook(id)) {
                copies.inactivateAllByBookId(id);
                books.save(book.deactivate());
            } else {
                books.delete(id);
            }
        });
    }

    @Override
    public List<BookCopy> addCopies(Long bookId, List<String> assetCodes) {
        if (assetCodes == null || assetCodes.isEmpty() || assetCodes.size() > MAX_BATCH_SIZE) {
            throw new InvalidDataException("O lote deve ter entre 1 e " + MAX_BATCH_SIZE + " exemplares");
        }
        List<BookCopy> candidates = assetCodes.stream().map(code -> BookCopy.create(bookId, code)).toList();
        List<String> codes = candidates.stream().map(BookCopy::assetCode).toList();
        requireNoDuplicates(codes, "Código de exemplar");

        return unitOfWork.inTransaction(() -> {
            requireBook(bookId);
            requireNewAssetCodes(codes);
            // Um exemplar novo atende primeiro quem já está na fila de reservas.
            return copies.saveAll(candidates).stream()
                    .map(copy -> queue.offer(copy, clock.instant()))
                    .toList();
        });
    }

    @Override
    public List<BookCopy> listCopies(Long bookId) {
        requireBook(bookId);
        return copies.findByBookId(bookId);
    }

    @Override
    public BookCopy inactivateCopy(Long bookId, Long copyId) {
        return unitOfWork.inTransaction(() -> {
            BookCopy copy = copies.findByIdForUpdate(copyId)
                    .filter(c -> c.bookId().equals(bookId))
                    .orElseThrow(() -> new NotFoundException("Exemplar não encontrado: " + copyId));
            return copies.save(copy.inactivate());
        });
    }

    private Book requireBook(Long id) {
        return books.findActiveById(id).orElseThrow(() -> new NotFoundException("Livro não encontrado: " + id));
    }

    private void requireNewAssetCodes(Collection<String> codes) {
        if (codes.isEmpty()) {
            return;
        }
        Set<String> existing = copies.findExistingAssetCodes(codes);
        if (!existing.isEmpty()) {
            throw new ConflictException("Código de exemplar já cadastrado: " + String.join(", ", existing));
        }
    }

    private static List<String> codesOf(NewBook book) {
        return book.assetCodes() == null ? List.of() : book.assetCodes();
    }

    private static void requireNoDuplicates(List<String> values, String label) {
        List<String> duplicates = values.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        if (!duplicates.isEmpty()) {
            throw new InvalidDataException(label + " repetido no lote: " + String.join(", ", duplicates));
        }
    }

    private List<BookView> toViews(List<Book> list) {
        Map<Long, CopyCount> counts = copies.countByBookIds(list.stream().map(Book::id).toList());
        return list.stream().map(book -> viewOf(book, counts)).toList();
    }

    private static BookView viewOf(Book book, Map<Long, CopyCount> counts) {
        CopyCount count = counts.getOrDefault(book.id(), CopyCount.NONE);
        return new BookView(book, count.total(), count.available());
    }
}
