package io.github.nchaugen.tabletest.reporter;

import org.junit.jupiter.api.Test;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

public class TableWithListTest {

    private static final String TABLE_CONTEXT_YAML = """
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
        """;

    @Test
    void supported_in_asciidoc() {
        assertThat(ASCIIDOC.renderTable(Context.fromYaml(TABLE_CONTEXT_YAML)))
            .isEqualTo("""
                == ++List values++
                
                [%header,cols="1,1,1"]
                |===
                |++a++
                |++b++
                |++c++
                
                a|{empty}
                a|
                . ++1++
                . ++2++
                . ++3++
                a|
                . \\|
                . \\|
                
                |===
                """
            );
    }

    @Test
    void supported_in_markdown() {
        assertThat(MARKDOWN.renderTable(Context.fromYaml(TABLE_CONTEXT_YAML)))
            .isEqualTo("""
                ## List values
                
                | a | b | c |
                | --- | --- | --- |
                | [] | [1, 2, 3] | [\\|, \\|] |
                """
            );
    }

}
