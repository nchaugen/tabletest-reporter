package org.tabletest.reporter.support;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One published table page, read the same way whatever format rendered it, so a rule about what a
 * run records states one expectation per format instead of splitting into one rule per format.
 *
 * <p>HTML carries a mark as a class on the cell and AsciiDoc as a role prefix on the cell value;
 * markdown has nowhere to put one, and reads back as no mark at all. The message a broken row
 * failed on is published by every format, each below a heading of its own and fenced its own way.
 */
public final class PublishedTable {

    /** {@code a|[.scenario.passed]#++A century year++#} — the roles, then the value. */
    private static final Pattern CELL = Pattern.compile("^a?\\|(?:\\[([^]]*)]#)?\\+\\+(.*?)\\+\\+#?$");

    private final String formatName;
    private final Document html;
    private final List<String> lines;

    private PublishedTable(String formatName, Document html, List<String> lines) {
        this.formatName = formatName;
        this.html = html;
        this.lines = lines;
    }

    /** The table page of a report generated in the named format from what a real run published. */
    public static PublishedTable of(Path publishedRunOutput, String formatName, Path workingDir) {
        List<String> lines = PublishedReport.pageLinesOf(publishedRunOutput, formatName, null, false, workingDir);
        return new PublishedTable(
                formatName, formatName.equals("html") ? HtmlValidator.parse(String.join("\n", lines)) : null, lines);
    }

    /** The mark the named column's header carries beyond being a cell, or null where it carries none. */
    public String markOf(String column) {
        return switch (formatName) {
            case "html" -> marksOf(htmlHeader(column).classNames());
            case "asciidoc" ->
                rolesOf(asciiDocCells("|").filter(cell -> cell.value().equals(column)), column);
            default -> null;
        };
    }

    /** The verdict every cell of the named row is marked with, or null where the format shows none. */
    public String verdictOf(String row) {
        return switch (formatName) {
            case "html" -> verdictIn(htmlRow(row).select("td").stream().flatMap(cell -> cell.classNames().stream()));
            case "asciidoc" -> verdictIn(asciiDocRow(row).stream().flatMap(cell -> cell.roleNames().stream()));
            default -> null;
        };
    }

    /**
     * The lines of the message published below the table for the named row, or null where the row
     * did not break.
     */
    public List<String> failureMessageOf(String row) {
        return formatName.equals("html") ? htmlFailureMessage(row) : fencedFailureMessage(row);
    }

    // ----- HTML: a mark is a class on the cell -------------------------------------------------

    private Element htmlHeader(String column) {
        return html.select("thead th").stream()
                .filter(cell -> cell.text().equals(column))
                .findFirst()
                .orElseThrow(() -> noSuch("column", column));
    }

    private Element htmlRow(String row) {
        return html.select("tbody tr").stream()
                .filter(cells -> cells.select("td").first().text().equals(row))
                .findFirst()
                .orElseThrow(() -> noSuch("row", row));
    }

    private List<String> htmlFailureMessage(String row) {
        return html.select("section.failures details").stream()
                .filter(broken -> broken.select("summary").text().endsWith(row))
                .map(broken -> broken.select("pre").text().lines().toList())
                .findFirst()
                .orElse(null);
    }

    private static String marksOf(Set<String> classNames) {
        Set<String> marks = new LinkedHashSet<>(classNames);
        marks.remove("cell");
        return marks.isEmpty() ? null : String.join(" ", marks);
    }

    // ----- AsciiDoc: a mark is a role prefix on the cell value ---------------------------------

    private java.util.stream.Stream<Cell> asciiDocCells(String prefix) {
        return lines.stream()
                .filter(line -> line.startsWith(prefix) && CELL.matcher(line).matches())
                .map(PublishedTable::cell);
    }

    /** The three cells of the named row: the one naming it, and the two that follow. */
    private List<Cell> asciiDocRow(String row) {
        List<Cell> cells = asciiDocCells("a|").toList();
        int naming = java.util.stream.IntStream.range(0, cells.size())
                .filter(index -> cells.get(index).value().equals(row))
                .findFirst()
                .orElseThrow(() -> noSuch("row", row));
        return cells.subList(naming, Math.min(naming + 3, cells.size()));
    }

    private static String rolesOf(java.util.stream.Stream<Cell> cells, String column) {
        return cells.findFirst().orElseThrow(() -> noSuch("column", column)).roles();
    }

    private static Cell cell(String line) {
        Matcher cell = CELL.matcher(line);
        if (!cell.matches()) throw new AssertionError("Not a published AsciiDoc cell: " + line);
        String roles = cell.group(1);
        return new Cell(roles == null ? null : roles.substring(1).replace('.', ' '), cell.group(2));
    }

    private record Cell(String roles, String value) {
        List<String> roleNames() {
            return roles == null ? List.of() : List.of(roles.split(" "));
        }
    }

    // ----- Markdown and AsciiDoc: the message is fenced below a "Failed Rows" heading ----------

    /**
     * Markdown fences with {@code ```} and AsciiDoc with {@code ----}; both leave a blank line
     * inside the fence that carries no part of the message.
     */
    private List<String> fencedFailureMessage(String row) {
        String fence = formatName.equals("markdown") ? "```" : "----";
        int entry = indexOfEntryNaming(row);
        if (entry < 0) return null;
        int open = lines.subList(entry, lines.size()).indexOf(fence) + entry;
        int close = lines.subList(open + 1, lines.size()).indexOf(fence) + open + 1;
        return lines.subList(open + 1, close).stream()
                .dropWhile(String::isBlank)
                .toList();
    }

    /** A broken row is named in bold, prefixed by the number of the row that broke. */
    private int indexOfEntryNaming(String row) {
        String entry = row + (formatName.equals("markdown") ? "**" : "*");
        return java.util.stream.IntStream.range(0, lines.size())
                .filter(line ->
                        lines.get(line).startsWith("*") && lines.get(line).endsWith(entry))
                .findFirst()
                .orElse(-1);
    }

    private static String verdictIn(java.util.stream.Stream<String> marks) {
        Set<String> verdicts = marks.filter(mark -> mark.equals("passed") || mark.equals("failed"))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (verdicts.isEmpty()) return null;
        return verdicts.size() == 1 ? verdicts.iterator().next() : String.join(" and ", verdicts);
    }

    private static AssertionError noSuch(String kind, String name) {
        return new AssertionError("The published table has no " + kind + " " + name);
    }
}
