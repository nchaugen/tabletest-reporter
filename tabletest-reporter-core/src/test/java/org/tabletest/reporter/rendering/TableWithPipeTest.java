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

public class TableWithPipeTest {

    private final TemplateEngine templateEngine = new TemplateEngine();

    private final Map<String, Object> context = new ContextLoader().fromYaml("""
        title: Escaped pipes
        headers:
          - value: "a"
          - value: "b"
          - value: "a|b"
        rows:
          - - value: "|"
            - value: '|'
            - value: "Text with | character"
        """);

    @Test
    void supported_in_asciidoc() {
        String rendered = templateEngine.renderTable(ASCIIDOC, context);

        assertThat(rendered).isEqualTo("""
                == ++Escaped pipes++

                [%header,cols="1,1,1"]
                |===
                |++a++
                |++b++
                |++a++\\|++b++

                a|\\|
                a|\\|
                a|++Text with ++\\|++ character++

                |===
                """);
        assertValidAsciiDoc(rendered);
    }

    @Test
    void supported_in_markdown() {
        String rendered = templateEngine.renderTable(MARKDOWN, context);

        assertThat(rendered).isEqualTo("""
                ## Escaped pipes

                | a | b | a\\|b |
                | --- | --- | --- |
                | \\| | \\| | Text with \\| character |
                """);
        assertValidMarkdown(rendered);
    }
}
