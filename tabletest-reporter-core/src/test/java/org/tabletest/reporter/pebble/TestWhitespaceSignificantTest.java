package org.tabletest.reporter.pebble;

import org.junit.jupiter.api.Test;
import org.tabletest.junit.TableTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TestWhitespaceSignificantTest {

    private final TestWhitespaceSignificant test = new TestWhitespaceSignificant();

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

    @Test
    void nonStringValuesAreNeverSignificant() {
        assertThat(test.apply(42, Map.of(), null, null, 0)).isFalse();
    }
}
