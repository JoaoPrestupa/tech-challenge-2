package br.com.fiap.library.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.library.domain.exception.InvalidDataException;
import br.com.fiap.library.domain.model.Isbn;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class IsbnTest {

    @ParameterizedTest
    @CsvSource({
            "978-85-359-0277-8, 9788535902778",
            "9780306406157, 9780306406157",
            "0-306-40615-2, 0306406152",
            "0 8044 2957 x, 080442957X"
    })
    void normalizes_valid_isbns(String raw, String expected) {
        assertThat(Isbn.normalize(raw)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "123", "9780306406158", "0306406153", "97803064061AB", "X306406152"})
    void rejects_invalid_isbns(String raw) {
        assertThatThrownBy(() -> Isbn.normalize(raw)).isInstanceOf(InvalidDataException.class);
    }
}
