package org.tabletest.reporter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.pebble.FilterMarkWhitespace;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Whitespace markers")
@Description("""
        Significant whitespace in a cell is made visible IDE-style: a centred dot per
        space and an arrow per tab, drawn by the report stylesheet over the real
        characters. Copy-paste still yields the original spaces and tabs. Single
        spaces between words are left unmarked.
        """)
public class WhitespaceMarkersTest {

    private final FilterMarkWhitespace filter = new FilterMarkWhitespace();

    @DisplayName("Wraps significant whitespace runs in marker spans")
    @Description("""
            HTML output: sp spans carry space runs and tab spans carry each tab; the
            stylesheet draws the dots and arrows. Markup characters in the value are
            escaped before marking, so cell content can never inject HTML.
            """)
    @TableTest("""
        Scenario                   | Value        | Marked?
        Plain word                 | Alice        | Alice
        Single internal space      | Alice Smith  | Alice Smith
        Leading space              | ' x'         | '<span class="sp"> </span>x'
        Trailing space             | 'x '         | 'x<span class="sp"> </span>'
        Run of two spaces          | 'a  b'       | 'a<span class="sp">  </span>b'
        Whitespace-only value      | '   '        | '<span class="sp">   </span>'
        Tab between words          | 'a\tb'       | 'a<span class="tab">\t</span>b'
        Run of two tabs            | '\t\t'       | '<span class="tab">\t</span><span class="tab">\t</span>'
        Tab then space             | '\t '        | '<span class="tab">\t</span><span class="sp"> </span>'
        Spaces around interior tab | 'a \t b'     | 'a<span class="sp"> </span><span class="tab">\t</span><span class="sp"> </span>b'
        Markup characters escaped  | "a<b> & 'c'" | "a&lt;b&gt; &amp; &#39;c&#39;"
        """)
    void marksSignificantWhitespaceRuns(String value, String marked) {
        assertThat(filter.apply(value, Map.of(), null, null, 0)).isEqualTo(marked);
    }

    @DisplayName("Marks each line of a multi-line value and leaves the line breaks alone")
    @Description("""
            Each line is marked on its own, so a space at the end of one line and a space in
            front of the next each get their own span. The breaks between lines are left as
            they were — only the whitespace inside a line is wrapped.
            """)
    @TableTest("""
        Scenario                | Lines of the value | Marked lines?
        A space ending a line   | ['ab ', cd]        | ['ab<span class="sp"> </span>', cd]
        A space starting a line | [ab, ' cd']        | [ab, '<span class="sp"> </span>cd']
        Both, on the same value | ['ab ', ' cd']     | ['ab<span class="sp"> </span>', '<span class="sp"> </span>cd']
        """)
    void marksEachLineOfAMultiLineValue(List<String> lines, List<String> markedLines) {
        assertThat(filter.apply(String.join("\n", lines), Map.of(), null, null, 0))
                .isEqualTo(String.join("\n", markedLines));
    }

    @Test
    void nullValuePassesThroughAsNull() {
        assertThat(filter.apply(null, Map.of(), null, null, 0)).isNull();
    }
}
