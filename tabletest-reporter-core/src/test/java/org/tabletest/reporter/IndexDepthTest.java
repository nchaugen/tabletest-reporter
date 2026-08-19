package org.tabletest.reporter;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link IndexDepth}.
 */
@DisplayName("The indexDepth option")
@Description("""
        The indexDepth option controls how many levels of nested features get their own
        index pages before the remaining tree is flattened onto one page. It is set on
        the CLI, Maven, or Gradle surface as a number or the keyword "infinite".
        """)
class IndexDepthTest {

    @DisplayName("indexDepth accepts a depth number or the infinite keyword")
    @Description("""
            The keyword is case-insensitive, and leaving the option unset means
            infinite: every feature level gets its own index page. Infinite is
            represented as the largest possible depth, 2147483647.
            """)
    @TableTest("""
        Scenario         | Input    | Expected Depth?
        Numeric value    | 1        | 1
        Larger value     | 5        | 5
        Infinite keyword | infinite | 2147483647
        Uppercase        | INFINITE | 2147483647
        Mixed case       | Infinite | 2147483647
        Null input       |          | 2147483647
        Empty string     | ''       | 2147483647
        Blank string     | '   '    | 2147483647
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

    @DisplayName("One is the shallowest depth an index can be built to")
    @Description("""
            A depth of one indexes the top level only. There is nothing shallower, so anything
            below one is rejected rather than clamped, and the message names the depth it was
            given.
            """)
    @TableTest("""
        Scenario                 | Depth | Error message?
        The shallowest depth     | 1     |
        One below the shallowest | 0     | Index depth must be at least 1, was: 0
        A negative depth         | -1    | Index depth must be at least 1, was: -1
        """)
    void rejects_a_depth_below_one(int depth, String errorMessage) {
        assertThat(errorMessageFrom(() -> IndexDepth.of(depth))).isEqualTo(errorMessage);
    }

    /** The message the action fails with, or null when it does not fail. */
    private static String errorMessageFrom(ThrowableAssert.ThrowingCallable action) {
        try {
            action.call();
            return null;
        } catch (Throwable thrown) {
            return thrown.getMessage();
        }
    }

    @DisplayName("Option values that are not a number or the keyword are rejected")
    @TableTest("""
        Scenario        | Input | Error Contains?
        Invalid word    | foo   | Invalid index depth
        Numeric text    | one   | Invalid index depth
        Negative string | -1    | at least 1
        """)
    void parse_rejects_invalid_strings(String input, String errorContains) {
        assertThatThrownBy(() -> IndexDepth.parse(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(errorContains);
    }
}
