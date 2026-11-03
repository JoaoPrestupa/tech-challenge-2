package br.com.fiap.library.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.library.support.IntegrationTestBase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Confere as migrations e as regras garantidas pelo banco, sem passar pela aplicação. */
class PersistenceConstraintsIT extends IntegrationTestBase {
    @Autowired
    private JdbcTemplate jdbc;

    private long book() {
        long id = jdbc.queryForObject("select nextval('books_seq')", Long.class);
        jdbc.update("insert into books (id, title, author, isbn) values (?, 'T', 'A', ?)", id, uniqueIsbn());
        return id;
    }

    private long copy(long bookId) {
        long id = jdbc.queryForObject("select nextval('book_copies_seq')", Long.class);
        jdbc.update("insert into book_copies (id, book_id, asset_code) values (?, ?, ?)", id, bookId, uniqueCode());
        return id;
    }

    private long user() {
        return jdbc.queryForObject("""
                insert into library_users (name, normalized_email, password_hash)
                values ('U', ?, 'hash') returning id""", Long.class, uniqueEmail());
    }

    private void openLoan(long userId, long copyId) {
        jdbc.update("insert into loans (user_id, book_copy_id, borrowed_at, due_at) values (?, ?, now(), now() + interval '1 day')",
                userId, copyId);
    }

    @Test
    void database_is_utf8_and_stores_accents_intact() {
        assertThat(jdbc.queryForObject("show server_encoding", String.class)).isEqualTo("UTF8");
        long id = jdbc.queryForObject("select nextval('books_seq')", Long.class);
        jdbc.update("insert into books (id, title, author, isbn) values (?, 'Coração, ação e ñ', 'José', ?)", id, uniqueIsbn());
        assertThat(jdbc.queryForObject("select title from books where id = ?", String.class, id))
                .isEqualTo("Coração, ação e ñ");
    }

    @Test
    void timestamps_use_time_zone_columns() {
        List<String> types = jdbc.queryForList("""
                select distinct data_type from information_schema.columns
                where table_name = 'loans' and column_name in ('borrowed_at', 'due_at', 'returned_at')""", String.class);
        assertThat(types).containsExactly("timestamp with time zone");
    }

    @Test
    void isbn_must_be_unique() {
        String isbn = uniqueIsbn();
        jdbc.update("insert into books (id, title, author, isbn) values (nextval('books_seq'), 'T', 'A', ?)", isbn);

        assertThatThrownBy(() ->
                jdbc.update("insert into books (id, title, author, isbn) values (nextval('books_seq'), 'T', 'A', ?)", isbn))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void email_must_be_unique() {
        String email = uniqueEmail();
        String sql = "insert into library_users (name, normalized_email, password_hash) values ('U', ?, 'h')";
        jdbc.update(sql, email);

        assertThatThrownBy(() -> jdbc.update(sql, email)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void copy_cannot_have_two_active_loans() {
        long copy = copy(book());
        openLoan(user(), copy);

        assertThatThrownBy(() -> openLoan(user(), copy)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void copy_can_be_loaned_again_after_the_return() {
        long copy = copy(book());
        openLoan(user(), copy);
        jdbc.update("update loans set returned_at = now() where book_copy_id = ?", copy);

        openLoan(user(), copy);

        assertThat(jdbc.queryForObject("select count(*) from loans where book_copy_id = ?", Long.class, copy)).isEqualTo(2L);
    }

    @Test
    void user_cannot_have_two_open_reservations_for_the_same_book() {
        long book = book();
        long user = user();
        String sql = "insert into reservations (user_id, book_id, created_at) values (?, ?, now())";
        jdbc.update(sql, user, book);

        assertThatThrownBy(() -> jdbc.update(sql, user, book)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void invalid_status_is_rejected() {
        long book = book();

        assertThatThrownBy(() -> jdbc.update(
                "insert into book_copies (id, book_id, asset_code, status) values (nextval('book_copies_seq'), ?, ?, 'X')",
                book, uniqueCode())).isInstanceOf(DataIntegrityViolationException.class);
    }
}
