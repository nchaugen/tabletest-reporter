package org.tabletest.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TableTestReporterCliTest {

    @TempDir
    Path tempDir;

    @Test
    void uses_custom_template_when_template_dir_provided() throws IOException {
        Path inputDir = setupInputDirectory(tempDir);
        Path templateDir = setupCustomTemplateDirectory(tempDir);
        Path outputDir = tempDir.resolve("output");

        CliResult result = runCli(
                "--input", inputDir.toString(),
                "--output", outputDir.toString(),
                "--template-dir", templateDir.toString(),
                "--format", "asciidoc");

        assertThat(result.exitCode()).isZero();
        String content = Files.readString(findGeneratedFile(outputDir, ".adoc"));
        assertThat(content).contains("CUSTOM HEADER");
        assertThat(content).contains("Custom template content");
        assertThat(content).contains("CUSTOM FOOTER");
        assertThat(content).doesNotContain("[%header,cols=");
    }

    @Test
    void uses_builtin_template_when_no_template_dir_provided() throws IOException {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("output");

        CliResult result = runCli(
                "--input", inputDir.toString(),
                "--output", outputDir.toString(),
                "--format", "asciidoc");

        assertThat(result.exitCode()).isZero();
        String content = Files.readString(findGeneratedFile(outputDir, ".adoc"));
        assertThat(content).startsWith("==");
        assertThat(content).contains("[%header,cols=");
        assertThat(content).contains("|===");
    }

    @Test
    void returns_error_when_template_dir_does_not_exist() throws IOException {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("output");

        CliResult result = runCli(
                "--input", inputDir.toString(),
                "--output", outputDir.toString(),
                "--template-dir", tempDir.resolve("nonexistent").toString(),
                "--format", "asciidoc");

        assertThat(result.exitCode()).isEqualTo(2);
    }

    @Test
    void returns_error_when_template_dir_is_not_a_directory() throws IOException {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("output");
        Path notADirectory = tempDir.resolve("file.txt");
        Files.writeString(notADirectory, "not a directory");

        CliResult result = runCli(
                "--input", inputDir.toString(),
                "--output", outputDir.toString(),
                "--template-dir", notADirectory.toString(),
                "--format", "asciidoc");

        assertThat(result.exitCode()).isEqualTo(2);
    }

    @Test
    void generates_markdown_when_format_is_markdown() throws IOException {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("output");

        CliResult result = runCli(
                "--input", inputDir.toString(),
                "--output", outputDir.toString(),
                "--format", "markdown");

        assertThat(result.exitCode()).isZero();
        String content = Files.readString(findGeneratedFile(outputDir, ".md"));
        assertThat(content).contains("## Test Table");
        assertThat(content).contains("| Column A |");
        assertThat(content).contains("---");
    }

    @Test
    void accepts_md_as_format_alias() throws IOException {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("output");

        CliResult result = runCli(
                "--input", inputDir.toString(),
                "--output", outputDir.toString(),
                "--format", "md");

        assertThat(result.exitCode()).isZero();
        assertThat(findGeneratedFile(outputDir, ".md")).exists();
    }

    @Test
    void accepts_adoc_as_format_alias() throws IOException {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("output");

        CliResult result = runCli(
                "--input", inputDir.toString(),
                "--output", outputDir.toString(),
                "--format", "adoc");

        assertThat(result.exitCode()).isZero();
        assertThat(findGeneratedFile(outputDir, ".adoc")).exists();
    }

    @Test
    void returns_error_when_format_is_invalid() throws IOException {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("output");

        CliResult result = runCli(
                "--input", inputDir.toString(),
                "--output", outputDir.toString(),
                "--format", "invalid-format");

        assertThat(result.exitCode()).isEqualTo(2);
    }

    @Test
    void returns_error_when_input_directory_does_not_exist() {
        Path outputDir = tempDir.resolve("output");

        CliResult result = runCli(
                "--input", tempDir.resolve("nonexistent").toString(),
                "--output", outputDir.toString());

        assertThat(result.exitCode()).isEqualTo(2);
    }

    @Test
    void shows_informational_message_when_no_yaml_files_found() throws IOException {
        Path inputDir = tempDir.resolve("empty-input");
        Files.createDirectories(inputDir);
        Path outputDir = tempDir.resolve("output");

        CliResult result = runCli(
                "--input", inputDir.toString(),
                "--output", outputDir.toString());

        assertThat(result.exitCode()).isZero();
        assertThat(result.stderr()).contains("No TableTest YAML files found in:");
        assertThat(result.stderr()).contains(inputDir.toString());
    }

    @Test
    void shows_success_message_with_file_count_when_files_generated() throws IOException {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("output");

        CliResult result = runCli(
                "--input", inputDir.toString(),
                "--output", outputDir.toString(),
                "--format", "asciidoc");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("Generated");
        assertThat(result.stdout()).contains("documentation file(s)");
    }

    @Test
    void list_formats_exits_successfully_and_prints_formats() {
        CliResult result = runCli("--list-formats");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("asciidoc").contains("markdown");
    }

    @Test
    void list_formats_includes_custom_formats_when_template_dir_provided() throws IOException {
        Path templateDir = tempDir.resolve("templates");
        Files.createDirectories(templateDir);
        Files.writeString(templateDir.resolve("table.html.peb"), "template");
        Files.writeString(templateDir.resolve("index.html.peb"), "template");

        CliResult result = runCli("--list-formats", "--template-dir", templateDir.toString());

        assertThat(result.stdout()).contains("html");
    }

    @Test
    void list_formats_handles_invalid_template_dir_gracefully() {
        CliResult result = runCli(
                "--list-formats",
                "--template-dir",
                tempDir.resolve("nonexistent").toString());

        assertThat(result.stdout()).contains("asciidoc");
    }

    @Test
    void help_option_shows_usage_and_exits_successfully() {
        CliResult result = runCli("--help");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("Usage:");
        assertThat(result.stdout()).contains("--input");
        assertThat(result.stdout()).contains("--output");
        assertThat(result.stdout()).contains("--format");
        assertThat(result.stdout()).contains("--template-dir");
        assertThat(result.stdout()).contains("--list-formats");
    }

    @Test
    void error_message_includes_available_formats_for_invalid_format() throws IOException {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("output");

        CliResult result = runCli(
                "--input", inputDir.toString(),
                "--output", outputDir.toString(),
                "--format", "invalid-format");

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stderr()).contains("Unknown format: invalid-format");
        assertThat(result.stderr()).contains("Available formats:");
    }

    private Path setupInputDirectory(Path parent) throws IOException {
        Path inputDir = parent.resolve("input");
        Files.createDirectories(inputDir);
        Files.writeString(inputDir.resolve("TABLETEST-test.yaml"), """
            "className": "TestClass"
            "slug": "test-class"
            "title": "Test Class"
            "tableTests":
              - "path": "TABLETEST-test-table.yaml"
                "methodName": "testTable"
                "slug": "test-table"
            """);
        Files.writeString(inputDir.resolve("TABLETEST-test-table.yaml"), """
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

    record CliResult(int exitCode, String stdout, String stderr) {}

    private CliResult runCli(String... args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        try {
            System.setOut(new PrintStream(stdout));
            System.setErr(new PrintStream(stderr));
            int exitCode = new CommandLine(new TableTestReporterCli()).execute(args);
            return new CliResult(exitCode, stdout.toString(), stderr.toString());
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private Path findGeneratedFile(Path outputDir, String extension) throws IOException {
        try (var files = Files.walk(outputDir)) {
            List<Path> matches =
                    files.filter(p -> p.toString().endsWith(extension)).toList();
            return matches.stream()
                    .filter(path -> !path.getFileName().toString().startsWith("index."))
                    .findFirst()
                    .orElseGet(() -> matches.stream().findFirst().orElseThrow());
        }
    }
}
