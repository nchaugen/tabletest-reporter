package io.github.nchaugen.tabletest.reporter;

import org.junit.jupiter.api.Test;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

public class TableWithMapTest {

    private static final String TABLE_CONTEXT_YAML = """
        title: Map values
        headers:
          - value: "a"
          - value: "b"
          - value: "c"
        rows:
          - - value: {}
              type: "map"
            - value:
                a: "1"
                b: "2"
                c: "3"
              type: "map"
            - value:
                b: "||"
              type: "map"
        """;

    @Test
    void supported_in_asciidoc() {
        assertThat(ASCIIDOC.renderTable(Context.fromYaml(TABLE_CONTEXT_YAML)))
            .isEqualTo("""
                == ++Map values++
                
                [%header,cols="1,1,1"]
                |===
                |++a++
                |++b++
                |++c++
                
                a|{empty}
                a|
                ++a++:: ++1++
                ++b++:: ++2++
                ++c++:: ++3++
                a|
                ++b++:: \\|\\|
                
                |===
                """
            );
    }

    @Test
    void supported_in_markdown() {
        assertThat(MARKDOWN.renderTable(Context.fromYaml(TABLE_CONTEXT_YAML)))
            .isEqualTo("""
                ## Map values
                
                | a | b | c |
                | --- | --- | --- |
                | [:] | [a: 1, b: 2, c: 3] | [b: \\|\\|] |
                """
            );
    }

}
