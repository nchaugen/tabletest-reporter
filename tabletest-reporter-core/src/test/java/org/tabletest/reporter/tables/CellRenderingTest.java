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
        The reporter publishes the value a row ran with, and not the text of the cell. It therefore
        writes every value back out, and each format writes that value back the way that format
        can.

        Where the formats differ, the rules below show all three side by side. Markdown and
        AsciiDoc come first. Both are plain text, and both answer most questions alike. HTML comes
        last, because its answer is markup and always the longest.

        A collection does not fit that shape. Markdown alone puts a whole collection inside a table
        cell. AsciiDoc opens a bulleted block or a description list below the cell. HTML nests list
        markup inside the cell. A table can hold neither of those, so the notation rule belongs to
        markdown alone.
        """)
public class CellRenderingTest {

    @DisplayName("Publishes a markdown cell in the notation you wrote it in")
    @Description("""
            Markdown writes a collection back in the notation a table uses, nested to any depth. A list
            takes square brackets, a set takes braces, and a map takes key: value pairs. A reader of
            the published spec therefore meets the value spelled the way they would spell it
            themselves.

            Below, Cell is what a table row holds. Published cell is what the markdown report shows for
            that row.
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
            Markdown and AsciiDoc both end a cell at a pipe. A value that holds a pipe would split the
            row, and the table would lose a column from that point on. Both formats therefore escape
            every pipe in the value, wherever that pipe sits: in a plain value, in a sentence, or
            inside a collection.

            HTML has no cell delimiter to protect, so it leaves the character alone. It still counts a
            pipe as significant whitespace, which is the ws class in its column.
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
            Three kinds of whitespace are significant: whitespace at either edge of the value, a run of
            two or more spaces, and a tab. A single space between words stays as it is, and so does a
            value with no whitespace at all.

            Markdown and AsciiDoc are plain text, and have no styling to reach for. They therefore put
            a glyph into the value itself. A space becomes an open box, and a tab becomes an arrow.
            AsciiDoc wraps every literal in ++ pass-through markers, whatever whitespace that literal
            holds, which is why those markers run through its whole column.

            HTML holds the real characters and marks them instead: one sp span per run of spaces, one
            tab span per tab. The stylesheet draws the dot and the arrow over those spans. A value
            copied off the page is therefore still the value the row ran with. A run at the end of the
            line takes a trailing mark as well. A layout cannot show that run, even where it holds
            the whitespace.
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
