package br.com.fiap.library.integration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.library.support.IntegrationTestBase;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class ConcurrencyIT extends IntegrationTestBase {
    private static final int THREADS = 8;

    private static List<Integer> runConcurrently(List<Callable<Response>> calls) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(calls.size())) {
            List<Future<Response>> futures = new ArrayList<>();
            for (Callable<Response> call : calls) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return call.call();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Response> future : futures) {
                statuses.add(future.get().statusCode());
            }
            return statuses;
        }
    }

    @Test
    void same_copy_requested_at_once_produces_a_single_loan() throws Exception {
        String librarian = librarianToken();
        long copy = addCopy(librarian, createBook(librarian, "Concorrência"));
        List<Callable<Response>> calls = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            long member = registerMember(uniqueEmail());
            calls.add(() -> as(librarian).body(json(Map.of("userId", member, "bookCopyId", copy))).post("/loans"));
        }

        List<Integer> statuses = runConcurrently(calls);

        assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
        assertThat(statuses).filteredOn(status -> status == 409).hasSize(THREADS - 1);
    }

    @Test
    void active_loan_limit_holds_under_concurrency() throws Exception {
        String librarian = librarianToken();
        long member = registerMember(uniqueEmail());
        List<Callable<Response>> calls = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            long copy = addCopy(librarian, createBook(librarian, "Limite " + i));
            calls.add(() -> as(librarian).body(json(Map.of("userId", member, "bookCopyId", copy))).post("/loans"));
        }

        List<Integer> statuses = runConcurrently(calls);

        assertThat(statuses).filteredOn(status -> status == 201).hasSize(3);
        assertThat(statuses).filteredOn(status -> status == 422).hasSize(THREADS - 3);
    }

    @Test
    void refresh_token_used_twice_at_once_is_accepted_only_once() throws Exception {
        String email = uniqueEmail();
        registerMember(email);
        String refresh = api().body(json(Map.of("email", email, "password", "senha-do-membro")))
                .post("/auth/login").then().statusCode(200).extract().path("refreshToken");
        List<Callable<Response>> calls = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            calls.add(() -> api().body(json(Map.of("refreshToken", refresh))).post("/auth/refresh"));
        }

        List<Integer> statuses = runConcurrently(calls);

        assertThat(statuses).filteredOn(status -> status == 200).hasSize(1);
    }
}
