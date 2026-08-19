package org.tabletest.reporter.support;

import org.tabletest.parser.TableParser;
import org.tabletest.reporter.junit.TableMetadata;

import java.util.Map;

/**
 * Turns the source text of a TableTest table into the render context a real test run would
 * produce, by driving the same path the JUnit extension drives: the TableTest parser builds the
 * table, and the extension's own {@code TableMetadata.toTableTestData(...).toMap()} shapes it for
 * the templates.
 *
 * <p>Rendering tests that build a context by hand instead have to restate the reporter's data
 * shape — including per-type YAML tags — and so can pass while diverging from what a real run
 * emits. Going through the extension's own code removes that risk, and lets a rule state its
 * input as the table a reader would actually write. {@code RenderBridgeFidelityTest} pins the
 * bridge against the YAML a real run writes.
 */
public final class RenderBridge {

    private RenderBridge() {}

    /** The render context for a table written as {@code source}, titled {@code title}. */
    public static Map<String, Object> contextFor(String source, String title) {
        return new TableMetadata()
                .withTitle(title)
                .toTableTestData(TableParser.parse(source))
                .toMap();
    }
}
