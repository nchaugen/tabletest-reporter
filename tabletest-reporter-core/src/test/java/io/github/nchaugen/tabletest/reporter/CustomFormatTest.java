package io.github.nchaugen.tabletest.reporter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link CustomFormat}.
 */
class CustomFormatTest {

    @Test
    void creates_custom_format() {
        assertThat(new CustomFormat("html")).satisfies(format -> {
            assertThat(format.formatName()).isEqualTo("html");
            assertThat(format.extension()).isEqualTo(".html");
        });
    }

    @Test
    void rejects_null_name() {
        assertThatThrownBy(() -> new CustomFormat(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_blank_name() {
        assertThatThrownBy(() -> new CustomFormat(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be blank");
    }

    @Test
    void rejects_name_starting_with_dot() {
        assertThatThrownBy(() -> new CustomFormat(".html"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot start with a dot");
    }
}
