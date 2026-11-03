package br.com.fiap.library.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import br.com.fiap.library.support.IntegrationTestBase;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ApiContractIT extends IntegrationTestBase {
    @Autowired
    private JdbcTemplate jdbc;

    @ParameterizedTest
    @CsvSource({"GET,/books", "GET,/users", "GET,/loans", "GET,/reports/most-borrowed", "GET,/auth/me", "POST,/books"})
    void protected_routes_require_a_token(String method, String path) {
        api().request(method, path).then().statusCode(401)
                .contentType(containsString("application/problem+json"));
    }

    @ParameterizedTest
    @CsvSource({"GET,/users", "GET,/loans", "GET,/reports/most-borrowed", "GET,/reports/active-loans", "POST,/books",
            "POST,/books/batch", "POST,/loans", "POST,/users"})
    void member_cannot_use_librarian_routes(String method, String path) {
        String email = uniqueEmail();
        registerMember(email);

        as(login(email, "senha-do-membro")).body("{}").request(method, path).then().statusCode(403);
    }

    @Test
    void tampered_token_is_rejected() {
        String token = librarianToken();

        as(token.substring(0, token.length() - 3) + "abc").get("/books").then().statusCode(401);
    }

    @Test
    void api_is_served_only_under_the_context_path() {
        io.restassured.RestAssured.given().basePath("/").get("/v1/books").then().statusCode(not(equalTo(200)));
    }

    @Test
    void documentation_and_health_are_public() {
        api().get("/v3/api-docs").then().statusCode(200).body("info.title", equalTo("API de Biblioteca Online"));
        api().get("/swagger-ui/index.html").then().statusCode(200);
        api().get("/actuator/health").then().statusCode(200).body("status", equalTo("UP"));
    }

    @Test
    void public_register_ignores_any_role_sent_by_the_client() {
        api().body(json(Map.of("name", "Invasor", "email", uniqueEmail(), "password", "senha-do-membro",
                        "roles", List.of("ROLE_LIBRARIAN"))))
                .post("/auth/register").then().statusCode(201)
                .body("roles", equalTo(List.of("ROLE_MEMBER")));
    }

    @Test
    void responses_never_expose_the_password() {
        api().body(json(Map.of("name", "Maria", "email", uniqueEmail(), "password", "senha-do-membro")))
                .post("/auth/register").then().statusCode(201)
                .body("password", nullValue())
                .body("passwordHash", nullValue());
    }

    @Test
    void password_is_stored_as_bcrypt() {
        String email = uniqueEmail();
        registerMember(email);

        String hash = jdbc.queryForObject("select password_hash from library_users where normalized_email = ?",
                String.class, email);
        assertThat(hash).startsWith("$2").isNotEqualTo("senha-do-membro");
    }

    @Test
    void refresh_token_is_stored_only_as_hash() {
        String email = uniqueEmail();
        registerMember(email);
        String refresh = api().body(json(Map.of("email", email, "password", "senha-do-membro")))
                .post("/auth/login").then().statusCode(200).extract().path("refreshToken");

        Long stored = jdbc.queryForObject("select count(*) from refresh_sessions where token_hash = ?", Long.class, refresh);
        assertThat(stored).isZero();
    }

    @Test
    void validation_errors_list_the_fields() {
        as(librarianToken()).body(json(Map.of("title", "", "author", "A", "isbn", "1")))
                .post("/books").then().statusCode(400)
                .body("errors", hasItem(containsString("title")));
    }

    @Test
    void invalid_isbn_checksum_returns_400() {
        as(librarianToken()).body(json(Map.of("title", "T", "author", "A", "isbn", "9780306406158")))
                .post("/books").then().statusCode(400).body("detail", containsString("ISBN"));
    }

    @Test
    void duplicate_isbn_returns_409() {
        String librarian = librarianToken();
        String body = json(Map.of("title", "T", "author", "A", "isbn", uniqueIsbn()));
        as(librarian).body(body).post("/books").then().statusCode(201);

        as(librarian).body(body).post("/books").then().statusCode(409);
    }

    @Test
    void malformed_json_returns_400() {
        as(librarianToken()).body("{quebrado").post("/books").then().statusCode(400);
    }

    @Test
    void unknown_book_returns_404() {
        as(librarianToken()).get("/books/{id}", 999_999_999L).then().statusCode(404);
    }

    @Test
    void utf8_round_trip_keeps_accents() {
        String librarian = librarianToken();
        long id = as(librarian).body(json(Map.of("title", "Memórias Póstumas de Brás Cubas", "author", "João Ñandú",
                        "isbn", uniqueIsbn())))
                .post("/books").then().statusCode(201).extract().<Number>path("id").longValue();

        as(librarian).get("/books/{id}", id).then().statusCode(200)
                .contentType(containsString("json"))
                .body("title", equalTo("Memórias Póstumas de Brás Cubas"))
                .body("author", equalTo("João Ñandú"));
    }

    @Test
    void search_filters_by_title_author_isbn_and_availability() {
        String librarian = librarianToken();
        String marker = "Filtro" + uniqueCode();
        String isbn = uniqueIsbn();
        long available = as(librarian).body(json(Map.of("title", marker + " Disponível", "author", "Clarice",
                        "isbn", isbn, "assetCodes", List.of(uniqueCode()))))
                .post("/books").then().statusCode(201).extract().<Number>path("id").longValue();
        long unavailable = createBook(librarian, marker + " Sem Exemplar");

        as(librarian).queryParam("title", marker.toLowerCase()).get("/books").then().statusCode(200)
                .body("totalElements", equalTo(2));
        as(librarian).queryParam("title", marker).queryParam("available", true).get("/books").then().statusCode(200)
                .body("content.id.collect { it as long }", equalTo(List.of(available)));
        as(librarian).queryParam("title", marker).queryParam("available", false).get("/books").then().statusCode(200)
                .body("content.id.collect { it as long }", equalTo(List.of(unavailable)));
        as(librarian).queryParam("isbn", isbn).get("/books").then().statusCode(200)
                .body("content[0].id", notNullValue())
                .body("content[0].isbn", equalTo(isbn))
                .body("content[0].available", equalTo(true))
                .body("content[0].availableCopies", equalTo(1));
    }

    @Test
    void like_wildcards_in_the_search_are_plain_text() {
        as(librarianToken()).queryParam("title", "%%%naoexiste%%%").get("/books").then().statusCode(200)
                .body("totalElements", equalTo(0));
    }

    @Test
    void pagination_and_sorting_work_and_are_limited() {
        String librarian = librarianToken();
        String marker = "Pagina" + uniqueCode();
        createBook(librarian, marker + " B");
        createBook(librarian, marker + " A");
        createBook(librarian, marker + " C");

        as(librarian).queryParam("title", marker).queryParam("size", 2).queryParam("sort", "title")
                .get("/books").then().statusCode(200)
                .body("content", hasSize(2))
                .body("content[0].title", equalTo(marker + " A"))
                .body("totalElements", equalTo(3))
                .body("totalPages", equalTo(2));
        as(librarian).queryParam("title", marker).queryParam("sort", "title").queryParam("direction", "desc")
                .get("/books").then().body("content[0].title", equalTo(marker + " C"));
        as(librarian).queryParam("size", 101).get("/books").then().statusCode(400);
        as(librarian).queryParam("sort", "senha").get("/books").then().statusCode(400);
    }

    @Test
    void batch_creates_everything_or_nothing() {
        String librarian = librarianToken();
        String marker = "Lote" + uniqueCode();
        String taken = uniqueIsbn();
        as(librarian).body(json(Map.of("title", "Já existe", "author", "A", "isbn", taken)))
                .post("/books").then().statusCode(201);

        as(librarian).body(json(Map.of("books", List.of(
                        Map.of("title", marker, "author", "A", "isbn", uniqueIsbn()),
                        Map.of("title", marker, "author", "A", "isbn", taken)))))
                .post("/books/batch").then().statusCode(409);
        as(librarian).queryParam("title", marker).get("/books").then().body("totalElements", equalTo(0));

        List<Map<String, Object>> books = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            books.add(Map.of("title", marker + " " + i, "author", "A", "isbn", uniqueIsbn(),
                    "assetCodes", List.of(uniqueCode(), uniqueCode())));
        }
        as(librarian).body(json(Map.of("books", books))).post("/books/batch").then().statusCode(201)
                .body("$", hasSize(120))
                .body("totalCopies", everyItem(equalTo(2)));
    }

    @Test
    void book_without_history_is_deleted_and_with_history_is_hidden() {
        String librarian = librarianToken();
        long plain = createBook(librarian, "Sem histórico");
        as(librarian).delete("/books/{id}", plain).then().statusCode(204);
        assertThat(jdbc.queryForObject("select count(*) from books where id = ?", Long.class, plain)).isZero();

        long used = createBook(librarian, "Com histórico");
        long copy = addCopy(librarian, used);
        long member = registerMember(uniqueEmail());
        long loan = as(librarian).body(json(Map.of("userId", member, "bookCopyId", copy)))
                .post("/loans").then().statusCode(201).extract().<Number>path("id").longValue();
        as(librarian).delete("/books/{id}", used).then().statusCode(409);
        as(librarian).post("/loans/{id}/return", loan).then().statusCode(200);
        as(librarian).delete("/books/{id}", used).then().statusCode(204);

        as(librarian).get("/books/{id}", used).then().statusCode(404);
        assertThat(jdbc.queryForObject("select active from books where id = ?", Boolean.class, used)).isFalse();
        as(librarian).get("/loans/{id}", loan).then().statusCode(200);
    }

    @Test
    void dates_use_iso_8601_with_the_sao_paulo_offset() {
        String librarian = librarianToken();
        long copy = addCopy(librarian, createBook(librarian, "Datas"));
        long member = registerMember(uniqueEmail());

        as(librarian).body(json(Map.of("userId", member, "bookCopyId", copy)))
                .post("/loans").then().statusCode(201)
                .body("borrowedAt", endsWith("-03:00"))
                .body("dueAt", containsString("T23:59:59-03:00"))
                .body("daysUntilDue", equalTo(14))
                .body("overdue", equalTo(false));
    }

    @Test
    void reports_rank_at_most_20_books_with_counts_descending() {
        String librarian = librarianToken();
        long member = registerMember(uniqueEmail());
        long book = createBook(librarian, "Campeão de empréstimos");
        long copy = addCopy(librarian, book);
        for (int i = 0; i < 3; i++) {
            long loan = as(librarian).body(json(Map.of("userId", member, "bookCopyId", copy)))
                    .post("/loans").then().statusCode(201).extract().<Number>path("id").longValue();
            as(librarian).post("/loans/{id}/return", loan).then().statusCode(200);
        }

        var response = as(librarian).get("/reports/most-borrowed").then().statusCode(200)
                .body("size()", lessThanOrEqualTo(20))
                .body("find { it.title == 'Campeão de empréstimos' }.loans", equalTo(3))
                .extract().jsonPath();
        List<Integer> counts = response.getList("loans", Integer.class);
        assertThat(counts).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }
}
