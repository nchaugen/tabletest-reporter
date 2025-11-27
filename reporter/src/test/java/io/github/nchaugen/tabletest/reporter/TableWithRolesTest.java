package io.github.nchaugen.tabletest.reporter;

import org.junit.jupiter.api.Test;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

public class TableWithRolesTest {

    private static final String TABLE_CONTEXT_YAML = """
        title: Leap Year Rules
        columnCount: 3
        headers:
          - value: Scenario
            role: scenario
          - value: Year
          - value: Is Leap Year?
            role: expectation
        rows:
          - - value: "Not divisible by 4"
              role: scenario
            - value: "2001"
            - value: "No"
              role: expectation
          - - value: "Divisible by 4"
              role: scenario
            - value: "2004"
            - value: "Yes"
              role: expectation
        """;

    @Test
    void should_add_roles_for_asciidoc() {
        assertThat(ASCIIDOC.renderTable(Context.fromYaml(TABLE_CONTEXT_YAML)))
            .isEqualTo("""
                == ++Leap Year Rules++
                
                [%header,cols="1,1,1"]
                |===
                |[.scenario]#++Scenario++#
                |++Year++
                |[.expectation]#++Is Leap Year?++#
                
                a|[.scenario]#++Not divisible by 4++#
                a|++2001++
                a|[.expectation]#++No++#
                
                a|[.scenario]#++Divisible by 4++#
                a|++2004++
                a|[.expectation]#++Yes++#
                
                |===
                """
            );
    }

    @Test
    void should_ignore_roles_for_markdown() {
        assertThat(MARKDOWN.renderTable(Context.fromYaml(TABLE_CONTEXT_YAML)))
            .isEqualTo("""
                ## Leap Year Rules
                
                | Scenario | Year | Is Leap Year? |
                | --- | --- | --- |
                | Not divisible by 4 | 2001 | No |
                | Divisible by 4 | 2004 | Yes |
                """
            );
    }

}
