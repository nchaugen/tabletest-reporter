package org.tabletest.reporter.rendering;

import org.junit.jupiter.api.Test;
import org.tabletest.reporter.ContextLoader;
import org.tabletest.reporter.TemplateEngine;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tabletest.reporter.BuiltInFormat.ASCIIDOC;
import static org.tabletest.reporter.BuiltInFormat.MARKDOWN;
import static org.tabletest.reporter.support.AsciiDocValidator.assertValidAsciiDoc;
import static org.tabletest.reporter.support.MarkdownValidator.assertValidMarkdown;

public class TableWithNestedSetTest {

    private final TemplateEngine templateEngine = new TemplateEngine();

    private final Map<String, Object> context = new ContextLoader().fromYaml("""
        "title": "Nested set values"
        "headers":
        - "value": "a"
        "rows":
        - - "value": !!set
              ? !!set
                "1": !!null "null"
                "2": !!null "null"
                "3": !!null "null"
              : !!null "null"
              ? !!set
                "a": !!null "null"
                "b": !!null "null"
                "c": !!null "null"
              : !!null "null"
              ? !!set
                "#": !!null "null"
                "$": !!null "null"
                "%": !!null "null"
              : !!null "null"
        """);

    @Test
    void supported_in_asciidoc() {
        String rendered = templateEngine.renderTable(ASCIIDOC, context);

        assertThat(rendered).isEqualTo("""
                == ++Nested set values++

                [%header%autowidth,cols="1"]
                |===
                |++a++

                a|
                * {empty}
                ** ++1++
                ** ++2++
                ** ++3++
                * {empty}
                ** ++a++
                ** ++b++
                ** ++c++
                * {empty}
                ** ++#++
                ** ++$++
                ** ++%++

                |===
                """);
        assertValidAsciiDoc(rendered, context);
    }

    @Test
    void supported_in_markdown() {
        String rendered = templateEngine.renderTable(MARKDOWN, context);

        assertThat(rendered).isEqualTo("""
                ## Nested set values

                | a |
                | --- |
                | {{1, 2, 3}, {a, b, c}, {#, $, %}} |
                """);
        assertValidMarkdown(rendered);
    }
}
