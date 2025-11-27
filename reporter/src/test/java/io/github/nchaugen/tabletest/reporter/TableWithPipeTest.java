package io.github.nchaugen.tabletest.reporter;

import org.junit.jupiter.api.Test;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

public class TableWithPipeTest {

    private static final String TABLE_CONTEXT_YAML = """
        title: Escaped pipes
        headers:
          - value: "a"
          - value: "b"
          - value: "a|b"
        rows:
          - - value: "|"
            - value: '|'
            - value: "Text with | character"
        """;

    @Test
    void supported_in_asciidoc() {
        assertThat(ASCIIDOC.renderTable(Context.fromYaml(TABLE_CONTEXT_YAML)))
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
        assertThat(MARKDOWN.renderTable(Context.fromYaml(TABLE_CONTEXT_YAML)))
            .isEqualTo("""
                ## Escaped pipes
                
                | a | b | a\\|b |
                | --- | --- | --- |
                | \\| | \\| | Text with \\| character |
                """
            );
    }

}
