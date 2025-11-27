package io.github.nchaugen.tabletest.reporter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class EndToEndTest {

    private Path inDir;
    private Path outDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        inDir = Files.createDirectory(tempDir.resolve("in"));
        outDir = Files.createDirectory(tempDir.resolve("out"));
        Files.writeString(inDir.resolve("tabletest.yaml"), TABLE_TEST_YAML);
    }

    public static final String TABLE_TEST_YAML = """
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
    void shouldProcessTableTestResultFileAndProduceAsciiDoc() throws IOException {
        new TableTestReporter().report(OutputFormat.ASCIIDOC, inDir, outDir);

        assertThat(Files.readAllLines(outDir.resolve("tabletest.adoc")))
            .containsExactly(
                "== +Leap Year Rules with Single Example+",
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
    void shouldProcessTableTestResultFileAndProduceMarkdown() throws IOException {
        new TableTestReporter().report(OutputFormat.MARKDOWN, inDir, outDir);

        assertThat(Files.readAllLines(outDir.resolve("tabletest.md")))
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
