package io.github.nchaugen.tabletest.reporter.rendering;

import io.github.nchaugen.tabletest.reporter.ContextLoader;
import io.github.nchaugen.tabletest.reporter.TemplateEngine;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static io.github.nchaugen.tabletest.reporter.rendering.AsciiDocValidator.assertValidAsciiDoc;
import static io.github.nchaugen.tabletest.reporter.rendering.MarkdownValidator.assertValidMarkdown;
import static org.assertj.core.api.Assertions.assertThat;

public class TableWithRolesTest {

    private final TemplateEngine templateEngine = new TemplateEngine();

    private final Map<String, Object> context = new ContextLoader().fromYaml("""
        title: Year selection
        headers:
          - value: Scenario
            roles:
            - "scenario"
          - value: Candidates
          - value: Selected?
            roles:
            - "expectation"
        rows:
          - - value: "Select leap years"
              roles:
              - "scenario"
              - "passed"
            - value:
              - "2000"
              - "2001"
              - "2002"
              - "2003"
              - "2004"
              roles:
              - "passed"
            - value:
              - "2004"
              roles:
              - "expectation"
              - "passed"
          - - value: "Empty years"
              roles:
              - "scenario"
              - "passed"
            - value: !!set {}
              roles:
              - "passed"
            - value: []
              roles:
              - "expectation"
              - "passed"
          - - value: "Empty map and null"
              roles:
              - "scenario"
              - "passed"
            - value: {}
              roles:
              - "passed"
            - value: !!null "null"
              roles:
              - "expectation"
              - "passed"
        """);

    @Test
    void should_add_roles_for_asciidoc() {
        String rendered = templateEngine.renderTable(ASCIIDOC, context);

        assertThat(rendered)
            .isEqualTo("""
                == ++Year selection++

                [%header,cols="1,1,1"]
                |===
                |[.scenario]#++Scenario++#
                |++Candidates++
                |[.expectation]#++Selected?++#

                a|[.scenario.passed]#++Select leap years++#
                a|[.passed]
                * ++2000++
                * ++2001++
                * ++2002++
                * ++2003++
                * ++2004++
                a|[.expectation.passed]
                * ++2004++

                a|[.scenario.passed]#++Empty years++#
                a|[.passed]#{empty}#
                a|[.expectation.passed]#{empty}#

                a|[.scenario.passed]#++Empty map and null++#
                a|[.passed]#{empty}#
                a|[.expectation.passed]

                |===
                """
            );
        assertValidAsciiDoc(rendered);
    }

    @Test
    void should_ignore_roles_for_markdown() {
        String rendered = templateEngine.renderTable(MARKDOWN, context);

        assertThat(rendered)
            .isEqualTo("""
                ## Year selection

                | Scenario | Candidates | Selected? |
                | --- | --- | --- |
                | Select leap years | [2000, 2001, 2002, 2003, 2004] | [2004] |
                | Empty years | {} | [] |
                | Empty map and null | [:] |  |
                """
            );
        assertValidMarkdown(rendered);
    }

}
