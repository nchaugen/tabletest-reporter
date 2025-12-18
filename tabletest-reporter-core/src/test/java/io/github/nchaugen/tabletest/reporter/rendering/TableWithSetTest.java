package io.github.nchaugen.tabletest.reporter.rendering;

import io.github.nchaugen.tabletest.reporter.ContextLoader;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static io.github.nchaugen.tabletest.reporter.rendering.MarkdownValidator.assertValidMarkdown;
import static org.assertj.core.api.Assertions.assertThat;

public class TableWithSetTest {

    private final Map<String, Object> context = new ContextLoader().fromYaml("""
        "title": "Set values"
        "headers":
        - "value": "a"
        - "value": "b"
        - "value": "c"
        "rows":
        - - "value": !!set {
              }
          - "value": !!set
              "1": !!null "null"
              "2": !!null "null"
              "3": !!null "null"
          - "value": !!set
              "||": !!null "null"
        """);

    @Test
    void supported_in_asciidoc() {
        assertThat(ASCIIDOC.renderTable(context))
            .isEqualTo("""
                == ++Set values++
                
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
                * \\|\\|
                
                |===
                """
            );
    }

    @Test
    void supported_in_markdown() {
        String rendered = MARKDOWN.renderTable(context);

        assertThat(rendered)
            .isEqualTo("""
                ## Set values

                | a | b | c |
                | --- | --- | --- |
                | {} | {1, 2, 3} | {\\|\\|} |
                """
            );
        assertValidMarkdown(rendered);
    }

}
