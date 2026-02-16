package org.tabletest.reporter.rendering;

import org.junit.jupiter.api.Test;
import org.tabletest.reporter.ContextLoader;
import org.tabletest.reporter.TemplateEngine;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tabletest.reporter.BuiltInFormat.ASCIIDOC;
import static org.tabletest.reporter.BuiltInFormat.MARKDOWN;
import static org.tabletest.reporter.rendering.AsciiDocValidator.assertValidAsciiDoc;
import static org.tabletest.reporter.rendering.MarkdownValidator.assertValidMarkdown;

public class IndexTest {

    private final TemplateEngine templateEngine = new TemplateEngine();

    private final Map<String, Object> context = new ContextLoader().fromYaml("""
        "title": "Title of the Test Class"
        "description": "A free-text description explaining what these tables are about."
        "name": "Test Class"
        "contents":
        - "name": "A Table"
          "type": "table"
          "path": "path/to/a_table"
        - "name": "B Table"
          "type": "table"
          "path": "path/to/b_table"
        - "name": "C Table"
          "type": "table"
          "path": "path/to/c_table"
        """);

    @Test
    void supported_in_asciidoc() {
        String rendered = templateEngine.renderIndex(ASCIIDOC, context);

        assertThat(rendered).isEqualTo("""
                = ++Title of the Test Class++

                A free-text description explaining what these tables are about.

                * xref:./path/to/a_table.adoc[++A Table++]
                * xref:./path/to/b_table.adoc[++B Table++]
                * xref:./path/to/c_table.adoc[++C Table++]
                """);
        assertValidAsciiDoc(rendered);
    }

    @Test
    void supported_in_markdown() {
        String rendered = templateEngine.renderIndex(MARKDOWN, context);

        assertThat(rendered).isEqualTo("""
                # Title of the Test Class

                A free-text description explaining what these tables are about.

                * [A Table](./path/to/a_table.md)
                * [B Table](./path/to/b_table.md)
                * [C Table](./path/to/c_table.md)
                """);
        assertValidMarkdown(rendered);
    }
}
