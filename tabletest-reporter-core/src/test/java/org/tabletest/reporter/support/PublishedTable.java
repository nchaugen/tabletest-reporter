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
 * run records states all three side by side instead of splitting into one rule per format.
 *
 * <p>Cells come back as the format wrote them — a {@code <th>} with its classes in HTML, a role
 * prefix on the value in AsciiDoc, the bare text in markdown — because how a mark reaches the page
 * is the thing those rules are about, and naming the mark instead would hide that the three do
 * quite different things to arrive at it.
 */
public final class PublishedTable {

    /** {@code a|[.scenario.passed]#++A century year++#} — the roles, then the value. */
    private static final Pattern ASCIIDOC_CELL = Pattern.compile("^a?\\|(?:\\[([^]]*)]#)?\\+\\+(.*?)\\+\\+#?$");

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
        Document parsed = null;
        if (formatName.equals("html")) {
            parsed = HtmlValidator.parse(String.join("\n", lines));
            parsed.outputSettings().prettyPrint(false);
        }
        return new PublishedTable(formatName, parsed, lines);
    }

    /** The header cell of the named column, as the format wrote it. */
    public String headerCellOf(String column) {
        return switch (formatName) {
            case "html" -> htmlHeader(column).outerHtml();
            case "asciidoc" -> asciiDocHeaders().get(columnIndex(column)).substring(1);
            default -> markdownCells(markdownRows().getFirst()).get(columnIndex(column));
        };
    }

    /** The cell the named row holds in the named column, as the format wrote it. */
    public String cellOf(String row, String column) {
        int column0 = columnIndex(column);
        return switch (formatName) {
            case "html" -> htmlRow(row).select("td").get(column0).outerHtml();
            case "asciidoc" -> asciiDocRow(row).get(column0).substring("a|".length());
            default -> markdownCells(markdownRow(row)).get(column0);
        };
    }

    /** The mark the named column's header carries beyond being a cell, or null where it has none. */
    public String markOf(String column) {
        return switch (formatName) {
            case "html" -> marksOf(htmlHeader(column).classNames());
            case "asciidoc" -> rolesOf(asciiDocHeaders().get(columnIndex(column)));
            default -> null;
        };
    }

    /** The verdict every cell of the named row is marked with, or null where the format shows none. */
    public String verdictOf(String row) {
        return switch (formatName) {
            case "html" -> verdictIn(htmlRow(row).select("td").stream().flatMap(cell -> cell.classNames().stream()));
            case "asciidoc" -> verdictIn(asciiDocRow(row).stream().flatMap(cell -> roleNamesOf(cell).stream()));
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

    /** The columns in the order the published header lists them. */
    public List<String> columns() {
        return switch (formatName) {
            case "html" -> html.select("thead th").stream().map(Element::text).toList();
            case "asciidoc" ->
                asciiDocHeaders().stream().map(PublishedTable::valueOf).toList();
            default -> markdownCells(markdownRows().getFirst());
        };
    }

    private int columnIndex(String column) {
        int index = columns().indexOf(column);
        if (index < 0) throw noSuch("column", column);
        return index;
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

    private List<String> asciiDocHeaders() {
        return lines.stream()
                .filter(line ->
                        line.startsWith("|") && ASCIIDOC_CELL.matcher(line).matches())
                .toList();
    }

    private List<String> asciiDocRow(String row) {
        List<String> cells =
                lines.stream().filter(line -> line.startsWith("a|")).toList();
        int naming = cells.indexOf(cells.stream()
                .filter(cell -> valueOf(cell).equals(row))
                .findFirst()
                .orElseThrow(() -> noSuch("row", row)));
        return cells.subList(naming, Math.min(naming + columns().size(), cells.size()));
    }

    private static String valueOf(String cell) {
        return matched(cell).group(2);
    }

    private static String rolesOf(String cell) {
        String roles = matched(cell).group(1);
        return roles == null ? null : roles.substring(1).replace('.', ' ');
    }

    private static List<String> roleNamesOf(String cell) {
        String roles = rolesOf(cell);
        return roles == null ? List.of() : List.of(roles.split(" "));
    }

    private static Matcher matched(String cell) {
        Matcher matcher = ASCIIDOC_CELL.matcher(cell);
        if (!matcher.matches()) throw new AssertionError("Not a published AsciiDoc cell: " + cell);
        return matcher;
    }

    // ----- Markdown: a row is pipe-delimited, the second row being the header rule --------------

    private List<String> markdownRows() {
        return lines.stream()
                .filter(line -> line.startsWith("|") && !line.startsWith("| ---"))
                .toList();
    }

    private String markdownRow(String row) {
        return markdownRows().stream()
                .filter(line -> markdownCells(line).getFirst().equals(row))
                .findFirst()
                .orElseThrow(() -> noSuch("row", row));
    }

    private static List<String> markdownCells(String row) {
        return List.of(row.substring(1, row.length() - 1).split("\\|")).stream()
                .map(String::trim)
                .toList();
    }

    // ----- Markdown and AsciiDoc: the message is fenced below a "Failed Rows" heading ----------

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
