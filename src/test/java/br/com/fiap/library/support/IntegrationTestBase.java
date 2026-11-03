package br.com.fiap.library.support;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

/** Sobe a aplicação inteira contra um PostgreSQL real, compartilhado por todos os testes. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestBase {
    protected static final String LIBRARIAN_EMAIL = "bibliotecaria@example.com";
    protected static final String LIBRARIAN_PASSWORD = "senha-da-bibliotecaria";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis() % 100_000_000L);

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
        registry.add("logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener", () -> "OFF");
        registry.add("library.jwt.secret", () -> "segredo-usado-somente-nos-testes-automatizados");
        registry.add("library.security.bcrypt-strength", () -> "4");
        registry.add("library.bootstrap.name", () -> "Bibliotecária");
        registry.add("library.bootstrap.email", () -> LIBRARIAN_EMAIL);
        registry.add("library.bootstrap.password", () -> LIBRARIAN_PASSWORD);
    }

    @LocalServerPort
    private int port;

    @BeforeEach
    void configureRestAssured() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }

    protected static RequestSpecification api() {
        return RestAssured.given().contentType(ContentType.JSON);
    }

    protected static RequestSpecification as(String token) {
        return api().header("Authorization", "Bearer " + token);
    }

    protected static String json(Map<String, ?> body) {
        return JSON.writeValueAsString(body);
    }

    protected static String librarianToken() {
        return login(LIBRARIAN_EMAIL, LIBRARIAN_PASSWORD);
    }

    protected static String login(String email, String password) {
        return api().body(json(Map.of("email", email, "password", password)))
                .post("/auth/login").then().statusCode(200).extract().path("accessToken");
    }

    protected static long registerMember(String email) {
        return api().body(json(Map.of("name", "Membro Teste", "email", email, "password", "senha-do-membro")))
                .post("/auth/register").then().statusCode(201).extract().<Number>path("id").longValue();
    }

    protected static String uniqueEmail() {
        return "membro" + SEQUENCE.incrementAndGet() + "@example.com";
    }

    protected static String uniqueCode() {
        return "EX-" + SEQUENCE.incrementAndGet();
    }

    /** Gera um ISBN-13 válido e único, calculando o dígito verificador. */
    protected static String uniqueIsbn() {
        String base = "978" + String.format("%09d", SEQUENCE.incrementAndGet() % 1_000_000_000L);
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (base.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return base + (10 - sum % 10) % 10;
    }

    protected static long createBook(String librarian, String title) {
        return as(librarian).body(json(Map.of("title", title, "author", "Autor Teste", "isbn", uniqueIsbn())))
                .post("/books").then().statusCode(201).extract().<Number>path("id").longValue();
    }

    protected static long addCopy(String librarian, long bookId) {
        return as(librarian).body(json(Map.of("assetCodes", java.util.List.of(uniqueCode()))))
                .post("/books/{id}/copies", bookId).then().statusCode(201).extract().<Number>path("[0].id").longValue();
    }
}
