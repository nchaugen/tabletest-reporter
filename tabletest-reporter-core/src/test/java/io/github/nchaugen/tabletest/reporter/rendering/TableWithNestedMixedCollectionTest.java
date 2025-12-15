package io.github.nchaugen.tabletest.reporter.rendering;

import io.github.nchaugen.tabletest.reporter.ContextLoader;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

public class TableWithNestedMixedCollectionTest {

    private final Map<String, Object> context = new ContextLoader().fromYaml("""
        "title": "Nested mixed collection"
        "headers":
        - "value": "a"
        "rows":
        - - "value":
              "a":
              - "1"
              - "2"
              "b": !!set
                "3": !!null "null"
                "4": !!null "null"
              "c": "5"
        - - "value": !!set
              ? "A": "1"
              : !!null "null"
              ? "B": "2"
              : !!null "null"
        - - "value":
              - "A": "1"
              - "B": "2"
        """);

    @Test
    void supported_in_asciidoc() {
        assertThat(ASCIIDOC.renderTable(context))
            .isEqualTo("""
                == ++Nested mixed collection++
                
                [%header,cols="1"]
                |===
                |++a++
                
                a|
                ++a++::
                ** ++1++
                ** ++2++
                ++b++::
                ** ++3++
                ** ++4++
                ++c++:: ++5++

                a|
                * {empty}
                ++A++::: ++1++
                * {empty}
                ++B++::: ++2++
                
                a|
                * {empty}
                ++A++::: ++1++
                * {empty}
                ++B++::: ++2++
                
                |===
                """
            );
    }

    @Test
    void supported_in_markdown() {
        assertThat(MARKDOWN.renderTable(context))
            .isEqualTo("""
                ## Nested mixed collection
                
                | a |
                | --- |
                | [a: [1, 2], b: {3, 4}, c: 5] |
                | {[A: 1], [B: 2]} |
                | [[A: 1], [B: 2]] |
                """
            );
    }

}
