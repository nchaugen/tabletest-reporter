package io.github.nchaugen.tabletest.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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

        Path generatedFile = findGeneratedFile(outputDir, ".adoc");
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

        Path generatedFile = findGeneratedFile(outputDir, ".adoc");
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

    @Test
    void generates_markdown_when_format_is_markdown() throws IOException {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("output");

        int exitCode = runCli(
            "--input", inputDir.toString(),
            "--output", outputDir.toString(),
            "--format", "markdown"
        );

        assertThat(exitCode).isZero();

        Path generatedFile = findGeneratedFile(outputDir, ".md");
        String content = Files.readString(generatedFile);

        assertThat(content).contains("## Test Table");
        assertThat(content).contains("| Column A |");
        assertThat(content).contains("---");
    }

    @Test
    void accepts_md_as_format_alias() throws IOException {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("output");

        int exitCode = runCli(
            "--input", inputDir.toString(),
            "--output", outputDir.toString(),
            "--format", "md"
        );

        assertThat(exitCode).isZero();
        assertThat(findGeneratedFile(outputDir, ".md")).exists();
    }

    @Test
    void accepts_adoc_as_format_alias() throws IOException {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("output");

        int exitCode = runCli(
            "--input", inputDir.toString(),
            "--output", outputDir.toString(),
            "--format", "adoc"
        );

        assertThat(exitCode).isZero();
        assertThat(findGeneratedFile(outputDir, ".adoc")).exists();
    }

    @Test
    void returns_error_when_format_is_invalid() throws IOException {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("output");

        int exitCode = runCli(
            "--input", inputDir.toString(),
            "--output", outputDir.toString(),
            "--format", "invalid-format"
        );

        assertThat(exitCode).isEqualTo(2);
    }

    @Test
    void returns_error_when_input_directory_does_not_exist() {
        Path nonexistentInput = tempDir.resolve("nonexistent");
        Path outputDir = tempDir.resolve("output");

        int exitCode = runCli(
            "--input", nonexistentInput.toString(),
            "--output", outputDir.toString()
        );

        assertThat(exitCode).isEqualTo(2);
    }

    @Test
    void list_formats_exits_successfully_and_prints_formats() {
        String output = captureOutput("--list-formats");
        int exitCode = runCli("--list-formats");

        assertThat(exitCode).isZero();
        assertThat(output).contains("asciidoc").contains("markdown");
    }

    @Test
    void list_formats_includes_custom_formats_when_template_dir_provided() throws IOException {
        Path templateDir = tempDir.resolve("templates");
        Files.createDirectories(templateDir);
        Files.writeString(templateDir.resolve("table.html.peb"), "template");
        Files.writeString(templateDir.resolve("index.html.peb"), "template");

        String output = captureOutput("--list-formats", "--template-dir", templateDir.toString());

        assertThat(output).contains("html");
    }

    @Test
    void list_formats_handles_invalid_template_dir_gracefully() {
        Path nonexistentDir = tempDir.resolve("nonexistent");

        String output = captureOutput("--list-formats", "--template-dir", nonexistentDir.toString());

        assertThat(output).contains("asciidoc");
    }

    private Path setupInputDirectory(Path parent) throws IOException {
        Path inputDir = parent.resolve("input");
        Files.createDirectories(inputDir);
        Files.writeString(inputDir.resolve("TABLETEST-test.yaml"), """
            "title": "Test Table"
            "headers":
            - "value": "Column A"
            "rows": []
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

    private String captureOutput(String... args) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(outputStream));
            new CommandLine(new TableTestReporterCli()).execute(args);
            return outputStream.toString();
        } finally {
            System.setOut(originalOut);
        }
    }

    private Path findGeneratedFile(Path outputDir, String extension) throws IOException {
        try (var files = Files.list(outputDir)) {
            return files
                .filter(p -> p.toString().endsWith(extension))
                .findFirst()
                .orElseThrow();
        }
    }
}
