package br.com.fiap.library.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import br.com.fiap.library.application.port.in.ReservationManagement;
import br.com.fiap.library.support.IntegrationTestBase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class LibraryFlowE2EIT extends IntegrationTestBase {
    private static final String PASSWORD = "senha-do-membro";

    @Autowired
    private ReservationManagement reservations;
    @Autowired
    private JdbcTemplate jdbc;

    private static long id(io.restassured.response.Response response) {
        return response.then().extract().<Number>path("id").longValue();
    }

    @Test
    void complete_library_flow() {
        // 1 e 2. Membros se registram e se autenticam
        String anaEmail = uniqueEmail();
        String brunoEmail = uniqueEmail();
        long ana = registerMember(anaEmail);
        long bruno = registerMember(brunoEmail);
        String anaToken = login(anaEmail, PASSWORD);
        String brunoToken = login(brunoEmail, PASSWORD);
        String librarian = librarianToken();

        // 3. Bibliotecária cadastra livro e exemplar
        String title = "Grande Sertão: Veredas " + uniqueCode();
        long book = as(librarian).body(json(Map.of("title", title, "author", "João Guimarães Rosa", "isbn", uniqueIsbn())))
                .post("/books").then().statusCode(201).extract().<Number>path("id").longValue();
        long copy = addCopy(librarian, book);
        as(anaToken).get("/books/{id}", book).then().statusCode(200).body("available", equalTo(true));

        // Enquanto há exemplar disponível, não se reserva
        as(brunoToken).body(json(Map.of("bookId", book))).post("/reservations").then().statusCode(422);

        // 4. Empréstimo
        long loan = as(librarian).body(json(Map.of("userId", ana, "bookCopyId", copy)))
                .post("/loans").then().statusCode(201).extract().<Number>path("id").longValue();
        as(anaToken).get("/books/{id}", book).then().body("available", equalTo(false));
        // Mesmo exemplar não pode ser emprestado de novo
        as(librarian).body(json(Map.of("userId", bruno, "bookCopyId", copy))).post("/loans").then().statusCode(409);

        // 5. A dona consulta o próprio empréstimo e o histórico
        as(anaToken).get("/loans/{id}", loan).then().statusCode(200).body("bookTitle", equalTo(title));
        as(anaToken).get("/users/{id}/loans", ana).then().statusCode(200).body("totalElements", equalTo(1));

        // 6. Outro membro não acessa os dados dela
        as(brunoToken).get("/loans/{id}", loan).then().statusCode(403);
        as(brunoToken).get("/users/{id}", ana).then().statusCode(403);
        as(brunoToken).get("/users/{id}/loans", ana).then().statusCode(403);
        as(brunoToken).get("/users/{id}/reservations", ana).then().statusCode(403);

        // Relatório de empréstimos ativos
        as(librarian).queryParam("size", 100).get("/reports/active-loans").then().statusCode(200)
                .body("content.bookTitle", hasItem(title));

        // 7. Reserva em fila e devolução
        long brunoReservation = as(brunoToken).body(json(Map.of("bookId", book)))
                .post("/reservations").then().statusCode(201)
                .body("status", equalTo("WAITING"))
                .body("queuePosition", equalTo(1))
                .extract().<Number>path("id").longValue();
        as(brunoToken).body(json(Map.of("bookId", book))).post("/reservations").then().statusCode(409);
        as(anaToken).delete("/reservations/{id}", brunoReservation).then().statusCode(403);

        as(librarian).post("/loans/{id}/return", loan).then().statusCode(200).body("active", equalTo(false));
        as(brunoToken).get("/reservations/{id}", brunoReservation).then().statusCode(200)
                .body("status", equalTo("READY"));

        // O exemplar está separado: outra pessoa não leva
        as(librarian).body(json(Map.of("userId", ana, "bookCopyId", copy))).post("/loans").then().statusCode(409);
        // Quem reservou leva, e a reserva é concluída
        as(librarian).body(json(Map.of("userId", bruno, "bookCopyId", copy))).post("/loans").then().statusCode(201);
        as(brunoToken).get("/reservations/{id}", brunoReservation).then().body("status", equalTo("FULFILLED"));

        // 9. Relatório dos mais emprestados
        as(librarian).get("/reports/most-borrowed").then().statusCode(200);
    }

    @Test
    void queue_is_fifo_and_expiry_calls_the_next_person() {
        String librarian = librarianToken();
        long book = createBook(librarian, "Fila " + uniqueCode());
        long copy = addCopy(librarian, book);
        long holder = registerMember(uniqueEmail());
        String firstEmail = uniqueEmail();
        String secondEmail = uniqueEmail();
        registerMember(firstEmail);
        registerMember(secondEmail);
        long loan = id(as(librarian).body(json(Map.of("userId", holder, "bookCopyId", copy))).post("/loans"));

        long first = id(as(login(firstEmail, PASSWORD)).body(json(Map.of("bookId", book))).post("/reservations"));
        String secondToken = login(secondEmail, PASSWORD);
        long second = as(secondToken).body(json(Map.of("bookId", book))).post("/reservations").then()
                .statusCode(201).body("queuePosition", equalTo(2)).extract().<Number>path("id").longValue();

        as(librarian).post("/loans/{id}/return", loan).then().statusCode(200);
        as(librarian).get("/reservations/{id}", first).then().body("status", equalTo("READY"));
        as(secondToken).get("/reservations/{id}", second).then().body("status", equalTo("WAITING"))
                .body("queuePosition", equalTo(1));

        // O prazo de retirada do primeiro vence
        jdbc.update("update reservations set ready_until = now() - interval '1 minute' where id = ?", first);
        assertThat(reservations.expireOverdue()).isGreaterThanOrEqualTo(1);

        as(librarian).get("/reservations/{id}", first).then().body("status", equalTo("EXPIRED"));
        as(secondToken).get("/reservations/{id}", second).then().body("status", equalTo("READY"));

        // Cancelar sem ninguém na fila libera o exemplar
        as(secondToken).delete("/reservations/{id}", second).then().statusCode(204);
        as(secondToken).get("/books/{id}", book).then().body("availableCopies", equalTo(1));
    }

    @Test
    void new_copy_serves_the_queue_first() {
        String librarian = librarianToken();
        long book = createBook(librarian, "Sem estoque " + uniqueCode());
        String email = uniqueEmail();
        registerMember(email);
        String token = login(email, PASSWORD);
        long reservation = id(as(token).body(json(Map.of("bookId", book))).post("/reservations"));

        long copy = addCopy(librarian, book);

        as(token).get("/reservations/{id}", reservation).then().body("status", equalTo("READY"))
                .body("bookCopyId", equalTo((int) copy));
    }

    @Test
    void active_loan_limit_is_three() {
        String librarian = librarianToken();
        long member = registerMember(uniqueEmail());
        for (int i = 0; i < 3; i++) {
            long copy = addCopy(librarian, createBook(librarian, "Limite " + uniqueCode()));
            as(librarian).body(json(Map.of("userId", member, "bookCopyId", copy))).post("/loans").then().statusCode(201);
        }
        long fourth = addCopy(librarian, createBook(librarian, "Limite " + uniqueCode()));

        as(librarian).body(json(Map.of("userId", member, "bookCopyId", fourth))).post("/loans").then().statusCode(422);
    }

    @Test
    void refresh_rotation_logout_and_password_change() {
        String email = uniqueEmail();
        registerMember(email);
        var login = api().body(json(Map.of("email", email, "password", PASSWORD))).post("/auth/login").then()
                .statusCode(200).extract().jsonPath();
        String access = login.getString("accessToken");
        String refresh = login.getString("refreshToken");

        as(access).get("/auth/me").then().statusCode(200).body("email", equalTo(email));

        // 8. Rotação: o token novo funciona e o antigo não
        String rotated = api().body(json(Map.of("refreshToken", refresh))).post("/auth/refresh").then()
                .statusCode(200).extract().path("refreshToken");
        assertThat(rotated).isNotEqualTo(refresh);

        // Reutilizar o antigo encerra todas as sessões, inclusive a nova
        api().body(json(Map.of("refreshToken", refresh))).post("/auth/refresh").then().statusCode(401);
        api().body(json(Map.of("refreshToken", rotated))).post("/auth/refresh").then().statusCode(401);

        // Logout revoga a sessão
        String second = api().body(json(Map.of("email", email, "password", PASSWORD))).post("/auth/login").then()
                .extract().path("refreshToken");
        api().body(json(Map.of("refreshToken", second))).post("/auth/logout").then().statusCode(204);
        api().body(json(Map.of("refreshToken", second))).post("/auth/refresh").then().statusCode(401);

        // Troca de senha exige a atual e revoga as sessões
        var third = api().body(json(Map.of("email", email, "password", PASSWORD))).post("/auth/login").then()
                .extract().jsonPath();
        as(third.getString("accessToken"))
                .body(json(Map.of("currentPassword", "senha-errada", "newPassword", "nova-senha-forte")))
                .post("/auth/change-password").then().statusCode(401);
        as(third.getString("accessToken"))
                .body(json(Map.of("currentPassword", PASSWORD, "newPassword", "nova-senha-forte")))
                .post("/auth/change-password").then().statusCode(204);
        api().body(json(Map.of("refreshToken", third.getString("refreshToken")))).post("/auth/refresh").then().statusCode(401);
        api().body(json(Map.of("email", email, "password", PASSWORD))).post("/auth/login").then().statusCode(401);
        login(email, "nova-senha-forte");
    }

    @Test
    void user_administration() {
        String librarian = librarianToken();
        String email = uniqueEmail();

        // Somente bibliotecário cria outro bibliotecário
        long created = id(as(librarian).body(json(Map.of("name", "Novo", "email", email, "password", PASSWORD,
                "roles", List.of("ROLE_LIBRARIAN")))).post("/users"));
        as(login(email, PASSWORD)).get("/users").then().statusCode(200);

        as(librarian).body(json(Map.of("name", "Novo Nome", "email", email, "roles", List.of("ROLE_MEMBER"))))
                .put("/users/{id}", created).then().statusCode(200)
                .body("name", equalTo("Novo Nome"))
                .body("roles", equalTo(List.of("ROLE_MEMBER")));

        // Membro altera o próprio perfil, mas não a role
        String memberToken = login(email, PASSWORD);
        as(memberToken).body(json(Map.of("name", "Outro Nome", "email", email))).put("/users/{id}", created)
                .then().statusCode(200);
        as(memberToken).body(json(Map.of("name", "X", "email", email, "roles", List.of("ROLE_LIBRARIAN"))))
                .put("/users/{id}", created).then().statusCode(403);

        // Desativado não entra mais
        as(librarian).delete("/users/{id}", created).then().statusCode(204);
        api().body(json(Map.of("email", email, "password", PASSWORD))).post("/auth/login").then().statusCode(401);
        as(librarian).get("/users/{id}", created).then().statusCode(200).body("status", equalTo("INACTIVE"));
    }
}
