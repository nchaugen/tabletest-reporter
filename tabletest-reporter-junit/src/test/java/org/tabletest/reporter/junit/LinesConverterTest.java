package org.tabletest.reporter.junit;

import org.junit.jupiter.api.DisplayName;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LinesConverterTest {

    @DisplayName("Reads a cell as the lines of one block of text")
    @Description("""
        A cell holding a single value counts as one line, so a one-line block needs no list
        notation. Every element is read as text, whatever notation it was written in.
        """)
    @TableTest("""
        Scenario              | Cell Value        | Lines?
        Several lines         | [alpha, beta]     | [alpha, beta]
        One line in a list    | [alpha]           | [alpha]
        A plain value         | alpha             | [alpha]
        A line holding a pipe | ["alpha | beta"]  | ["alpha | beta"]
        A line held blank     | [alpha, '', beta] | [alpha, '', beta]
        No lines at all       | []                | []
        """)
    void readsCellAsLines(Object cellValue, List<String> lines) {
        assertEquals(lines, LinesConverter.toLines(cellValue));
    }

    @DisplayName("Joins the lines of a cell into the text they make up")
    @Description("""
        The expectation is written as the lines of the text, per the convention that keeps a
        multi-line value legible in a table, and joined in the method to make the text itself.
        """)
    @TableTest("""
        Scenario           | Cell Value        | Text?
        Several lines      | [alpha, beta]     | [alpha, beta]
        One line in a list | [alpha]           | [alpha]
        A plain value      | alpha             | [alpha]
        A line held blank  | [alpha, '', beta] | [alpha, '', beta]
        No lines at all    | []                | []
        """)
    void joinsLinesIntoText(Object cellValue, List<String> text) {
        assertEquals(String.join("\n", text), LinesConverter.toText(cellValue));
    }
}
