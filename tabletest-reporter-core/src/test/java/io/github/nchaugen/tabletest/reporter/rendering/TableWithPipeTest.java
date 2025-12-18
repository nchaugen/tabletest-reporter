package io.github.nchaugen.tabletest.reporter.rendering;

import io.github.nchaugen.tabletest.reporter.ContextLoader;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static io.github.nchaugen.tabletest.reporter.rendering.MarkdownValidator.assertValidMarkdown;
import static org.assertj.core.api.Assertions.assertThat;

public class TableWithPipeTest {

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
        assertThat(ASCIIDOC.renderTable(context))
            .isEqualTo("""
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
                """
            );
    }

    @Test
    void supported_in_markdown() {
        String rendered = MARKDOWN.renderTable(context);

        assertThat(rendered)
            .isEqualTo("""
                ## Escaped pipes

                | a | b | a\\|b |
                | --- | --- | --- |
                | \\| | \\| | Text with \\| character |
                """
            );
        assertValidMarkdown(rendered);
    }

}
