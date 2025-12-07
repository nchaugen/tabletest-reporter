package io.github.nchaugen.tabletest.reporter.rendering;

import io.github.nchaugen.tabletest.reporter.ContextLoader;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

public class TableWithListTest {

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
        assertThat(ASCIIDOC.renderTable(context))
            .isEqualTo("""
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
                """
            );
    }

    @Test
    void supported_in_markdown() {
        assertThat(MARKDOWN.renderTable(context))
            .isEqualTo("""
                ## List values
                
                | a | b | c |
                | --- | --- | --- |
                | [] | [1, 2, 3] | [\\|, \\|] |
                """
            );
    }

}
