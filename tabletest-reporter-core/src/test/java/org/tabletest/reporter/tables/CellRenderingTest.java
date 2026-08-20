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

@DisplayName("Cell rendering")
@Description("""
        The reporter publishes the value a row actually ran with, not the text of the cell, so
        every value has to be written back out — and each format writes it back the way that
        format can. Where they differ the rules below show all three side by side, markdown and
        AsciiDoc first because they are plain text and answer most questions alike, HTML last
        because its answer is markup and always the longest.

        The one thing that does not fit that shape is a collection. Only markdown can put a whole
        collection inside a table cell: AsciiDoc opens a bulleted or description-list block below
        the cell and HTML nests list markup inside it, neither of which is a cell a table can
        hold. The notation rule is therefore markdown's alone until the Lines value type lands.
        """)
public class CellRenderingTest {

    @DisplayName("Publishes a markdown cell in the notation you wrote it in")
    @Description("""
            Markdown writes a collection back in the notation a table uses — square brackets for
            a list, braces for a set, key: value pairs for a map, nested to any depth — so a
            reader of the published spec sees the value spelled the way they would spell it
            themselves. Below, Cell is what a table row holds and Published cell is what the
            markdown report shows for it.
            """)
    @TableTest("""
        Scenario             | Cell                  | Published cell?
        A plain value        | 'Alice'               | 'Alice'
        An empty list        | '[]'                  | '[]'
        A list of values     | '[1, 2, 3]'           | '[1, 2, 3]'
        An empty set         | '{}'                  | '{}'
        A set of values      | '{1, 2, 3}'           | '{1, 2, 3}'
        An empty map         | '[:]'                 | '[:]'
        A map of values      | '[a: 1, b: 2]'        | '[a: 1, b: 2]'
        A list within a list | '[[1, 2], [a, b]]'    | '[[1, 2], [a, b]]'
        A map within a map   | '[a: [b: 1]]'         | '[a: [b: 1]]'
        Collections mixed    | '[a: [1, 2], b: {3}]' | '[a: [1, 2], b: {3}]'
        A list holding pipes | '["|", "|"]'          | '[\\|, \\|]'
        """)
    void publishesAMarkdownCellInTheNotationItWasWrittenIn(String cell, String publishedCell) {
        assertThat(PublishedCell.of("markdown", cell)).isEqualTo(publishedCell);
    }

    @DisplayName("Escapes a pipe where the format would end the cell at it")
    @Description("""
            Markdown and AsciiDoc both end a cell at a pipe, so a value holding one would split
            the row and the table would lose a column from that point on. Both escape every pipe
            the value contains, wherever it sits — in a plain value, in a sentence, or inside a
            collection. HTML has no cell delimiter to protect and leaves the character alone; it
            still counts a pipe as whitespace-significant, which is the ws class in its column.
            """)
    @TableTest("""
        Scenario                  | Cell                   | In markdown?           | In AsciiDoc?                   | In HTML?
        A value that is one pipe  | '"|"'                  | '\\|'                  | '\\|'                          | '<span class="literal ws">|</span>'
        A pipe inside a sentence  | '"Text with | a pipe"' | 'Text with \\| a pipe' | '++Text with ++\\|++ a pipe++' | '<span class="literal ws">Text with | a pipe</span>'
        A pipe inside a set       | '{"||"}'               | '{\\|\\|}'             | '* \\|\\|'                     | '<ul class="coll set"><li><span class="literal ws">||</span></li></ul>'
        A pipe inside a map value | '[b: "||"]'            | '[b: \\|\\|]'          | '++b++:: \\|\\|'               | '<dl class="coll map"><dt>b</dt><dd><span class="literal ws">||</span></dd></dl>'
        """)
    void escapesEveryPipeInsideAValue(String cell, String inMarkdown, String inAsciiDoc, String inHtml) {
        assertThat(PublishedCell.of("markdown", cell)).isEqualTo(inMarkdown);
        assertThat(PublishedCell.of("asciidoc", cell)).isEqualTo(inAsciiDoc);
        assertThat(PublishedCell.of("html", cell)).isEqualTo(inHtml);
    }

    @DisplayName("Renders significant whitespace so a reader can count it")
    @Description("""
            Significant means whitespace at either edge of the value, a run of two or more spaces,
            or a tab; a single space between words is left alone, and so is a value with no
            whitespace at all. Markdown and AsciiDoc are plain text with no styling to reach for,
            so they substitute a glyph into the value itself: an open box for a space, an arrow
            for a tab. AsciiDoc wraps every literal in ++ pass-through markers whatever its
            whitespace, which is why they appear throughout its column. HTML keeps the real
            characters and marks them instead — a sp span per run of spaces, a tab span per tab,
            with the stylesheet drawing the dot and the arrow over them — so a value copied off
            the page is still the value the row ran with. A run at the end of the line is marked
            trailing as well, since that is the one a layout cannot show even when it preserves
            whitespace.
            """)
    @TableTest("""
        Scenario              | Cell        | In markdown?                 | In AsciiDoc?                         | In HTML?
        Plain word            | Alice       | Alice                        | '++Alice++'                          | '<span class="literal">Alice</span>'
        Single internal space | Alice Smith | Alice Smith                  | '++Alice Smith++'                    | '<span class="literal">Alice Smith</span>'
        Leading space         | "' x'"      | '&#x2423;x'                  | '&#x2423;++x++'                      | '<span class="literal ws"><span class="sp"> </span>x</span>'
        Trailing space        | "'x '"      | 'x&#x2423;'                  | '++x++&#x2423;'                      | '<span class="literal ws">x<span class="sp trailing"> </span></span>'
        Run of two spaces     | "'a  b'"    | 'a&#x2423;&#x2423;b'         | '++a++&#x2423;&#x2423;++b++'         | '<span class="literal ws">a<span class="sp">  </span>b</span>'
        Tab between words     | "'a	b'"      | 'a&#x21E5;b'                 | '++a++&#x21E5;++b++'                 | '<span class="literal ws">a<span class="tab">	</span>b</span>'
        Run of two tabs       | "'		'"        | '&#x21E5;&#x21E5;'           | '&#x21E5;&#x21E5;'                   | '<span class="literal ws"><span class="tab">	</span><span class="tab">	</span></span>'
        Tab then space        | "'	 '"       | '&#x21E5;&#x2423;'           | '&#x21E5;&#x2423;'                   | '<span class="literal ws"><span class="tab">	</span><span class="sp trailing"> </span></span>'
        Spaces around a tab   | "'a 	 b'"    | 'a&#x2423;&#x21E5;&#x2423;b' | '++a++&#x2423;&#x21E5;&#x2423;++b++' | '<span class="literal ws">a<span class="sp"> </span><span class="tab">	</span><span class="sp"> </span>b</span>'
        Whitespace-only value | "'   '"     | '&#x2423;&#x2423;&#x2423;'   | '&#x2423;&#x2423;&#x2423;'           | '<span class="literal ws"><span class="sp trailing">   </span></span>'
        An empty value        | "''"        | '""'                         | '+""+'                               | '<span class="literal empty-string">“”</span>'
        """)
    void rendersSignificantWhitespace(String cell, String inMarkdown, String inAsciiDoc, String inHtml) {
        assertThat(PublishedCell.of("markdown", cell)).isEqualTo(inMarkdown);
        assertThat(PublishedCell.of("asciidoc", cell)).isEqualTo(inAsciiDoc);
        assertThat(PublishedCell.of("html", cell)).isEqualTo(inHtml);
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
                .isEqualTo("ab<span class=\"sp trailing\"> </span>\n<span class=\"sp\"> </span>cd");
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
