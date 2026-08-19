package org.tabletest.reporter.rendering;

import org.junit.jupiter.api.DisplayName;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.TemplateEngine;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tabletest.reporter.BuiltInFormat.MARKDOWN;

@DisplayName("Cell rendering")
public class CellRenderingTest {

    private final TemplateEngine templateEngine = new TemplateEngine();

    @DisplayName("Publishes a cell in the notation it was written in")
    @Description("""
            The reporter publishes the value a row actually ran with, not the text of the cell,
            so a collection arrives as a collection and has to be written back out. It is written
            back in the notation the table format uses — square brackets for a list, braces for a
            set, key: value pairs for a map, nested to any depth — so a reader of the published
            spec sees the value spelled the way they would spell it themselves. Below, Cell is
            what a table row holds and Published cell is what the markdown report shows for it.
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
        """)
    void publishesACellInTheNotationItWasWrittenIn(String cell, String publishedCell) {
        assertThat(publishedCellFor(cell)).isEqualTo(publishedCell);
    }

    @DisplayName("Escapes a pipe inside a value so it cannot end the cell")
    @Description("""
            Markdown ends a cell at a pipe, so a value holding one would split the row and the
            table would lose a column from that point on. Every pipe the value itself contains is
            escaped instead, wherever it sits — in a plain value, inside a collection, or in a
            column header.
            """)
    @TableTest("""
        Scenario                  | Cell                   | Published cell?
        A value that is one pipe  | '"|"'                  | '\\|'
        A pipe inside a sentence  | '"Text with | a pipe"' | 'Text with \\| a pipe'
        Pipes inside a list       | '["|", "|"]'           | '[\\|, \\|]'
        Pipes inside a set        | '{"||"}'               | '{\\|\\|}'
        A pipe inside a map value | '[b: "||"]'            | '[b: \\|\\|]'
        """)
    void escapesEveryPipeInsideAValue(String cell, String publishedCell) {
        assertThat(publishedCellFor(cell)).isEqualTo(publishedCell);
    }

    /**
     * The markdown the report publishes for a one-column table whose single row holds this cell,
     * rendered through the same path a real test run takes — see {@link RenderBridge}.
     */
    private String publishedCellFor(String cell) {
        String rendered =
                templateEngine.renderTable(MARKDOWN, RenderBridge.contextFor("Value\n" + cell + "\n", "Values"));

        List<String> lines = rendered.lines().filter(line -> !line.isBlank()).toList();
        String dataRow = lines.getLast();
        return dataRow.substring(1, dataRow.length() - 1).trim();
    }
}
