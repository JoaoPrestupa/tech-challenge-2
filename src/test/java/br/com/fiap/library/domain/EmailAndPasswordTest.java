package br.com.fiap.library.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.library.domain.exception.InvalidDataException;
import br.com.fiap.library.domain.model.EmailAddress;
import br.com.fiap.library.domain.model.PasswordPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmailAndPasswordTest {

    @Test
    void normalizes_email_to_trimmed_lowercase() {
        assertThat(EmailAddress.normalize("  Maria.Silva@Example.COM ")).isEqualTo("maria.silva@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "sem-arroba", "a@b", "a b@c.com", "@c.com"})
    void rejects_invalid_emails(String email) {
        assertThatThrownBy(() -> EmailAddress.normalize(email)).isInstanceOf(InvalidDataException.class);
    }

    @Test
    void password_limits_are_8_chars_and_72_bytes() {
        assertThat(PasswordPolicy.accepts("1234567")).isFalse();
        assertThat(PasswordPolicy.accepts("12345678")).isTrue();
        assertThat(PasswordPolicy.accepts("a".repeat(72))).isTrue();
        assertThat(PasswordPolicy.accepts("a".repeat(73))).isFalse();
    }

    @Test
    void password_limit_counts_utf8_bytes_and_not_characters() {
        assertThat(PasswordPolicy.accepts("ç".repeat(36))).isTrue();
        assertThat(PasswordPolicy.accepts("ç".repeat(37))).isFalse();
    }
}
