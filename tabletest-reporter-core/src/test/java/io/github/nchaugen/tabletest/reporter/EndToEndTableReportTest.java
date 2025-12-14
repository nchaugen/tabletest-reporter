package io.github.nchaugen.tabletest.reporter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.github.nchaugen.tabletest.reporter.ReportFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.ReportFormat.MARKDOWN;
import static org.assertj.core.api.Assertions.assertThat;

public class EndToEndTableReportTest {

    private static @TempDir Path tempDir;
    private static Path inDir;
    private static Path outDir;

    @BeforeAll
    static void setUp() throws IOException {
        inDir = Files.createDirectory(tempDir.resolve("in"));
        Path testClassDir = Files.createDirectory(inDir.resolve("org.example.CalendarTest"));
        Files.writeString(testClassDir.resolve("TABLETEST-calendar-calculations.yaml"), TEST_CLASS_CONTEXT_YAML);
        Path tableDir = Files.createDirectory(testClassDir.resolve("leapYearRules(java.time.Year, boolean)"));
        Files.writeString(tableDir.resolve("TABLETEST-leap-year-rules.yaml"), TABLE_CONTEXT_YAML);
        outDir = Files.createDirectory(tempDir.resolve("out"));
    }

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
        Path testDir = Files.createDirectory(tempDir.resolve("with-failures"));
        Path inDirWithFailures = Files.createDirectory(testDir.resolve("in"));
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
        Path outDirWithFailures = Files.createDirectory(testDir.resolve("out"));

        new TableTestReporter().report(ASCIIDOC, inDirWithFailures, outDirWithFailures);

        assertThat(Files.readString(outDirWithFailures.resolve("math-test/addition.adoc")))
            .contains("=== Failed Rows")
            .contains("*[2] 2 + 2 = 5*")
            .contains("expected: <5> but was: <4>");
    }

    @Test
    void should_render_failed_rows_section_in_markdown() throws IOException {
        Path testDir = Files.createDirectory(tempDir.resolve("with-failures-md"));
        Path inDirWithFailures = Files.createDirectory(testDir.resolve("in"));
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
        Path outDirWithFailures = Files.createDirectory(testDir.resolve("out"));

        new TableTestReporter().report(MARKDOWN, inDirWithFailures, outDirWithFailures);

        assertThat(Files.readString(outDirWithFailures.resolve("math-test/addition.md")))
            .contains("### Failed Rows")
            .contains("**[2] 2 + 2 = 5**")
            .contains("expected: <5> but was: <4>");
    }

}
