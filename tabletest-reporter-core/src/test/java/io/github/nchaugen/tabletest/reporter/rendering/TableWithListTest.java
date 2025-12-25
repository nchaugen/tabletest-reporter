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

public class TableWithListTest {

    private final TemplateEngine templateEngine = new TemplateEngine();

    private final Map<String, Object> context = new ContextLoader().fromYaml("""
        title: List values
        headers:
          - value: "a"
          - value: "b"
          - value: "c"
        rows:
          - - value: []
              type: "list"
            - value:
              - 1
              - 2
              - 3
              type: "list"
            - value:
              - "|"
              - "|"
              type: "list"
        """);

    @Test
    void supported_in_asciidoc() {
        String rendered = templateEngine.renderTable(ASCIIDOC, context);

        assertThat(rendered).isEqualTo("""
                == ++List values++

                [%header,cols="1,1,1"]
                |===
                |++a++
                |++b++
                |++c++

                a|{empty}
                a|
                * ++1++
                * ++2++
                * ++3++
                a|
                * \\|
                * \\|

                |===
                """);
        assertValidAsciiDoc(rendered);
    }

    @Test
    void supported_in_markdown() {
        String rendered = templateEngine.renderTable(MARKDOWN, context);

        assertThat(rendered).isEqualTo("""
                ## List values

                | a | b | c |
                | --- | --- | --- |
                | [] | [1, 2, 3] | [\\|, \\|] |
                """);
        assertValidMarkdown(rendered);
    }
}
