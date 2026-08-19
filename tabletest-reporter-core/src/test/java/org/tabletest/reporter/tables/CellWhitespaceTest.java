package org.tabletest.reporter.tables;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.BuiltInFormat;
import org.tabletest.reporter.Format;
import org.tabletest.reporter.TemplateEngine;
import org.tabletest.reporter.pebble.FilterMarkWhitespace;
import org.tabletest.reporter.pebble.TestWhitespaceSignificant;
import org.tabletest.reporter.support.RenderBridge;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Cell whitespace")
@Description("""
        A space at the end of a value, a run of two, a tab: whitespace a reader could miscount or
        miss entirely, in a table whose whole point is the exact value a row ran with. Every format
        makes it visible, and which whitespace counts is the same question for all three. How they
        answer it is not: HTML keeps the real characters and draws a marker over them with CSS, so
        copy-paste and search still yield the original text, while markdown and AsciiDoc have no
        styling to reach for and substitute a glyph into the text instead.
        """)
public class CellWhitespaceTest {

    private final TestWhitespaceSignificant significance = new TestWhitespaceSignificant();
    private final FilterMarkWhitespace marker = new FilterMarkWhitespace();
    private final TemplateEngine templateEngine = new TemplateEngine();

    @DisplayName("Treats whitespace as significant at the value's edges, in runs, and in tabs")
    @Description("""
            Significant means: leading or trailing whitespace, any run of two or more spaces, any
            tab, and values aligned with pipes as in a formatted table row. A single space between
            words is not significant, and neither is a value with no whitespace at all.
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
        assertThat(significance.apply(value, Map.of(), null, null, 0)).isEqualTo(significant);
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
        assertThat(significance.apply(String.join("\n", lines), Map.of(), null, null, 0))
                .isEqualTo(significant);
    }

    @DisplayName("Wraps significant whitespace runs in marker spans")
    @Description("""
            HTML output: sp spans carry space runs and tab spans carry each tab, and the report
            stylesheet draws a centred dot and an arrow over them. The characters inside the spans
            are the real ones, so a value copied out of the page is the value the row ran with.
            Markup characters are escaped before marking, so cell content can never inject HTML.
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
        assertThat(marker.apply(value, Map.of(), null, null, 0)).isEqualTo(marked);
    }

    @DisplayName("Marks each line of a multi-line value and leaves the line breaks alone")
    @Description("""
            Each line is marked on its own, so a space at the end of one line and a space in front
            of the next each get their own span. The breaks between lines are left as they were —
            only the whitespace inside a line is wrapped.
            """)
    @TableTest("""
        Scenario                | Lines of the value | Marked lines?
        A space ending a line   | ['ab ', cd]        | ['ab<span class="sp"> </span>', cd]
        A space starting a line | [ab, ' cd']        | [ab, '<span class="sp"> </span>cd']
        Both, on the same value | ['ab ', ' cd']     | ['ab<span class="sp"> </span>', '<span class="sp"> </span>cd']
        """)
    void marksEachLineOfAMultiLineValue(List<String> lines, List<String> markedLines) {
        assertThat(marker.apply(String.join("\n", lines), Map.of(), null, null, 0))
                .isEqualTo(String.join("\n", markedLines));
    }

    @DisplayName("Encodes significant whitespace as a glyph where a format has no styling to reach for")
    @Description("""
            Markdown and AsciiDoc are plain text: the only channel they have is the text itself,
            so an open box stands for a space and an arrow for a tab, one per character. The value
            in the file is no longer the value the row ran with, which is the price of being able
            to see it at all — and the reason HTML does not do this. AsciiDoc wraps every literal
            in ++ pass-through markers whatever its whitespace, which is why they appear below.
            """)
    @TableTest("""
        Scenario              | Cell        | In markdown?               | In AsciiDoc?
        Plain word            | Alice       | Alice                      | '++Alice++'
        Single internal space | Alice Smith | Alice Smith                | '++Alice Smith++'
        Leading space         | "' x'"      | '&#x2423;x'                | '&#x2423;++x++'
        Trailing space        | "'x '"      | 'x&#x2423;'                | '++x++&#x2423;'
        Run of two spaces     | "'a  b'"    | 'a&#x2423;&#x2423;b'       | '++a++&#x2423;&#x2423;++b++'
        Tab between words     | "'a\tb'"    | 'a&#x21E5;b'               | '++a++&#x21E5;++b++'
        Whitespace-only value | "'   '"     | '&#x2423;&#x2423;&#x2423;' | '&#x2423;&#x2423;&#x2423;'
        """)
    void encodesSignificantWhitespaceAsGlyphs(String cell, String inMarkdown, String inAsciiDoc) {
        assertThat(publishedCellFor("markdown", cell)).isEqualTo(inMarkdown);
        assertThat(publishedCellFor("asciidoc", cell)).isEqualTo(inAsciiDoc);
    }

    @Test
    void nonStringValuesAreNeverSignificant() {
        assertThat(significance.apply(42, Map.of(), null, null, 0)).isFalse();
    }

    @Test
    void nullValuePassesThroughTheMarkerAsNull() {
        assertThat(marker.apply(null, Map.of(), null, null, 0)).isNull();
    }

    /**
     * The cell a one-column, one-row table publishes in the named format for a row written as
     * this cell text, rendered through the same path a real test run takes — see
     * {@link RenderBridge}. Markdown pads its cells and AsciiDoc opens each with {@code a|}.
     */
    private String publishedCellFor(String formatName, String cell) {
        String rendered = templateEngine.renderTable(
                formatNamed(formatName), RenderBridge.contextFor("Value\n" + cell + "\n", "V"));
        List<String> lines = rendered.lines().filter(line -> !line.isBlank()).toList();
        return formatName.equals("markdown")
                ? stripBetween(lines.getLast(), "|", "|")
                : lines.stream()
                        .filter(line -> line.startsWith("a|"))
                        .reduce((first, last) -> last)
                        .orElseThrow()
                        .substring("a|".length());
    }

    private static String stripBetween(String line, String opening, String closing) {
        return line.substring(opening.length(), line.length() - closing.length())
                .trim();
    }

    private static Format formatNamed(String formatName) {
        return Arrays.stream(BuiltInFormat.values())
                .filter(format -> format.formatName().equals(formatName))
                .findFirst()
                .orElseThrow();
    }
}
