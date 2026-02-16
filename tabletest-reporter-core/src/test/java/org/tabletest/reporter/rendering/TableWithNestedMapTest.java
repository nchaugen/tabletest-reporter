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

public class TableWithNestedMapTest {

    private final TemplateEngine templateEngine = new TemplateEngine();

    private final Map<String, Object> context = new ContextLoader().fromYaml("""
        title: Nested map values
        headers:
          - value: "a"
          - value: "b"
        rows:
          - - value:
                a: {}
                b: {}
            - value:
                a:
                  A: "1"
                b:
                  B: "2"
        """);

    @Test
    void supported_in_asciidoc() {
        String rendered = templateEngine.renderTable(ASCIIDOC, context);

        assertThat(rendered).isEqualTo("""
                == ++Nested map values++

                [%header,cols="1,1"]
                |===
                |++a++
                |++b++

                a|
                ++a++:: {empty}
                ++b++:: {empty}
                a|
                ++a++::
                ++A++::: ++1++
                ++b++::
                ++B++::: ++2++

                |===
                """);
        assertValidAsciiDoc(rendered);
    }

    @Test
    void supported_in_markdown() {
        String rendered = templateEngine.renderTable(MARKDOWN, context);

        assertThat(rendered).isEqualTo("""
                ## Nested map values

                | a | b |
                | --- | --- |
                | [a: [:], b: [:]] | [a: [A: 1], b: [B: 2]] |
                """);
        assertValidMarkdown(rendered);
    }
}
