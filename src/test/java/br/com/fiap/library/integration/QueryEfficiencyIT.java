package br.com.fiap.library.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import br.com.fiap.library.support.IntegrationTestBase;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Mede os comandos enviados ao banco, para que a eficiência não seja apenas uma afirmação. */
class QueryEfficiencyIT extends IntegrationTestBase {
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    @Test
    void batch_of_200_books_with_copies_uses_jdbc_batching() {
        String librarian = librarianToken();
        List<Map<String, Object>> books = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            books.add(Map.of("title", "Eficiência " + i, "author", "A", "isbn", uniqueIsbn(),
                    "assetCodes", List.of(uniqueCode())));
        }
        String body = json(Map.of("books", books));
        statistics.clear();

        as(librarian).body(body).post("/books/batch").then().statusCode(201);

        // 400 linhas inseridas. Sem lote, seriam ao menos 400 comandos.
        assertThat(statistics.getEntityInsertCount()).isEqualTo(400);
        assertThat(statistics.getPrepareStatementCount()).isLessThan(50);
    }

    @Test
    void book_search_does_not_grow_with_the_page_size() {
        String librarian = librarianToken();
        String marker = "NMaisUm" + uniqueCode();
        for (int i = 0; i < 30; i++) {
            addCopy(librarian, createBook(librarian, marker + " " + i));
        }
        statistics.clear();

        as(librarian).queryParam("title", marker).queryParam("size", 30).get("/books").then().statusCode(200)
                .body("totalElements", equalTo(30));

        // Página, contagem e disponibilidade agregada: sem N+1.
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }
}
