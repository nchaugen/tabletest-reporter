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
        Files.writeString(testClassDir.resolve("Calendar Calculations.yaml"), TEST_CLASS_CONTEXT_YAML);
        Path tableDir = Files.createDirectory(testClassDir.resolve("leapYearRules(java.time.Year, boolean)"));
        Files.writeString(tableDir.resolve("Leap Year Rules.yaml"), TABLE_CONTEXT_YAML);
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
                "* xref:./calendar-calculations[++Calendar Calculations++]"
            );

        assertThat(Files.readAllLines(outDir.resolve("calendar-calculations").resolve("index.adoc")))
            .containsExactly(
                "= ++Calendar++",
                "",
                "Various rules for calendar calculations.",
                "",
                "* xref:./leap-year-rules.adoc[++Leap Year Rules++]"
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
                "* [Calendar Calculations](./calendar-calculations)"
            );

        assertThat(Files.readAllLines(outDir.resolve("calendar-calculations").resolve("index.md")))
            .containsExactly(
                "# Calendar",
                "",
                "Various rules for calendar calculations.",
                "",
                "* [Leap Year Rules](./leap-year-rules.md)"
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

}
