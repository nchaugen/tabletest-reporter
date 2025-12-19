package io.github.nchaugen.tabletest.reporter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

public class EndToEndTableReportTest {

    @TempDir
    Path tempDir;

    private static final String TEST_CLASS_CONTEXT_YAML = """
        "title": "Calendar"
        "description": "Various rules for calendar calculations."
        """;

    private static final String TABLE_CONTEXT_YAML = """
        "title": "Leap Year Rules with Single Example"
        "description": "The leap year rules should be well-known."
        "headers":
          - "value": "Scenario"
          - "value": "Year"
          - "value": "Is Leap Year?"
        "rows":
            - - "value": "Not divisible by 4"
              - "value": "2001"
              - "value": "No"
            - - "value": "Divisible by 4"
              - "value": "2004"
              - "value": "Yes"
        """;

    @Test
    void should_produce_asciidoc_file_from_table_context_file() throws IOException {
        Path testDir = createTestDir("asciidoc-basic");
        Path inDir = setupCalendarTestInput(testDir);
        Path outDir = Files.createDirectory(testDir.resolve("out"));

        new TableTestReporter().report(ASCIIDOC, inDir, outDir);

        assertThat(Files.readAllLines(outDir.resolve("index.adoc")))
            .containsExactly(
                "= ++example++",
                "",
                "* xref:./calendar-calculations[++Calendar++]"
            );

        assertThat(Files.readAllLines(outDir.resolve("calendar-calculations").resolve("index.adoc")))
            .containsExactly(
                "= ++Calendar++",
                "",
                "Various rules for calendar calculations.",
                "",
                "* xref:./leap-year-rules.adoc[++Leap Year Rules with Single Example++]"
            );

        assertThat(Files.readAllLines(outDir.resolve("calendar-calculations").resolve("leap-year-rules.adoc")))
            .containsExactly(
                "== ++Leap Year Rules with Single Example++",
                "",
                "The leap year rules should be well-known.",
                "",
                "[%header,cols=\"1,1,1\"]",
                "|===",
                "|++Scenario++",
                "|++Year++",
                "|++Is Leap Year?++",
                "",
                "a|++Not divisible by 4++",
                "a|++2001++",
                "a|++No++",
                "",
                "a|++Divisible by 4++",
                "a|++2004++",
                "a|++Yes++",
                "",
                "|==="
            );

    }

    @Test
    void should_produce_markdown_file_from_table_context_file() throws IOException {
        Path testDir = createTestDir("markdown-basic");
        Path inDir = setupCalendarTestInput(testDir);
        Path outDir = Files.createDirectory(testDir.resolve("out"));

        new TableTestReporter().report(MARKDOWN, inDir, outDir);

        assertThat(Files.readAllLines(outDir.resolve("index.md")))
            .containsExactly(
                "# example",
                "",
                "* [Calendar](./calendar-calculations)"
            );

        assertThat(Files.readAllLines(outDir.resolve("calendar-calculations").resolve("index.md")))
            .containsExactly(
                "# Calendar",
                "",
                "Various rules for calendar calculations.",
                "",
                "* [Leap Year Rules with Single Example](./leap-year-rules.md)"
            );

        assertThat(Files.readAllLines(outDir.resolve("calendar-calculations").resolve("leap-year-rules.md")))
            .containsExactly(
                "## Leap Year Rules with Single Example",
                "",
                "The leap year rules should be well-known.",
                "",
                "| Scenario | Year | Is Leap Year? |",
                "| --- | --- | --- |",
                "| Not divisible by 4 | 2001 | No |",
                "| Divisible by 4 | 2004 | Yes |"
            );
    }

    @Test
    void should_render_failed_rows_section_in_asciidoc() throws IOException {
        Path testDir = createTestDir("with-failures");
        Path inDirWithFailures = setupInputWithFailures(testDir);
        Path outDirWithFailures = Files.createDirectory(testDir.resolve("out"));

        new TableTestReporter().report(ASCIIDOC, inDirWithFailures, outDirWithFailures);

        assertThat(Files.readString(outDirWithFailures.resolve("math-test/addition.adoc")))
            .contains("=== Failed Rows")
            .contains("*[2] 2 + 2 = 5*")
            .contains("expected: <5> but was: <4>");
    }

