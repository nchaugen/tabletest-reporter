package org.tabletest.reporter.rendering;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.ast.Table;

import java.util.List;
import java.util.Map;

/**
 * Validates AsciiDoc syntax by attempting to parse it.
 * <p>
 * Uses AsciidoctorJ parser to verify that generated AsciiDoc is syntactically valid.
 * Invalid AsciiDoc will cause parsing exceptions.
 */
class AsciiDocValidator {

    private static final Asciidoctor ASCIIDOCTOR = Asciidoctor.Factory.create();

    /**
     * Validates that the given AsciiDoc content can be successfully parsed.
     *
     * @param asciidoc the AsciiDoc content to validate
     * @throws RuntimeException if the AsciiDoc is invalid
     */
    static void assertValidAsciiDoc(String asciidoc) {
        try {
            ASCIIDOCTOR.load(asciidoc, Options.builder().build());
        } catch (Exception e) {
            throw new AssertionError("Invalid AsciiDoc syntax: " + e.getMessage(), e);
        }
    }

    /**
     * Validates that rendered table AsciiDoc parses and renders as many columns as the
     * source context has headers.
     * <p>
     * Source-level golden assertions cannot catch a table that parses but renders with the
     * wrong column count (e.g. Asciidoctor inferring a single column when each cell sits on
     * its own line without an explicit {@code cols} attribute). This asserts the rendered
     * structure against the context, which is the independent source of truth for how many
     * columns the reporter intended.
     *
     * @param asciidoc the rendered AsciiDoc content
     * @param context  the render context the AsciiDoc was produced from
     */
    static void assertValidAsciiDoc(String asciidoc, Map<String, Object> context) {
        assertValidAsciiDoc(asciidoc);
        assertColumnsMatchHeaders(asciidoc, expectedColumns(context));
    }

    private static void assertColumnsMatchHeaders(String asciidoc, int expectedColumns) {
        Document document = ASCIIDOCTOR.load(asciidoc, Options.builder().build());
        List<StructuralNode> tables = document.findBy(Map.of("context", ":table"));
        if (tables.isEmpty()) {
            throw new AssertionError("Expected a rendered table but found none");
        }
        int renderedColumns = ((Table) tables.get(0)).getColumns().size();
        if (renderedColumns != expectedColumns) {
            throw new AssertionError("Expected " + expectedColumns + " columns from " + expectedColumns
                    + " headers, but Asciidoctor inferred " + renderedColumns);
        }
    }

    private static int expectedColumns(Map<String, Object> context) {
        return ((List<?>) context.get("headers")).size();
    }
}
