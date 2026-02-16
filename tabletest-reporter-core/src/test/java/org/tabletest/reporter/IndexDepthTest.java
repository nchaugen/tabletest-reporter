package org.tabletest.reporter;

import org.junit.jupiter.api.Test;
import org.tabletest.junit.TableTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link IndexDepth}.
 */
class IndexDepthTest {

    @TableTest("""
        Scenario         | Input      | Expected Depth?
        Numeric value    | 1          | 1
        Larger value     | 5          | 5
        Infinite keyword | infinite   | 2147483647
        Uppercase        | INFINITE   | 2147483647
        Mixed case       | Infinite   | 2147483647
        Null input       |            | 2147483647
        Empty string     | ''         | 2147483647
        Blank string     | '   '      | 2147483647
        """)
    void parses_valid_values(String input, int expectedDepth) {
        IndexDepth result = IndexDepth.parse(input);
        assertThat(result.value()).isEqualTo(expectedDepth);
    }

    @Test
    void of_creates_depth_with_specified_value() {
        IndexDepth depth = IndexDepth.of(3);
        assertThat(depth.value()).isEqualTo(3);
    }

    @Test
    void infinite_constant_has_max_value() {
        assertThat(IndexDepth.INFINITE.value()).isEqualTo(Integer.MAX_VALUE);
        assertThat(IndexDepth.INFINITE.isInfinite()).isTrue();
    }

    @Test
    void default_is_infinite() {
        assertThat(IndexDepth.DEFAULT).isSameAs(IndexDepth.INFINITE);
    }

    @Test
    void isInfinite_returns_false_for_finite_depth() {
        assertThat(IndexDepth.of(5).isInfinite()).isFalse();
    }

    @TableTest("""
        Scenario           | Input  | Error Contains?
        Zero depth         | 0      | at least 1
        Negative depth     | -1     | at least 1
        Large negative     | -100   | at least 1
        """)
    void rejects_invalid_depth_values(int input, String errorContains) {
        assertThatThrownBy(() -> IndexDepth.of(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(errorContains);
    }

    @TableTest("""
        Scenario           | Input     | Error Contains?
        Invalid word       | foo       | Invalid index depth
        Numeric text       | one       | Invalid index depth
        Negative string    | -1        | at least 1
        """)
    void parse_rejects_invalid_strings(String input, String errorContains) {
        assertThatThrownBy(() -> IndexDepth.parse(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(errorContains);
    }
}
