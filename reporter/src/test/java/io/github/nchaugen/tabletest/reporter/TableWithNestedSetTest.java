package io.github.nchaugen.tabletest.reporter;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

public class TableWithNestedSetTest {

    private final Map<String, Object> context = new Context().fromYaml("""
        "title": "Nested set values"
        "headers":
        - "value": "a"
        "rows":
        - - "value": !!set
              ? !!set
                "1": !!null "null"
                "2": !!null "null"
                "3": !!null "null"
              : !!null "null"
              ? !!set
                "a": !!null "null"
                "b": !!null "null"
                "c": !!null "null"
              : !!null "null"
              ? !!set
                "#": !!null "null"
                "$": !!null "null"
                "%": !!null "null"
              : !!null "null"
        """);

    @Test
    void supported_in_asciidoc() {
        assertThat(ASCIIDOC.renderTable(context))
            .isEqualTo("""
                == ++Nested set values++
                
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
        assertThat(MARKDOWN.renderTable(context))
            .isEqualTo("""
                ## Nested set values
                
                | a |
                | --- |
                | {{1, 2, 3}, {a, b, c}, {#, $, %}} |
                """
            );
    }

}