    @Test
    void should_render_failed_rows_section_in_markdown() throws IOException {
        Path testDir = createTestDir("with-failures-md");
        Path inDirWithFailures = setupInputWithFailures(testDir);
        Path outDirWithFailures = Files.createDirectory(testDir.resolve("out"));

        new TableTestReporter().report(MARKDOWN, inDirWithFailures, outDirWithFailures);

        assertThat(Files.readString(outDirWithFailures.resolve("math-test/addition.md")))
            .contains("### Failed Rows")
            .contains("**[2] 2 + 2 = 5**")
            .contains("expected: <5> but was: <4>");
    }

    @Test
    void uses_child_template_with_jekyll_front_matter_markdown() throws IOException {
        Path testDir = createTestDir("jekyll-md");
        Path inDir = setupCalendarTestInput(testDir);
        Path templateDir = setupJekyllTemplateMarkdown(testDir);

        new TableTestReporter(templateDir).report(MARKDOWN, inDir, testDir.resolve("out"));

        String content = readContent(testDir, "calendar-calculations/leap-year-rules.md");
        assertThat(content)
            .startsWith("---\nlayout: default\ntitle: Leap Year Rules with Single Example---")
            .contains("## Leap Year Rules with Single Example")
            .contains("| Scenario | Year | Is Leap Year? |");
    }

    @Test
    void uses_child_template_with_front_matter_asciidoc() throws IOException {
        Path testDir = createTestDir("frontmatter-adoc");
        Path inDir = setupCalendarTestInput(testDir);
        Path templateDir = setupFrontMatterTemplateAsciiDoc(testDir);

        new TableTestReporter(templateDir).report(ASCIIDOC, inDir, testDir.resolve("out"));

        String content = readContent(testDir, "calendar-calculations/leap-year-rules.adoc");
        assertThat(content)
            .startsWith(":toc: left\n:icons: font")
            .contains("== ++Leap Year Rules with Single Example++")
            .contains("[%header,cols=\"1,1,1\"]");
    }

    @Test
    void uses_child_template_with_custom_footer() throws IOException {
        Path testDir = createTestDir("footer");
        Path inDir = setupCalendarTestInput(testDir);
        Path templateDir = setupTemplateWithFooter(testDir);

        new TableTestReporter(templateDir).report(ASCIIDOC, inDir, testDir.resolve("out"));

        String content = readContent(testDir, "calendar-calculations/leap-year-rules.adoc");
        assertThat(content)
            .endsWith("---\nGenerated by TableTest Reporter\n")
            .contains("== ++Leap Year Rules with Single Example++")
            .contains("|===");
    }

    @Test
    void uses_child_template_overriding_multiple_blocks() throws IOException {
        Path testDir = createTestDir("multiple-blocks");
        Path inDir = setupCalendarTestInput(testDir);
        Path templateDir = setupTemplateWithMultipleBlockOverrides(testDir);

        new TableTestReporter(templateDir).report(MARKDOWN, inDir, testDir.resolve("out"));

        String content = readContent(testDir, "calendar-calculations/leap-year-rules.md");
        assertThat(content)
            .startsWith("---\nauthor: Test Team\n---")
            .contains("# Leap Year Rules with Single Example - Custom")
            .contains("| Scenario | Year |")
            .endsWith("---\n_End of report_\n");
    }

    @Test
    void uses_child_template_for_index_with_front_matter() throws IOException {
        Path testDir = createTestDir("index-custom");
        Path inDir = setupCalendarTestInput(testDir);
        Path templateDir = setupIndexTemplateWithFrontMatter(testDir);

        new TableTestReporter(templateDir).report(MARKDOWN, inDir, testDir.resolve("out"));

        String indexContent = readContent(testDir, "calendar-calculations/index.md");
        assertThat(indexContent)
            .startsWith("---\nnav_order: 1\n---")
            .contains("# Calendar")
            .contains("Various rules for calendar calculations.")
            .contains("[Leap Year Rules with Single Example](./leap-year-rules.md)");
    }

    @Test
    void uses_completely_replaced_template() throws IOException {
        Path testDir = createTestDir("replaced");
        Path inDir = setupCalendarTestInput(testDir);
        Path templateDir = setupCompletelyReplacedTemplate(testDir);

        new TableTestReporter(templateDir).report(ASCIIDOC, inDir, testDir.resolve("out"));

        String content = readContent(testDir, "calendar-calculations/leap-year-rules.adoc");
        assertThat(content)
            .startsWith("= CUSTOM TABLE TEMPLATE")
            .contains("Title: Leap Year Rules with Single Example")
            .contains("Description: The leap year rules should be well-known.")
            .doesNotContain("[%header,cols=")
            .doesNotContain("|===");
    }

