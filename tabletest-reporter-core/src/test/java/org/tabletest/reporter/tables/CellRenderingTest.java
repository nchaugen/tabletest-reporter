package org.tabletest.reporter.tables;

import org.junit.jupiter.api.DisplayName;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.support.PublishedCell;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Cell rendering")
@Description("""
        The reporter publishes the value a row actually ran with, not the text of the cell, so a
        collection arrives as a collection and has to be written back out. Each format writes it
        back the way that format expresses structure, and only markdown can put a whole collection
        inside a table cell: markdown writes the notation a TableTest table uses, AsciiDoc opens a
        bulleted or description-list block below the cell, HTML nests list markup inside it. The
        notation rule below is therefore markdown's alone; the pipe rule holds for all three and
        shows them side by side.
        """)
public class CellRenderingTest {

    @DisplayName("Publishes a markdown cell in the notation the value was written in")
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
        Scenario                  | Cell                   | In HTML?                                                                          | In markdown?           | In AsciiDoc?
        A value that is one pipe  | '"|"'                  | '<span class="literal ws">|</span>'                                               | '\\|'                  | '\\|'
        A pipe inside a sentence  | '"Text with | a pipe"' | '<span class="literal ws">Text with | a pipe</span>'                              | 'Text with \\| a pipe' | '++Text with ++\\|++ a pipe++'
        A pipe inside a set       | '{"||"}'               | '<ul class="coll set"><li><span class="literal ws">||</span></li></ul>'           | '{\\|\\|}'             | '* \\|\\|'
        A pipe inside a map value | '[b: "||"]'            | '<dl class="coll map"><dt>b</dt><dd><span class="literal ws">||</span></dd></dl>' | '[b: \\|\\|]'          | '++b++:: \\|\\|'
        """)
    void escapesEveryPipeInsideAValue(String cell, String inHtml, String inMarkdown, String inAsciiDoc) {
        assertThat(PublishedCell.of("html", cell)).isEqualTo(inHtml);
        assertThat(PublishedCell.of("markdown", cell)).isEqualTo(inMarkdown);
        assertThat(PublishedCell.of("asciidoc", cell)).isEqualTo(inAsciiDoc);
    }
}
