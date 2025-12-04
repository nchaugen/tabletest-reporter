package io.github.nchaugen.tabletest.reporter;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

public class TableWithSetTest {

    private final Map<String, Object> context = new Context().fromYaml("""
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
        assertThat(MARKDOWN.renderTable(context))
            .isEqualTo("""
                ## Set values
                
                | a | b | c |
                | --- | --- | --- |
                | {} | {1, 2, 3} | {\\|\\|} |
                """
            );
    }

}
