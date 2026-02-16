package org.tabletest.reporter.rendering;

import org.junit.jupiter.api.Test;
import org.tabletest.reporter.ContextLoader;
import org.tabletest.reporter.TemplateEngine;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tabletest.reporter.BuiltInFormat.ASCIIDOC;
import static org.tabletest.reporter.BuiltInFormat.MARKDOWN;
import static org.tabletest.reporter.rendering.AsciiDocValidator.assertValidAsciiDoc;
import static org.tabletest.reporter.rendering.MarkdownValidator.assertValidMarkdown;

public class TableWithExplicitWhitespaceTest {

    private final TemplateEngine templateEngine = new TemplateEngine();

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
        String rendered = templateEngine.renderTable(ASCIIDOC, context);

        assertThat(rendered).isEqualTo("""
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
                """);
        assertValidAsciiDoc(rendered);
    }

    @Test
    void supported_in_markdown() {
        String rendered = templateEngine.renderTable(MARKDOWN, context);

        assertThat(rendered).isEqualTo("""
                ## Explicit whitespace

                | a | b | c d | &#x2423;e&#x2423; | f | g |
                | --- | --- | --- | --- | --- | --- |
                |  | "" | &#x2423;&#x2423;&#x2423; | a bc&#x2423;&#x2423;def | &#x21E5; | &#x21E5;&#x2423; |
                """);
        assertValidMarkdown(rendered);
    }
}
