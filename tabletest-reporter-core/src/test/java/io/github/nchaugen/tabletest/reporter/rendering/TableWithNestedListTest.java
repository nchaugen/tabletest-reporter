package io.github.nchaugen.tabletest.reporter.rendering;

import io.github.nchaugen.tabletest.reporter.ContextLoader;
import io.github.nchaugen.tabletest.reporter.TemplateEngine;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.nchaugen.tabletest.reporter.BuiltInFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.BuiltInFormat.MARKDOWN;
import static io.github.nchaugen.tabletest.reporter.rendering.AsciiDocValidator.assertValidAsciiDoc;
import static io.github.nchaugen.tabletest.reporter.rendering.MarkdownValidator.assertValidMarkdown;
import static org.assertj.core.api.Assertions.assertThat;

public class TableWithNestedListTest {

    private final TemplateEngine templateEngine = new TemplateEngine();

    private final Map<String, Object> context = new ContextLoader().fromYaml("""
        title: Nested list
        headers:
          - value: "a"
        rows:
          - - value:
              - - "1"
                - "2"
                - "3"
              - - "a"
                - "b"
                - "c"
              - - "#"
                - "$"
                - "%"
              type: "list"
        """);

    @Test
    void supported_in_asciidoc() {
        String rendered = templateEngine.renderTable(ASCIIDOC, context);

        assertThat(rendered)
            .isEqualTo("""
                == ++Nested list++

                [%header,cols="1"]
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
                """
            );
        assertValidAsciiDoc(rendered);
    }

    @Test
    void supported_in_markdown() {
        String rendered = templateEngine.renderTable(MARKDOWN, context);

        assertThat(rendered)
            .isEqualTo("""
                ## Nested list

                | a |
                | --- |
                | [[1, 2, 3], [a, b, c], [#, $, %]] |
                """
            );
        assertValidMarkdown(rendered);
    }

}
