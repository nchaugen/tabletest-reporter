package org.tabletest.reporter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.pebble.TestWhitespaceSignificant;

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

    @DisplayName("Treats whitespace as significant at line edges, in runs, and in tabs")
    @Description("""
            Significant means: any leading or trailing whitespace (on any line of a
            multiline value), any run of two or more spaces, any tab, and values
            aligned with pipes as in a formatted table row. A single space between
            words is not significant.
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
        Multiline plain         | ab\\ncd      | false
        Space before newline    | 'ab \\ncd'   | true
        Space after newline     | 'ab\\n cd'   | true
        Null value              |              | false
        """)
    void classifiesWhitespaceSignificance(String value, boolean significant) {
        assertThat(test.apply(unescapedNewlines(value), Map.of(), null, null, 0))
                .isEqualTo(significant);
    }

    private static String unescapedNewlines(String value) {
        return value == null ? null : value.replace("\\n", "\n");
    }

    @Test
    void nonStringValuesAreNeverSignificant() {
        assertThat(test.apply(42, Map.of(), null, null, 0)).isFalse();
    }
}
