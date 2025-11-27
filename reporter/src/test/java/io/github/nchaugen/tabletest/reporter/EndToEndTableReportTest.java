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
        outDir = Files.createDirectory(tempDir.resolve("out"));
        Files.writeString(inDir.resolve("table.yaml"), TABLE_CONTEXT_YAML);
    }

    private static final String TABLE_CONTEXT_YAML = """
        title: Leap Year Rules with Single Example
        
        description: |
            The leap year rules should be well-known.
        
        headers:
          - value: Scenario
          - value: Year
          - value: Is Leap Year?
        rows:
            - - value: "Not divisible by 4"
              - value: "2001"
              - value: "No"
            - - value: "Divisible by 4"
              - value: "2004"
              - value: "Yes"
        """;

    @Test
    void should_produce_asciidoc_file_from_table_context_file() throws IOException {
        new TableTestReporter().report(ASCIIDOC, inDir, outDir);

        assertThat(Files.readAllLines(outDir.resolve("table.adoc")))
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

        assertThat(Files.readAllLines(outDir.resolve("table.md")))
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
