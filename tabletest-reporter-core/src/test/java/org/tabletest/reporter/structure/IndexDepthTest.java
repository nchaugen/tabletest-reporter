package org.tabletest.reporter.structure;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.IndexDepth;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link IndexDepth}.
 */
@DisplayName("The indexDepth option")
@Description("""
        The indexDepth option says how many levels of nested features get an index page of their
        own. The reporter writes the rest of the tree onto one page. Set the option on the CLI,
        Maven or Gradle surface, as a number or as the keyword "infinite".
        """)
class IndexDepthTest {

    @DisplayName("Reads the option as a depth, or as no limit")
    @Description("""
            The number counts the levels that get an index page of their own. The reporter writes
            the rest of the tree onto one page, so a depth of one indexes the top level alone.

            The keyword ignores case, and an option you never set means the same as the keyword.
            Absent, empty and blank all count as never set. No limit is the largest depth the
            option can hold, which is the number the rows below read.
            """)
    @TableTest("""
        Scenario                 | Option value                   | Index depth? | Unlimited?
        The shallowest depth     | 1                              | 1            | false
        Several levels of index  | 5                              | 5            | false
        The keyword, in any case | {infinite, INFINITE, Infinite} | 2147483647   | true
        The option never set     |                                | 2147483647   | true
        An empty or blank value  | {'', '   '}                    | 2147483647   | true
        """)
    void reads_the_option_as_a_depth_or_as_no_limit(String optionValue, int indexDepth, boolean unlimited) {
        IndexDepth depth = IndexDepth.parse(optionValue);

        assertThat(depth.value()).isEqualTo(indexDepth);
        assertThat(depth.isInfinite()).isEqualTo(unlimited);
    }

    @Test
    void of_creates_depth_with_specified_value() {
        IndexDepth depth = IndexDepth.of(3);
        assertThat(depth.value()).isEqualTo(3);
    }

    @Test
    void default_is_infinite() {
        assertThat(IndexDepth.DEFAULT).isSameAs(IndexDepth.INFINITE);
    }

    @DisplayName("Refuses an index depth below one level")
    @Description("""
            A depth of one indexes the top level alone. Nothing is shallower. The reporter therefore
            refuses a depth below one. It never raises the depth to one instead. The message names
            the depth you gave.
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

    @DisplayName("Refuses a value that is neither a number nor the keyword")
    @Description("""
            The message repeats the value the reporter could not read, and names what it accepts. A
            value that does read as a number, but lies out of range, belongs to the rule above about
            the shallowest depth.
            """)
    @TableTest("""
        Scenario                | Option value | Error message?
        A word that is not one  | foo          | "Invalid index depth: 'foo'. Expected a positive integer or 'infinite'."
        A number with a decimal | 1.5          | "Invalid index depth: '1.5'. Expected a positive integer or 'infinite'."
        """)
    void rejects_a_value_that_is_neither_a_number_nor_the_keyword(String optionValue, String errorMessage) {
        assertThat(errorMessageFrom(() -> IndexDepth.parse(optionValue))).isEqualTo(errorMessage);
    }
}
