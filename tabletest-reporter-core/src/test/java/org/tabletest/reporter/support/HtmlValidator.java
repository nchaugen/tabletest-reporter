package org.tabletest.reporter.support;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.Map;

/**
 * Parses rendered HTML into a DOM and asserts its structure against the render context.
 * <p>
 * String-level {@code contains} assertions on the generated markup cannot catch an HTML
 * table that is structurally broken — a dropped cell, a row rendered with the wrong number
 * of columns, or a missing row — because the offending substring is simply absent rather
 * than wrong. This validator loads the output into a real DOM and asserts the rendered
 * structure against the context, which is the independent source of truth for what the
 * reporter intended, mirroring {@link AsciiDocValidator} for the HTML format.
 */
public class HtmlValidator {

    /**
     * Parses the given HTML into a queryable DOM so tests can assert with CSS selectors.
     *
     * @param html the rendered HTML content
     * @return the parsed document
     */
    public static Document parse(String html) {
        return Jsoup.parse(html);
    }

    /**
     * Asserts that the rendered table has one header cell per context header and that every
     * body row renders exactly that many data cells, over exactly as many rows as the context.
     *
     * @param html    the rendered HTML content
     * @param context the render context the HTML was produced from
     */
    public static void assertTableMatchesContext(String html, Map<String, Object> context) {
        Document document = parse(html);
        Elements tables = document.select("table");
        if (tables.isEmpty()) {
            throw new AssertionError("Expected a rendered table but found none");
        }
        Element table = tables.first();

        assertHeaderColumnsMatch(table, expectedColumns(context));
        assertBodyRowsMatch(table, expectedColumns(context), expectedRows(context));
    }

    private static void assertHeaderColumnsMatch(Element table, int expectedColumns) {
        int renderedColumns = table.select("thead > tr > th").size();
        if (renderedColumns != expectedColumns) {
            throw new AssertionError("Expected " + expectedColumns + " header cells from " + expectedColumns
                    + " headers, but rendered " + renderedColumns);
        }
    }

    private static void assertBodyRowsMatch(Element table, int expectedColumns, int expectedRows) {
        Elements rows = table.select("tbody > tr");
        if (rows.size() != expectedRows) {
            throw new AssertionError("Expected " + expectedRows + " body rows but rendered " + rows.size());
        }
        for (int i = 0; i < rows.size(); i++) {
            int cells = rows.get(i).select("> td").size();
            if (cells != expectedColumns) {
                throw new AssertionError(
                        "Expected row " + i + " to render " + expectedColumns + " cells but rendered " + cells);
            }
        }
    }

    private static int expectedColumns(Map<String, Object> context) {
        return ((List<?>) context.get("headers")).size();
    }

    private static int expectedRows(Map<String, Object> context) {
        return ((List<?>) context.get("rows")).size();
    }
}
