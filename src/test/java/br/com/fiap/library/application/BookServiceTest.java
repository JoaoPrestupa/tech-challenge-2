package br.com.fiap.library.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.library.application.model.NewBook;
import br.com.fiap.library.application.port.out.BookCopyRepository;
import br.com.fiap.library.application.port.out.BookRepository;
import br.com.fiap.library.application.port.out.LoanRepository;
import br.com.fiap.library.application.port.out.ReservationRepository;
import br.com.fiap.library.application.usecase.BookService;
import br.com.fiap.library.application.usecase.ReservationQueue;
import br.com.fiap.library.domain.exception.ConflictException;
import br.com.fiap.library.domain.exception.InvalidDataException;
import br.com.fiap.library.domain.model.Book;
import br.com.fiap.library.domain.service.LibraryPolicy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BookServiceTest {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final String ISBN = "9788535902778";

    private final BookRepository books = mock();
    private final BookCopyRepository copies = mock();
    private final LoanRepository loans = mock();
    private final ReservationRepository reservations = mock();
    private BookService service;

    @BeforeEach
    void setUp() {
        LibraryPolicy policy = new LibraryPolicy(14, 3, Duration.ofHours(48), ZONE);
        service = new BookService(books, copies, loans, reservations,
                new ReservationQueue(reservations, copies, policy), new DirectUnitOfWork(),
                Clock.fixed(Instant.parse("2026-11-02T13:00:00Z"), ZONE));
        lenient().when(books.findExistingIsbns(any())).thenReturn(Set.of());
        lenient().when(copies.findExistingAssetCodes(any())).thenReturn(Set.of());
        lenient().when(copies.countByBookIds(any())).thenReturn(Map.of());
        lenient().when(books.saveAll(any())).thenAnswer(call -> {
            List<Book> list = call.getArgument(0);
            return list.stream().map(b -> new Book(1L, b.title(), b.author(), b.isbn(), b.description(), true)).toList();
        });
        lenient().when(books.findActiveById(1L)).thenReturn(Optional.of(book()));
    }

    private static Book book() {
        return new Book(1L, "Dom Casmurro", "Machado de Assis", ISBN, null, true);
    }

    private static NewBook newBook(String isbn, String... codes) {
        return new NewBook("Dom Casmurro", "Machado de Assis", isbn, null, List.of(codes));
    }

    @Test
    void creates_a_book_with_its_copies() {
        var view = service.create(newBook("978-85-359-0277-8", "EX-1", "EX-2"));

        assertThat(view.book().isbn()).isEqualTo(ISBN);
        verify(copies).saveAll(org.mockito.ArgumentMatchers.argThat(list -> list.size() == 2));
    }

    @Test
    void batch_rejects_the_same_isbn_written_in_different_ways() {
        assertThatThrownBy(() -> service.createBatch(List.of(newBook(ISBN), newBook("978-85-359-0277-8"))))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining(ISBN);
        verify(books, never()).saveAll(any());
    }

    @Test
    void batch_rejects_a_repeated_asset_code() {
        assertThatThrownBy(() -> service.createBatch(List.of(newBook(ISBN, "EX-1"), newBook("9780306406157", "EX-1"))))
                .isInstanceOf(InvalidDataException.class);
    }

    @Test
    void batch_is_cancelled_when_an_isbn_already_exists() {
        when(books.findExistingIsbns(any())).thenReturn(Set.of(ISBN));

        assertThatThrownBy(() -> service.createBatch(List.of(newBook(ISBN), newBook("9780306406157"))))
                .isInstanceOf(ConflictException.class);
        verify(books, never()).saveAll(any());
    }

    @Test
    void empty_batch_is_rejected() {
        assertThatThrownBy(() -> service.createBatch(List.of())).isInstanceOf(InvalidDataException.class);
    }

    @Test
    void delete_is_blocked_by_an_active_loan() {
        when(loans.existsActiveByBook(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void delete_is_blocked_by_an_open_reservation() {
        when(reservations.existsOpenByBook(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void book_without_history_is_deleted() {
        service.delete(1L);

        verify(books).delete(1L);
    }

    @Test
    void book_with_history_is_deactivated_with_its_copies() {
        when(loans.existsByBook(1L)).thenReturn(true);

        service.delete(1L);

        verify(books, never()).delete(any());
        verify(copies).inactivateAllByBookId(1L);
        ArgumentCaptor<Book> saved = ArgumentCaptor.forClass(Book.class);
        verify(books).save(saved.capture());
        assertThat(saved.getValue().active()).isFalse();
    }
}