    @Test
    void uses_child_template_with_multiple_yaml_files() throws IOException {
        Path testDir = createTestDir("multi-yaml");
        Path inDirMulti = setupInputWithMultipleTests(testDir);
        Path templateDir = setupJekyllTemplateMarkdown(testDir);
        Path outDirMulti = Files.createDirectory(testDir.resolve("out"));

        new TableTestReporter(templateDir).report(MARKDOWN, inDirMulti, outDirMulti);

        String authContent = Files.readString(outDirMulti.resolve("auth-test/login-validation.md"));
        assertThat(authContent)
            .startsWith("---\nlayout: default\ntitle: Login Validation---")
            .contains("## Login Validation")
            .contains("| Username | Password | Expected? |");

        String orderContent = Files.readString(outDirMulti.resolve("order-test/place-order.md"));
        assertThat(orderContent)
            .startsWith("---\nlayout: default\ntitle: Place Order---")
            .contains("## Place Order")
            .contains("| Item | Quantity | Valid? |");

        String indexContent = Files.readString(outDirMulti.resolve("index.md"));
        assertThat(indexContent)
            .startsWith("---\nlayout: default\ntitle: example---")
            .contains("# example")
            .contains("[Auth Test](./auth-test)")
            .contains("[Order Test](./order-test)");
    }

    private Path setupJekyllTemplateMarkdown(Path parent) throws IOException {
        Path templateDir = parent.resolve("templates");
        Files.createDirectories(templateDir);
        Files.writeString(templateDir.resolve("custom-table.md.peb"), """
            {% extends "table.md.peb" %}
            {% block frontMatter %}---
            layout: default
            title: {{ title }}
            ---

            {% endblock %}
            """);
        Files.writeString(templateDir.resolve("custom-index.md.peb"), """
            {% extends "index.md.peb" %}
            {% block frontMatter %}---
            layout: default
            title: {{ title ? title : name }}
            ---

            {% endblock %}
            """);
        return templateDir;
    }

    private Path setupFrontMatterTemplateAsciiDoc(Path parent) throws IOException {
        Path templateDir = parent.resolve("templates");
        Files.createDirectories(templateDir);
        Files.writeString(templateDir.resolve("custom-table.adoc.peb"), """
            {% extends "table.adoc.peb" %}
            {% block frontMatter %}:toc: left
            :icons: font

            {% endblock %}
            """);
        return templateDir;
    }

    private Path setupTemplateWithFooter(Path parent) throws IOException {
        Path templateDir = parent.resolve("templates");
        Files.createDirectories(templateDir);
        Files.writeString(templateDir.resolve("custom-table.adoc.peb"), """
            {% extends "table.adoc.peb" %}
            {% block footer %}

            ---
            Generated by TableTest Reporter
            {% endblock %}
            """);
        return templateDir;
    }

    private Path setupTemplateWithMultipleBlockOverrides(Path parent) throws IOException {
        Path templateDir = parent.resolve("templates");
        Files.createDirectories(templateDir);
        Files.writeString(templateDir.resolve("custom-table.md.peb"), """
            {% extends "table.md.peb" %}
            {% block frontMatter %}---
            author: Test Team
            ---

            {% endblock %}
            {% block title %}
            # {{ title }} - Custom
            {% endblock %}
            {% block footer %}

            ---
            _End of report_
            {% endblock %}
            """);
        return templateDir;
    }

    private Path setupIndexTemplateWithFrontMatter(Path parent) throws IOException {
        Path templateDir = parent.resolve("templates");
        Files.createDirectories(templateDir);
        Files.writeString(templateDir.resolve("custom-index.md.peb"), """
            {% extends "index.md.peb" %}
            {% block frontMatter %}---
            nav_order: 1
            ---

            {% endblock %}
            """);
        return templateDir;
    }

