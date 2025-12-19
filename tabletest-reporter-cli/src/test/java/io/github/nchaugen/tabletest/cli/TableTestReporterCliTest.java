package io.github.nchaugen.tabletest.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TableTestReporterCliTest {

    @TempDir
    Path tempDir;

    @Test
    void uses_custom_template_when_template_dir_provided() throws IOException {
        Path inputDir = setupInputDirectory(tempDir);
        Path templateDir = setupCustomTemplateDirectory(tempDir);
        Path outputDir = tempDir.resolve("output");

        int exitCode = runCli(
            "--input", inputDir.toString(),
            "--output", outputDir.toString(),
            "--template-dir", templateDir.toString(),
            "--format", "asciidoc"
        );

        assertThat(exitCode).isZero();

        Path generatedFile = findGeneratedFile(outputDir);
        String content = Files.readString(generatedFile);

        assertThat(content).contains("CUSTOM HEADER");
        assertThat(content).contains("Custom template content");
        assertThat(content).contains("CUSTOM FOOTER");
        assertThat(content).doesNotContain("[%header,cols=");
    }

    @Test
    void uses_builtin_template_when_no_template_dir_provided() throws IOException {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("output");

        int exitCode = runCli(
            "--input", inputDir.toString(),
            "--output", outputDir.toString(),
            "--format", "asciidoc"
        );

        assertThat(exitCode).isZero();

        Path generatedFile = findGeneratedFile(outputDir);
        String content = Files.readString(generatedFile);

        assertThat(content).startsWith("==");
        assertThat(content).contains("[%header,cols=");
        assertThat(content).contains("|===");
    }

    @Test
    void returns_error_when_template_dir_does_not_exist() {
        Path inputDir = tempDir.resolve("input");
        Path outputDir = tempDir.resolve("output");
        Path nonexistentDir = tempDir.resolve("nonexistent");

        int exitCode = runCli(
            "--input", inputDir.toString(),
            "--output", outputDir.toString(),
            "--template-dir", nonexistentDir.toString(),
            "--format", "asciidoc"
        );

        assertThat(exitCode).isEqualTo(2);
    }

    @Test
    void returns_error_when_template_dir_is_not_a_directory() throws IOException {
        Path inputDir = tempDir.resolve("input");
        Path outputDir = tempDir.resolve("output");
        Path notADirectory = tempDir.resolve("file.txt");
        Files.writeString(notADirectory, "not a directory");

        int exitCode = runCli(
            "--input", inputDir.toString(),
            "--output", outputDir.toString(),
            "--template-dir", notADirectory.toString(),
            "--format", "asciidoc"
        );

        assertThat(exitCode).isEqualTo(2);
    }

    private Path setupInputDirectory(Path parent) throws IOException {
        Path inputDir = parent.resolve("input");
        Files.createDirectories(inputDir);
        Files.writeString(inputDir.resolve("test.yaml"), """
            title: Test Table
            headers:
              - value: Column A
            rows: []
            """);
        return inputDir;
    }

    private Path setupCustomTemplateDirectory(Path parent) throws IOException {
        Path templateDir = parent.resolve("templates");
        Files.createDirectories(templateDir);
        Files.writeString(templateDir.resolve("table.adoc.peb"), """
            = CUSTOM HEADER
            == {{ title }}
            Custom template content
            = CUSTOM FOOTER
            """);
        return templateDir;
    }

    private int runCli(String... args) {
        return new CommandLine(new TableTestReporterCli()).execute(args);
    }

    private Path findGeneratedFile(Path outputDir) throws IOException {
        try (var files = Files.list(outputDir)) {
            return files
                .filter(p -> p.toString().endsWith(".adoc"))
                .findFirst()
                .orElseThrow();
        }
    }
}
