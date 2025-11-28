package io.github.nchaugen.tabletest.reporter;

import org.junit.jupiter.api.Test;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

public class TableWithNestedListTest {

    private static final String TABLE_CONTEXT_YAML = """
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
        """;

    @Test
    void supported_in_asciidoc() {
        assertThat(ASCIIDOC.renderTable(Context.fromYaml(TABLE_CONTEXT_YAML)))
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
    }

    @Test
    void supported_in_markdown() {
        assertThat(MARKDOWN.renderTable(Context.fromYaml(TABLE_CONTEXT_YAML)))
            .isEqualTo("""
                ## Nested list
                
                | a |
                | --- |
                | [[1, 2, 3], [a, b, c], [#, $, %]] |
                """
            );
    }

}
