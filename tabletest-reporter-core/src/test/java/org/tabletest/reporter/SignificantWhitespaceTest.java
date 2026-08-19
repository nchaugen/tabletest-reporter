package org.tabletest.reporter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.pebble.TestWhitespaceSignificant;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Significant whitespace")
@Description("""
        Whitespace markers appear only where whitespace could mislead: prose with
        single spaces between words stays clean, while the whitespace a reader could
        miscount or miss entirely is flagged for marking.
        """)
class SignificantWhitespaceTest {

    private final TestWhitespaceSignificant test = new TestWhitespaceSignificant();

    @DisplayName("Treats whitespace as significant at the value's edges, in runs, and in tabs")
    @Description("""
            Significant means: leading or trailing whitespace, any run of two or more
            spaces, any tab, and values aligned with pipes as in a formatted table row.
            A single space between words is not significant.
            """)
    @TableTest("""
        Scenario                | Value        | Significant?
        Plain word              | Alice        | false
        Single internal space   | Alice Smith  | false
        Leading space           | ' x'         | true
        Trailing space          | 'x '         | true
        Whitespace-only value   | '   '        | true
        Empty string            | ''           | false
        Tab between words       | 'a\tb'       | true
        Run of two spaces       | 'a  b'       | true
        Pipe between words      | "a|b"        | true
        Formatted row with pipe | "Alice | 30" | true
        Null value              |              | false
        """)
    void classifiesWhitespaceSignificance(String value, boolean significant) {
        assertThat(test.apply(value, Map.of(), null, null, 0)).isEqualTo(significant);
    }

    @DisplayName("Reads every line of a multi-line value for whitespace at its edges")
    @Description("""
            A value that spans lines is judged line by line, so a space left at the end of one
            line or in front of the next counts, though neither sits at an edge of the value
            itself.
            """)
    @TableTest("""
        Scenario                 | Lines of the value | Significant?
        Neither line has an edge | [ab, cd]           | false
        A space ending a line    | ['ab ', cd]        | true
        A space starting a line  | [ab, ' cd']        | true
        """)
    void classifiesWhitespaceAcrossLines(List<String> lines, boolean significant) {
        assertThat(test.apply(String.join("\n", lines), Map.of(), null, null, 0))
                .isEqualTo(significant);
    }

    @Test
    void nonStringValuesAreNeverSignificant() {
        assertThat(test.apply(42, Map.of(), null, null, 0)).isFalse();
    }
}