    private Path setupCompletelyReplacedTemplate(Path parent) throws IOException {
        Path templateDir = parent.resolve("templates");
        Files.createDirectories(templateDir);
        Files.writeString(templateDir.resolve("table.adoc.peb"), """
            = CUSTOM TABLE TEMPLATE

            Title: {{ title }}

            {% if description %}
            Description: {{ description }}
            {% endif %}

            This is a completely custom template that doesn't use the built-in structure.
            """);
        return templateDir;
    }

    private Path createTestDir(String name) throws IOException {
        return Files.createDirectory(tempDir.resolve(name));
    }

    private String readContent(Path testDir, String relativePath) throws IOException {
        return Files.readString(testDir.resolve("out").resolve(relativePath));
    }

    private Path setupInputWithFailures(Path parent) throws IOException {
        Path inDirWithFailures = Files.createDirectory(parent.resolve("in"));
        Path testClassDir = Files.createDirectory(inDirWithFailures.resolve("org.example.MathTest"));
        Files.writeString(testClassDir.resolve("TABLETEST-math-test.yaml"), """
            "title": "Math Test"
            """);
        Path tableDir = Files.createDirectory(testClassDir.resolve("addition(int, int, int)"));
        Files.writeString(tableDir.resolve("TABLETEST-addition.yaml"), """
            "title": "Addition Test"
            "description": "Testing addition with some failures."
            "headers":
              - "value": "a"
              - "value": "b"
              - "value": "sum?"
            "rows":
                - - "value": "1"
                  - "value": "1"
                  - "value": "2"
                - - "value": "2"
                  - "value": "2"
                  - "value": "5"
                - - "value": "3"
                  - "value": "3"
                  - "value": "6"
            "rowResults":
              - "rowIndex": !!int "1"
                "passed": !!bool "true"
                "displayName": "[1] 1 + 1 = 2"
              - "rowIndex": !!int "2"
                "passed": !!bool "false"
                "displayName": "[2] 2 + 2 = 5"
                "errorMessage": "expected: <5> but was: <4>"
              - "rowIndex": !!int "3"
                "passed": !!bool "true"
                "displayName": "[3] 3 + 3 = 6"
            """);
        return inDirWithFailures;
    }

    private Path setupInputWithMultipleTests(Path parent) throws IOException {
        Path inDirMulti = Files.createDirectory(parent.resolve("in"));

        Path authTestDir = Files.createDirectory(inDirMulti.resolve("org.example.AuthTest"));
        Files.writeString(authTestDir.resolve("TABLETEST-auth-test.yaml"), """
            "title": "Auth Test"
            """);
        Path loginDir = Files.createDirectory(authTestDir.resolve("login(String, String, boolean)"));
        Files.writeString(loginDir.resolve("TABLETEST-login-validation.yaml"), """
            "title": "Login Validation"
            "headers":
              - "value": "Username"
              - "value": "Password"
              - "value": "Expected?"
            "rows":
                - - "value": "admin"
                  - "value": "secret"
                  - "value": "true"
                - - "value": "guest"
                  - "value": "wrong"
                  - "value": "false"
            """);

        Path orderTestDir = Files.createDirectory(inDirMulti.resolve("org.example.OrderTest"));
        Files.writeString(orderTestDir.resolve("TABLETEST-order-test.yaml"), """
            "title": "Order Test"
            """);
        Path orderDir = Files.createDirectory(orderTestDir.resolve("placeOrder(String, int, boolean)"));
        Files.writeString(orderDir.resolve("TABLETEST-place-order.yaml"), """
            "title": "Place Order"
            "headers":
              - "value": "Item"
              - "value": "Quantity"
              - "value": "Valid?"
            "rows":
                - - "value": "Book"
                  - "value": "2"
                  - "value": "true"
                - - "value": "Laptop"
                  - "value": "1"
                  - "value": "true"
            """);

        return inDirMulti;
    }

    private Path setupCalendarTestInput(Path parent) throws IOException {
        Path inDir = Files.createDirectory(parent.resolve("in"));
        Path testClassDir = Files.createDirectory(inDir.resolve("org.example.CalendarCalculations"));
        Files.writeString(testClassDir.resolve("TABLETEST-calendar-calculations.yaml"), TEST_CLASS_CONTEXT_YAML);
        Path tableDir = Files.createDirectory(testClassDir.resolve("leapYear(int)"));
        Files.writeString(tableDir.resolve("TABLETEST-leap-year-rules.yaml"), TABLE_CONTEXT_YAML);
        return inDir;
    }

}
