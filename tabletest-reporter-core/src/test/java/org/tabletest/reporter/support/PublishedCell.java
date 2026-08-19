package org.tabletest.reporter.support;

import org.jsoup.nodes.Document;
import org.tabletest.reporter.BuiltInFormat;
import org.tabletest.reporter.Format;
import org.tabletest.reporter.TemplateEngine;

import java.util.Arrays;
import java.util.List;

/**
 * The cell a one-column, one-row table publishes for one written cell, in whichever built-in
 * format is asked for, so a rule about cell rendering states all three side by side instead of
 * splitting into one rule per format.
 *
 * <p>Each format is stripped of the chrome that is the same for every cell and carries nothing
 * about the value: markdown's surrounding pipes and padding, AsciiDoc's {@code a|} cell opener,
 * and in HTML the {@code <td>} itself. What is left is what that format made of the value —
 * including HTML's {@code literal} span, whose classes are how HTML says the value needs care.
 */
public final class PublishedCell {

    private static final String ASCIIDOC_CELL = "a|";
    private static final String TABLE_END = "|===";

    private PublishedCell() {}

    /** The published cell, for a value the format renders on one line. */
    public static String of(String formatName, String cell) {
        List<String> lines = linesOf(formatName, cell);
        if (lines.size() != 1) {
            throw new AssertionError("The published cell spans %d lines: %s".formatted(lines.size(), lines));
        }
        return lines.getFirst();
    }

    /** The published cell as its lines, for a value a format spreads over several. */
    public static List<String> linesOf(String formatName, String cell) {
        List<String> rendered = renderedLines(formatName, cell);
        return switch (formatName) {
            case "html" -> List.of(htmlCell(String.join("\n", rendered)));
            case "markdown" -> List.of(betweenPipes(rendered.getLast()));
            case "asciidoc" -> asciiDocCell(rendered);
            default -> throw new IllegalArgumentException("No built-in format named " + formatName);
        };
    }

    private static List<String> renderedLines(String formatName, String cell) {
        String rendered = new TemplateEngine()
                .renderTable(formatNamed(formatName), RenderBridge.contextFor("Value\n" + cell + "\n", "V"));
        return rendered.lines().filter(line -> !line.isBlank()).toList();
    }

    /** The inner markup of the row's only data cell, with the {@code <td>} chrome dropped. */
    private static String htmlCell(String rendered) {
        Document document = HtmlValidator.parse(rendered);
        document.outputSettings().prettyPrint(false);
        return document.select("tbody td").first().html();
    }

    private static String betweenPipes(String row) {
        return row.substring(1, row.length() - 1).trim();
    }

    /** The cell opener may carry the value or stand alone above the block that is the value. */
    private static List<String> asciiDocCell(List<String> rendered) {
        int opener = lastIndexOfCellOpener(rendered);
        String openerValue = rendered.get(opener).substring(ASCIIDOC_CELL.length());
        return openerValue.isEmpty()
                ? rendered.subList(opener + 1, rendered.lastIndexOf(TABLE_END))
                : List.of(openerValue);
    }

    private static int lastIndexOfCellOpener(List<String> rendered) {
        for (int line = rendered.size() - 1; line >= 0; line--) {
            if (rendered.get(line).startsWith(ASCIIDOC_CELL)) {
                return line;
            }
        }
        throw new AssertionError("The published AsciiDoc table has no data cell");
    }

    private static Format formatNamed(String formatName) {
        return Arrays.stream(BuiltInFormat.values())
                .filter(format -> format.formatName().equals(formatName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No built-in format named " + formatName));
    }
}
