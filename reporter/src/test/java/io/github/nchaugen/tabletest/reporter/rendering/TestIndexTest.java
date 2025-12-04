package io.github.nchaugen.tabletest.reporter.rendering;

import io.github.nchaugen.tabletest.reporter.ContextLoader;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

public class TestIndexTest {

    private final Map<String, Object> context = new ContextLoader().fromYaml("""
        "type": "test_index"
        "title": "Title of the Test Class"
        "description": "A free-text description explaining what these tables are about."
        "tables":
        - "title": "A Table"
          "path": "path/to/a_table"
        - "title": "B Table"
          "path": "path/to/b_table"
        - "title": "C Table"
          "path": "path/to/c_table"
        """);

    @Test
    void supported_in_asciidoc() {
        assertThat(ASCIIDOC.renderTable(context))
            .isEqualTo("""
                = ++Title of the Test Class++
                
                A free-text description explaining what these tables are about.
                
                * xref:path/to/a_table[++A Table++]
                * xref:path/to/b_table[++B Table++]
                * xref:path/to/c_table[++C Table++]
                """
            );
    }

    @Test
    void supported_in_markdown() {
        assertThat(MARKDOWN.renderTable(context))
            .isEqualTo("""
                # Title of the Test Class
                
                A free-text description explaining what these tables are about.
                
                * [A Table](path/to/a_table)
                * [B Table](path/to/b_table)
                * [C Table](path/to/c_table)
                """
            );
    }

}
