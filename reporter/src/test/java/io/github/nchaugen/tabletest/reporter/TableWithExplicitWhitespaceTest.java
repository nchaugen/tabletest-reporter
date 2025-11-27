package io.github.nchaugen.tabletest.reporter;

import org.junit.jupiter.api.Test;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

public class TableWithExplicitWhitespaceTest {

    private static final String TABLE_CONTEXT_YAML = """
        title: Explicit whitespace
        headers:
          - value: "a"
          - value: "b"
          - value: "c d"
          - value: " e "
          - value: "f"
          - value: "g"
        rows:
          - - value:
            - value: ""
            - value: "   "
            - value: "a bc  def"
            - value: "\t"
            - value: "\t "
        """;

    @Test
    void supported_in_asciidoc() {
        assertThat(ASCIIDOC.renderTable(Context.fromYaml(TABLE_CONTEXT_YAML)))
            .isEqualTo("""
                == ++Explicit whitespace++
                
                [%header,cols="1,1,1,1,1,1"]
                |===
                |++a++
                |++b++
                |++c d++
                |&#x2423;++e++&#x2423;
                |++f++
                |++g++
                
                a|
                a|+""+
                a|&#x2423;&#x2423;&#x2423;
                a|++a bc++&#x2423;&#x2423;++def++
                a|&#x21E5;
                a|&#x21E5;&#x2423;
                
                |===
                """
            );
    }

    @Test
    void supported_in_markdown() {
        assertThat(MARKDOWN.renderTable(Context.fromYaml(TABLE_CONTEXT_YAML)))
            .isEqualTo("""
                ## Explicit whitespace
                
                | a | b | c d | &#x2423;e&#x2423; | f | g |
                | --- | --- | --- | --- | --- | --- |
                |  | "" | &#x2423;&#x2423;&#x2423; | a bc&#x2423;&#x2423;def | &#x21E5; | &#x21E5;&#x2423; |
                """
            );
    }

}
