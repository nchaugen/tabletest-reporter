package io.github.nchaugen.tabletest.reporter.rendering;

import io.github.nchaugen.tabletest.reporter.ContextLoader;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static io.github.nchaugen.tabletest.reporter.rendering.AsciiDocValidator.assertValidAsciiDoc;
import static io.github.nchaugen.tabletest.reporter.rendering.MarkdownValidator.assertValidMarkdown;
import static org.assertj.core.api.Assertions.assertThat;

public class TableWithExplicitWhitespaceTest {

    private final Map<String, Object> context = new ContextLoader().fromYaml("""
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
        """);

    @Test
    void supported_in_asciidoc() {
        String rendered = ASCIIDOC.renderTable(context);

        assertThat(rendered)
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
        assertValidAsciiDoc(rendered);
    }

    @Test
    void supported_in_markdown() {
        String rendered = MARKDOWN.renderTable(context);

        assertThat(rendered)
            .isEqualTo("""
                ## Explicit whitespace

                | a | b | c d | &#x2423;e&#x2423; | f | g |
                | --- | --- | --- | --- | --- | --- |
                |  | "" | &#x2423;&#x2423;&#x2423; | a bc&#x2423;&#x2423;def | &#x21E5; | &#x21E5;&#x2423; |
                """
            );
        assertValidMarkdown(rendered);
    }

}
