package org.tabletest.reporter.rendering;

import org.junit.jupiter.api.Test;
import org.tabletest.reporter.ContextLoader;
import org.tabletest.reporter.TemplateEngine;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tabletest.reporter.BuiltInFormat.HTML;

/**
 * Verifies the self-contained HTML index page: a link-tree of relative,
 * type-aware links with nested children, and self-containment.
 */
public class HtmlIndexRenderingTest {

    private final TemplateEngine templateEngine = new TemplateEngine();

    private final Map<String, Object> context = new ContextLoader().fromYaml("""
        "title": "Title of the Test Class"
        "description": "What these tables are about."
        "name": "Test Class"
        "contents":
        - "name": "A Table"
          "type": "table"
          "path": "path/to/a_table"
        - "name": "Nested Package"
          "type": "index"
          "path": "path/to/pkg"
          "contents":
          - "name": "B Table"
            "type": "table"
            "path": "path/to/pkg/b_table"
        """);

    @Test
    void renders_self_contained_document_with_title_and_description() {
        String rendered = templateEngine.renderIndex(HTML, context);

        assertThat(rendered)
                .startsWith("<!DOCTYPE html>")
                .contains("<title>Title of the Test Class</title>")
                .contains("What these tables are about.")
                .doesNotContain("http://")
                .doesNotContain("https://");
    }

    @Test
    void renders_relative_type_aware_links_with_nesting() {
        String rendered = templateEngine.renderIndex(HTML, context);

        assertThat(rendered)
                .contains("<a href=\"./path/to/a_table.html\">A Table</a>")
                .contains("<a href=\"./path/to/pkg/index.html\">Nested Package</a>")
                .contains("<ul class=\"nav-children\">")
                .contains("<a href=\"./path/to/pkg/b_table.html\">B Table</a>");
    }
}
