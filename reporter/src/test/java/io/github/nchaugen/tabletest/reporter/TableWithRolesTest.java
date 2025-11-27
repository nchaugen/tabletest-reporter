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

public class TableWithRolesTest {

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
    void should_add_roles_for_asciidoc() throws IOException {
        new TableTestReporter().report(ASCIIDOC, inDir, outDir);

        assertThat(Files.readAllLines(outDir.resolve("table.adoc")))
            .containsExactly(
                "== ++Leap Year Rules with Single Example++",
                "",
                "The leap year rules should be well-known.",
                "",
                "[%header,cols=\"1,1,1\"]",
                "|===",
                "|[.scenario]#++Scenario++#",
                "|++Year++",
                "|[.expectation]#++Is Leap Year?++#",
                "",
                "a|[.scenario]#++Not divisible by 4++#",
                "a|++2001++",
                "a|[.expectation]#++No++#",
                "",
                "a|[.scenario]#++Divisible by 4++#",
                "a|++2004++",
                "a|[.expectation]#++Yes++#",
                "",
                "|==="
            );

    }

    @Test
    void should_ignore_roles_for_markdown() throws IOException {
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
