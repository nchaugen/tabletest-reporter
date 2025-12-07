package io.github.nchaugen.tabletest.reporter.rendering;

import io.github.nchaugen.tabletest.reporter.ContextLoader;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

public class TestIndexTest {

    private final Map<String, Object> context = new ContextLoader().fromYaml("""
        "title": "Title of the Test Class"
        "description": "A free-text description explaining what these tables are about."
        "name": "Test Class"
        "contents":
        - "name": "A Table"
          "type": "table"
          "path": "path/to/a_table"
        - "name": "B Table"
          "type": "table"
          "path": "path/to/b_table"
        - "name": "C Table"
          "type": "table"
          "path": "path/to/c_table"
        """);

    @Test
    void supported_in_asciidoc() {
        assertThat(ASCIIDOC.renderIndex(context))
            .isEqualTo("""
                = ++Title of the Test Class++
                
                A free-text description explaining what these tables are about.
                
                * xref:./path/to/a_table.adoc[++A Table++]
                * xref:./path/to/b_table.adoc[++B Table++]
                * xref:./path/to/c_table.adoc[++C Table++]
                """
            );
    }

    @Test
    void supported_in_markdown() {
        assertThat(MARKDOWN.renderIndex(context))
            .isEqualTo("""
                # Title of the Test Class
                
                A free-text description explaining what these tables are about.
                
                * [A Table](./path/to/a_table.md)
                * [B Table](./path/to/b_table.md)
                * [C Table](./path/to/c_table.md)
                """
            );
    }

}
