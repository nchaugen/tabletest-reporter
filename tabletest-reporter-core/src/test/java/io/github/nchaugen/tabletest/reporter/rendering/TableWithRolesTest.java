package io.github.nchaugen.tabletest.reporter.rendering;

import io.github.nchaugen.tabletest.reporter.ContextLoader;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

public class TableWithRolesTest {

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
            - "passed"
        rows:
          - - value: "Select leap years"
              roles:
              - "scenario"
            - value:
              - "2000"
              - "2001"
              - "2002"
              - "2003"
              - "2004"
            - value:
              - "2004"
              roles:
              - "expectation"
              - "passed"
        """);

    @Test
    void should_add_roles_for_asciidoc() {
        assertThat(ASCIIDOC.renderTable(context))
            .isEqualTo("""
                == ++Year selection++
                
                [%header,cols="1,1,1"]
                |===
                |[.scenario]#++Scenario++#
                |++Candidates++
                |[.expectation.passed]#++Selected?++#
                
                a|[.scenario]#++Select leap years++#
                a|
                * ++2000++
                * ++2001++
                * ++2002++
                * ++2003++
                * ++2004++
                a|[.expectation.passed]
                * ++2004++
                
                |===
                """
            );
    }

    @Test
    void should_ignore_roles_for_markdown() {
        assertThat(MARKDOWN.renderTable(context))
            .isEqualTo("""
                ## Year selection
                
                | Scenario | Candidates | Selected? |
                | --- | --- | --- |
                | Select leap years | [2000, 2001, 2002, 2003, 2004] | [2004] |
                """
            );
    }

}
