package io.github.nchaugen.tabletest.reporter.rendering;

import io.github.nchaugen.tabletest.reporter.ContextLoader;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

public class TableWithNestedMapTest {

    private final Map<String, Object> context = new ContextLoader().fromYaml("""
        title: Nested map values
        headers:
          - value: "a"
          - value: "b"
        rows:
          - - value:
                a: {}
                b: {}
            - value:
                a:
                  A: "1"
                b:
                  B: "2"
        """);

    @Test
    void supported_in_asciidoc() {
        assertThat(ASCIIDOC.renderTable(context))
            .isEqualTo("""
                == ++Nested map values++
                
                [%header,cols="1,1"]
                |===
                |++a++
                |++b++
                
                a|
                ++a++:: {empty}
                ++b++:: {empty}
                a|
                ++a++::
                ++A++::: ++1++
                ++b++::
                ++B++::: ++2++
                
                |===
                """
            );
    }

    @Test
    void supported_in_markdown() {
        assertThat(MARKDOWN.renderTable(context))
            .isEqualTo("""
                ## Nested map values
                
                | a | b |
                | --- | --- |
                | [a: [:], b: [:]] | [a: [A: 1], b: [B: 2]] |
                """
            );
    }

}
