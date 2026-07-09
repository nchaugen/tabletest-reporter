package org.tabletest.reporter.rendering;

import org.junit.jupiter.api.Test;
import org.tabletest.reporter.ContextLoader;
import org.tabletest.reporter.TemplateEngine;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tabletest.reporter.BuiltInFormat.HTML;

/**
 * Verifies the self-contained HTML table page: nested-collection markup, role
 * classes, scroll/sticky scaffolding, pass/fail badge, failure details, and
 * self-containment (no external references).
 */
public class HtmlTableRenderingTest {

    private final TemplateEngine templateEngine = new TemplateEngine();

    private final Map<String, Object> passingContext = new ContextLoader().fromYaml("""
        title: Year selection
        description: Selecting leap years
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
              - "2004"
              roles:
              - "passed"
            - value:
                key: "2004"
              roles:
              - "expectation"
              - "passed"
          - - value: "Distinct years"
              roles:
              - "scenario"
              - "passed"
            - value: !!set
                "2000": null
              roles:
              - "passed"
            - value: []
              roles:
              - "expectation"
              - "passed"
        rowResults:
          - rowIndex: !!int "1"
            passed: !!bool "true"
            displayName: "[1] Select leap years"
          - rowIndex: !!int "2"
            passed: !!bool "true"
            displayName: "[2] Distinct years"
        """);

    @Test
    void renders_self_contained_document() {
        String rendered = templateEngine.renderTable(HTML, passingContext);

        assertThat(rendered).startsWith("<!DOCTYPE html>").contains("<title>Year selection</title>");
        assertThat(rendered).doesNotContain("http://").doesNotContain("https://");
    }

    @Test
    void renders_role_classes_on_cells() {
        String rendered = templateEngine.renderTable(HTML, passingContext);

        assertThat(rendered)
                .contains("<th class=\"cell scenario\">")
                .contains("<th class=\"cell expectation\">")
                .contains("class=\"cell scenario passed\"")
                .contains("class=\"cell expectation passed\"");
    }

    @Test
    void renders_nested_collections_with_kind_classes() {
        String rendered = templateEngine.renderTable(HTML, passingContext);

        assertThat(rendered)
                .contains("<ul class=\"coll list\">")
                .contains("<ul class=\"coll set\">")
                .contains("<dl class=\"coll map\">")
                .contains("<dt>key</dt>");
    }

    @Test
    void renders_scroll_wrapper_and_sticky_scaffolding() {
        String rendered = templateEngine.renderTable(HTML, passingContext);

        assertThat(rendered)
                .contains("class=\"table-wrap\"")
                .contains(".table-wrap { overflow-x: auto")
                .contains("position: sticky");
    }

    @Test
    void anchors_first_column_only_when_it_is_a_scenario_column() {
        String withScenario = templateEngine.renderTable(HTML, passingContext);
        assertThat(withScenario).contains("<table class=\"anchored\">");

        Map<String, Object> noScenario = new ContextLoader().fromYaml("""
            title: Addition
            headers:
              - value: a
              - value: b
              - value: sum
                roles: ["expectation"]
            rows:
              - - value: "2"
                - value: "3"
                - value: "5"
                  roles: ["expectation"]
            """);
        String rendered = templateEngine.renderTable(HTML, noScenario);
        assertThat(rendered).contains("<table>").doesNotContain("class=\"anchored\"");
    }

    @Test
    void renders_null_as_empty_cell_and_empty_collections_as_literal_tokens() {
        Map<String, Object> empties = new ContextLoader().fromYaml("""
            title: Empties
            headers:
              - value: Scenario
                roles: ["scenario"]
              - value: List
              - value: Set
              - value: Map
              - value: Nothing
            rows:
              - - value: "all empty"
                  roles: ["scenario"]
                - value: []
                - value: !!set {}
                - value: {}
                - value: !!null "null"
            """);

        String rendered = templateEngine.renderTable(HTML, empties);

        assertThat(rendered)
                .contains("<span class=\"empty\">[]</span>")
                .contains("<span class=\"empty\">{}</span>")
                .contains("<span class=\"empty\">[:]</span>")
                .contains("<td class=\"cell\"></td>")
                .doesNotContain("&#8709;");
    }

    @Test
    void shows_passing_verdict_with_count_when_no_failures() {
        String rendered = templateEngine.renderTable(HTML, passingContext);

        assertThat(rendered)
                .contains("class=\"verdict pass\"")
                .contains("All <span class=\"count\">2</span> scenarios hold")
                .doesNotContain("verdict fail");
    }

    @Test
    void shows_failing_badge_and_failure_details() {
        Map<String, Object> failingContext = new ContextLoader().fromYaml("""
            title: Addition
            headers:
              - value: a
              - value: b
              - value: sum
                roles:
                - "expectation"
            rows:
              - - value: "2"
                - value: "2"
                - value: "5"
                  roles:
                  - "expectation"
                  - "failed"
            rowResults:
              - rowIndex: !!int "1"
                passed: !!bool "false"
                displayName: "[1] 2 + 2 = 5"
                errorMessage: "expected: <5> but was: <4>"
            """);

        String rendered = templateEngine.renderTable(HTML, failingContext);

        assertThat(rendered)
                .contains("class=\"verdict fail\"")
                .contains("<span class=\"count\">1</span> of 1 scenario broken")
                .contains("class=\"cell expectation failed\"")
                .contains("data-failed=\"true\"")
                .contains("<summary>[1] 2 + 2 = 5</summary>")
                .contains("expected: &lt;5&gt; but was: &lt;4&gt;");
    }
}
