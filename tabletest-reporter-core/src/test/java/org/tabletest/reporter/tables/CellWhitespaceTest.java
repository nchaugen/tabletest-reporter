package org.tabletest.reporter.tables;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.pebble.FilterMarkWhitespace;
import org.tabletest.reporter.pebble.TestWhitespaceSignificant;
import org.tabletest.reporter.support.PublishedCell;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Cell whitespace")
@Description("""
        A space at the end of a value, a run of two, a tab: whitespace a reader could miscount or
        miss entirely, in a table whose whole point is the exact value a row ran with. Which
        whitespace needs care is the same question for all three formats, and they all answer it
        the same way. What they can do about it differs, and that is what the rule below shows
        side by side.
        """)
public class CellWhitespaceTest {

    @DisplayName("Renders significant whitespace so a reader can count it")
    @Description("""
            Significant means whitespace at either edge of the value, a run of two or more spaces,
            or a tab; a single space between words is left alone, and so is a value with no
            whitespace at all. HTML keeps the real characters and marks them — a sp span per run
            of spaces, a tab span per tab, with the stylesheet drawing the dot and the arrow over
            them — so a value copied off the page is still the value the row ran with. Markdown
            and AsciiDoc are plain text with no styling to reach for, so they substitute a glyph
            into the value itself: an open box for a space, an arrow for a tab. AsciiDoc wraps
            every literal in ++ pass-through markers whatever its whitespace, which is why they
            appear throughout its column.
            """)
    @TableTest("""
        Scenario              | Cell        | In HTML?                                                                                                        | In markdown?                 | In AsciiDoc?
        Plain word            | Alice       | '<span class="literal">Alice</span>'                                                                            | Alice                        | '++Alice++'
        Single internal space | Alice Smith | '<span class="literal">Alice Smith</span>'                                                                      | Alice Smith                  | '++Alice Smith++'
        Leading space         | "' x'"      | '<span class="literal ws"><span class="sp"> </span>x</span>'                                                    | '&#x2423;x'                  | '&#x2423;++x++'
        Trailing space        | "'x '"      | '<span class="literal ws">x<span class="sp"> </span></span>'                                                    | 'x&#x2423;'                  | '++x++&#x2423;'
        Run of two spaces     | "'a  b'"    | '<span class="literal ws">a<span class="sp">  </span>b</span>'                                                  | 'a&#x2423;&#x2423;b'         | '++a++&#x2423;&#x2423;++b++'
        Tab between words     | "'a\tb'"    | '<span class="literal ws">a<span class="tab">\t</span>b</span>'                                                 | 'a&#x21E5;b'                 | '++a++&#x21E5;++b++'
        Whitespace-only value | "'   '"     | '<span class="literal ws"><span class="sp">   </span></span>'                                                   | '&#x2423;&#x2423;&#x2423;'   | '&#x2423;&#x2423;&#x2423;'
        Run of two tabs       | "'		'"        | '<span class="literal ws"><span class="tab">	</span><span class="tab">	</span></span>'                            | '&#x21E5;&#x21E5;'           | '&#x21E5;&#x21E5;'
        Tab then space        | "'	 '"       | '<span class="literal ws"><span class="tab">	</span><span class="sp"> </span></span>'                            | '&#x21E5;&#x2423;'           | '&#x21E5;&#x2423;'
        Spaces around a tab   | "'a 	 b'"    | '<span class="literal ws">a<span class="sp"> </span><span class="tab">	</span><span class="sp"> </span>b</span>' | 'a&#x2423;&#x21E5;&#x2423;b' | '++a++&#x2423;&#x21E5;&#x2423;++b++'
        An empty value        | "''"        | '<span class="literal empty-string">“”</span>'                                                                  | '""'                         | '+""+'
        """)
    void rendersSignificantWhitespace(String cell, String inHtml, String inMarkdown, String inAsciiDoc) {
        assertThat(PublishedCell.of("html", cell)).isEqualTo(inHtml);
        assertThat(PublishedCell.of("markdown", cell)).isEqualTo(inMarkdown);
        assertThat(PublishedCell.of("asciidoc", cell)).isEqualTo(inAsciiDoc);
    }

    /**
     * Unpublished: the marker filter escapes before it marks, so cell content can never inject
     * HTML. A property of the filter rather than a rule about whitespace, and the escaping is the
     * same for every value whether or not it needs a marker.
     */
    @Test
    void escapesMarkupBeforeMarkingIt() {
        assertThat(new FilterMarkWhitespace().apply("a<b> & 'c'", Map.of(), null, null, 0))
                .isEqualTo("a&lt;b&gt; &amp; &#39;c&#39;");
    }

    /**
     * Unpublished: no published cell can hold a newline — the grammar is strictly one row per
     * line (`decisions/no-multiline-cells-in-grammar.md`), and the one multi-line thing a report
     * does publish, a broken row's message, is escaped rather than marked. These pin the filter's
     * defensive behaviour, which no report can reach.
     */
    @Test
    void marksEachLineOfAMultiLineValueSeparately() {
        assertThat(new FilterMarkWhitespace().apply("ab \n cd", Map.of(), null, null, 0))
                .isEqualTo("ab<span class=\"sp\"> </span>\n<span class=\"sp\"> </span>cd");
    }

    @Test
    void readsEveryLineOfAMultiLineValueForEdgeWhitespace() {
        TestWhitespaceSignificant significance = new TestWhitespaceSignificant();

        assertThat(significance.apply("ab\ncd", Map.of(), null, null, 0)).isFalse();
        assertThat(significance.apply("ab \ncd", Map.of(), null, null, 0)).isTrue();
        assertThat(significance.apply("ab\n cd", Map.of(), null, null, 0)).isTrue();
    }

    @Test
    void nonStringValuesAreNeverSignificant() {
        assertThat(new TestWhitespaceSignificant().apply(42, Map.of(), null, null, 0))
                .isFalse();
    }

    @Test
    void nullValuePassesThroughTheMarkerAsNull() {
        assertThat(new FilterMarkWhitespace().apply(null, Map.of(), null, null, 0))
                .isNull();
    }
}
