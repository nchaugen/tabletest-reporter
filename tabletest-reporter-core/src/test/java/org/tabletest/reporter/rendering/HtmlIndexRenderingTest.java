package org.tabletest.reporter.rendering;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.tabletest.reporter.ContextLoader;
import org.tabletest.reporter.TemplateEngine;
import org.tabletest.reporter.support.HtmlValidator;

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
        "status":
          "state": "failed"
          "total": !!int "5"
          "passed": !!int "3"
          "broken": !!int "2"
        "contents":
        - "name": "A Table"
          "type": "table"
          "path": "path/to/a_table"
          "status": "passed"
        - "name": "Nested Package"
          "type": "index"
          "path": "path/to/pkg"
          "status": "failed"
          "contents":
          - "name": "B Table"
            "type": "table"
            "path": "path/to/pkg/b_table"
            "status": "failed"
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
        Document document = HtmlValidator.parse(templateEngine.renderIndex(HTML, context));

        assertThat(document.select("a.nav-row").eachAttr("href"))
                .contains("./path/to/a_table.html", "./path/to/pkg/index.html", "./path/to/pkg/b_table.html");
        assertThat(document.select("details.nav-branch > ul.nav-children > li.nav-item a.nav-row")
                        .eachText())
                .contains("B Table");
    }

    @Test
    void marks_each_nav_item_with_its_rolled_up_status() {
        Document document = HtmlValidator.parse(templateEngine.renderIndex(HTML, context));

        assertThat(document.select("a.nav-row.table.passed").text()).isEqualTo("A Table");
        assertThat(document.select("a.nav-row.index.failed").first().text()).contains("Nested Package");
        assertThat(document.select("a.nav-row.table.failed").text()).isEqualTo("B Table");
    }

    @Test
    void summarises_the_pass_rate_in_the_masthead() {
        String rendered = templateEngine.renderIndex(HTML, context);

        assertThat(rendered)
                .contains("class=\"verdict fail\"")
                .contains("<span class=\"count\">2</span> of 5 scenarios broken");
    }
}
