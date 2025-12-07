package io.github.nchaugen.tabletest.reporter.rendering;

import io.github.nchaugen.tabletest.reporter.ContextLoader;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

public class TableWithMapTest {

    private final Map<String, Object> context = new ContextLoader().fromYaml("""
        title: Map values
        headers:
          - value: "a"
          - value: "b"
          - value: "c"
        rows:
          - - value: {}
            - value:
                a: "1"
                b: "2"
                c: "3"
            - value:
                b: "||"
        """);

    @Test
    void supported_in_asciidoc() {
        assertThat(ASCIIDOC.renderTable(context))
            .isEqualTo("""
                == ++Map values++
                
                [%header,cols="1,1,1"]
                |===
                |++a++
                |++b++
                |++c++
                
                a| {empty}
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
        assertThat(MARKDOWN.renderTable(context))
            .isEqualTo("""
                ## Map values
                
                | a | b | c |
                | --- | --- | --- |
                | [:] | [a: 1, b: 2, c: 3] | [b: \\|\\|] |
                """
            );
    }

}
