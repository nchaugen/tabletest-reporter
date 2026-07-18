package org.tabletest.reporter.pebble;

import org.junit.jupiter.api.Test;
import org.tabletest.junit.TableTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class FilterMarkWhitespaceTest {

    private final FilterMarkWhitespace filter = new FilterMarkWhitespace();

    @TableTest("""
        Scenario                      | Value        | Marked?
        Plain word                    | Alice        | Alice
        Single internal space         | Alice Smith  | Alice Smith
        Leading space                 | ' x'         | '<span class="sp"> </span>x'
        Trailing space                | 'x '         | 'x<span class="sp"> </span>'
        Run of two spaces             | 'a  b'       | 'a<span class="sp">  </span>b'
        Whitespace-only value         | '   '        | '<span class="sp">   </span>'
        Tab between words             | 'a\tb'       | 'a<span class="tab">\t</span>b'
        Run of two tabs               | '\t\t'       | '<span class="tab">\t</span><span class="tab">\t</span>'
        Tab then space                | '\t '        | '<span class="tab">\t</span><span class="sp"> </span>'
        Spaces around interior tab    | 'a \t b'     | 'a<span class="sp"> </span><span class="tab">\t</span><span class="sp"> </span>b'
        Trailing space before newline | 'ab \\ncd'   | 'ab<span class="sp"> </span>\\ncd'
        Leading space on second line  | 'ab\\n cd'   | 'ab\\n<span class="sp"> </span>cd'
        Markup characters escaped     | "a<b> & 'c'" | "a&lt;b&gt; &amp; &#39;c&#39;"
        """)
    void marksSignificantWhitespaceRuns(String value, String marked) {
        assertThat(filter.apply(unescapedNewlines(value), Map.of(), null, null, 0))
                .isEqualTo(unescapedNewlines(marked));
    }

    @Test
    void nullValuePassesThroughAsNull() {
        assertThat(filter.apply(null, Map.of(), null, null, 0)).isNull();
    }

    private static String unescapedNewlines(String cell) {
        return cell.replace("\\n", "\n");
    }
}
