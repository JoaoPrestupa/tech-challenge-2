package br.com.fiap.library.adapter.in.web;

import br.com.fiap.library.application.model.BookFilter;
import br.com.fiap.library.application.model.PageQuery;
import br.com.fiap.library.application.model.PageResult;
import br.com.fiap.library.application.port.in.BookManagement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/books")
@Tag(name = "Livros e exemplares")
class BookController {
    private final BookManagement books;
    private final Responses responses;

    BookController(BookManagement books, Responses responses) {
        this.books = books;
        this.responses = responses;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Responses.BookResponse create(@Valid @RequestBody Requests.BookRequest request) {
        return responses.book(books.create(request.toNewBook()));
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra livros em lote", description = "Tudo ou nada: um item inválido cancela o lote.")
    List<Responses.BookResponse> createBatch(@Valid @RequestBody Requests.BookBatchRequest request) {
        return books.createBatch(request.books().stream().map(Requests.BookRequest::toNewBook).toList())
                .stream().map(responses::book).toList();
    }

    @GetMapping
    @Operation(summary = "Busca livros por título, autor, ISBN e disponibilidade")
    PageResult<Responses.BookResponse> search(@RequestParam(required = false) String title,
                                              @RequestParam(required = false) String author,
                                              @RequestParam(required = false) String isbn,
                                              @RequestParam(required = false) Boolean available,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestParam(defaultValue = "title") String sort,
                                              @RequestParam(defaultValue = "asc") String direction) {
        return books.search(new BookFilter(title, author, isbn, available),
                        new PageQuery(page, size, sort, !"desc".equalsIgnoreCase(direction)))
                .map(responses::book);
    }

    @GetMapping("/{id}")
    Responses.BookResponse get(@PathVariable Long id) {
        return responses.book(books.get(id));
    }

    @PutMapping("/{id}")
    Responses.BookResponse update(@PathVariable Long id, @Valid @RequestBody Requests.BookRequest request) {
        return responses.book(books.update(id, request.title(), request.author(), request.isbn(), request.description()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui o livro", description = "Bloqueado com empréstimo ativo ou reserva aberta. "
            + "Sem histórico, exclui de fato. Com histórico, desativa.")
    void delete(@PathVariable Long id) {
        books.delete(id);
    }

    @PostMapping("/{id}/copies")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um ou mais exemplares (lote)")
    List<Responses.CopyResponse> addCopies(@PathVariable Long id, @Valid @RequestBody Requests.CopiesRequest request) {
        return books.addCopies(id, request.assetCodes()).stream().map(responses::copy).toList();
    }

    @GetMapping("/{id}/copies")
    List<Responses.CopyResponse> listCopies(@PathVariable Long id) {
        return books.listCopies(id).stream().map(responses::copy).toList();
    }

    @DeleteMapping("/{id}/copies/{copyId}")
    @Operation(summary = "Inativa um exemplar disponível")
    Responses.CopyResponse inactivateCopy(@PathVariable Long id, @PathVariable Long copyId) {
        return responses.copy(books.inactivateCopy(id, copyId));
    }
}
