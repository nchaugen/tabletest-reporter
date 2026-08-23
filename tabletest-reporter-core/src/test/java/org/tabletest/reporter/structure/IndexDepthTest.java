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

    @DisplayName("Reads indexDepth as a count of index levels")
    @Description("""
            The number counts the levels that get an index page. The reporter writes the rest of the
            tree onto one page. A depth of one therefore indexes the top level alone.
            """)
    @TableTest("""
        Scenario                | Option value | Index depth?
        The shallowest depth    | 1            | 1
        Several levels of index | 5            | 5
        """)
    void reads_a_number_as_the_depth(String optionValue, int indexDepth) {
        assertThat(IndexDepth.parse(optionValue).value()).isEqualTo(indexDepth);
    }

    @DisplayName("Treats the keyword, and an unset option, as unlimited depth")
    @Description("""
            The keyword ignores case. An option you never set means the same as the keyword: every
            level of features gets an index page of its own. Three values count as never set: absent,
            empty and blank. A depth alone limits the index.
            """)
    @TableTest("""
        Scenario                 | Option value                   | Unlimited?
        The keyword, in any case | {infinite, INFINITE, Infinite} | true
        The option never set     |                                | true
        An empty value           | ''                             | true
        A blank value            | '   '                          | true
        A depth of its own       | 5                              | false
        """)
    void reads_the_keyword_and_an_unset_option_as_unlimited(String optionValue, boolean unlimited) {
        assertThat(IndexDepth.parse(optionValue).isInfinite()).isEqualTo(unlimited);
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

    @DisplayName("Refuses an index depth below one level")
    @Description("""
            A depth of one indexes the top level alone. Nothing is shallower. The reporter therefore
            refuses a depth below one, and never raises it to one quietly. The message names the depth
            you gave.
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
        Scenario               | Input | Error message?
        A word that is not one | foo   | "Invalid index depth: 'foo'. Expected a positive integer or 'infinite'."
        A number written out   | one   | "Invalid index depth: 'one'. Expected a positive integer or 'infinite'."
        """)
    void parse_rejects_invalid_strings(String input, String errorMessage) {
        assertThat(errorMessageFrom(() -> IndexDepth.parse(input))).isEqualTo(errorMessage);
    }
}
