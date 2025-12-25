/*
 * Copyright 2025-present Nils Christian Haugen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.nchaugen.tabletest.maven;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportMojoTest {

    @TempDir
    Path tempDir;

    @Test
    void execute_generates_output_from_minimal_yaml_asciidoc_default() throws Exception {
        // Arrange: create input/output directories and minimal YAML structure
        Path inDir = tempDir.resolve("in");
        Path testClassDir = inDir.resolve("org.example.CalendarTest");
        Path tableDir = testClassDir.resolve("leapYearRules(java.time.Year, boolean)");
        Files.createDirectories(tableDir);

        Files.writeString(testClassDir.resolve("TABLETEST-calendar-calculations.yaml"), """
            "title": "Calendar"
            "description": "Various rules for calendar calculations."
            """);

        Files.writeString(tableDir.resolve("TABLETEST-leap-year-rules.yaml"), """
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
            """);

        Path outDir = tempDir.resolve("out");

        ReportMojo mojo = new ReportMojo();
        setField(mojo, "format", "asciidoc");
        setField(mojo, "inputDirectory", inDir.toFile());
        setField(mojo, "outputDirectory", outDir.toFile());

        // Act
        mojo.execute();

        // Assert
        assertThat(outDir.resolve("index.adoc")).exists();
        assertThat(outDir.resolve("calendar-calculations")).isDirectory();
        assertThat(outDir.resolve("calendar-calculations").resolve("leap-year-rules.adoc"))
                .exists();
    }

    @Test
    void execute_fails_when_input_directory_missing() {
        ReportMojo mojo = new ReportMojo();
        setField(mojo, "format", "markdown");
        setField(mojo, "inputDirectory", new File(tempDir.resolve("missing").toString()));
        setField(mojo, "outputDirectory", tempDir.resolve("out").toFile());

        assertThatThrownBy(mojo::execute)
                .isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("Input directory");
    }

    @Test
    void execute_uses_custom_template_when_template_directory_provided() throws Exception {
        Path inputDir = setupInputDirectory(tempDir);
        Path templateDir = setupCustomTemplateDirectory(tempDir);
        Path outputDir = tempDir.resolve("out");

        ReportMojo mojo = new ReportMojo();
        setField(mojo, "format", "asciidoc");
        setField(mojo, "inputDirectory", inputDir.toFile());
        setField(mojo, "outputDirectory", outputDir.toFile());
        setField(mojo, "templateDirectory", templateDir.toFile());

        mojo.execute();

        Path generatedFile = findGeneratedFile(outputDir, ".adoc");
        String content = Files.readString(generatedFile);

        assertThat(content).contains("CUSTOM HEADER");
        assertThat(content).contains("Custom template content");
        assertThat(content).contains("CUSTOM FOOTER");
        assertThat(content).doesNotContain("[%header,cols=");
    }

    @Test
    void execute_fails_when_template_directory_does_not_exist() throws IOException {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("out");
        Path nonexistentDir = tempDir.resolve("nonexistent");

        ReportMojo mojo = new ReportMojo();
        setField(mojo, "format", "asciidoc");
        setField(mojo, "inputDirectory", inputDir.toFile());
        setField(mojo, "outputDirectory", outputDir.toFile());
        setField(mojo, "templateDirectory", nonexistentDir.toFile());

        assertThatThrownBy(mojo::execute)
                .isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("Template directory does not exist");
    }

    @Test
    void execute_fails_when_template_directory_is_not_a_directory() throws IOException {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("out");
        Path notADirectory = tempDir.resolve("file.txt");
        Files.writeString(notADirectory, "not a directory");

        ReportMojo mojo = new ReportMojo();
        setField(mojo, "format", "asciidoc");
        setField(mojo, "inputDirectory", inputDir.toFile());
        setField(mojo, "outputDirectory", outputDir.toFile());
        setField(mojo, "templateDirectory", notADirectory.toFile());

        assertThatThrownBy(mojo::execute)
                .isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("Template path is not a directory");
    }

    @Test
    void execute_generates_markdown_when_format_is_markdown() throws Exception {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("out");

        ReportMojo mojo = new ReportMojo();
        setField(mojo, "format", "markdown");
        setField(mojo, "inputDirectory", inputDir.toFile());
        setField(mojo, "outputDirectory", outputDir.toFile());

        mojo.execute();

        Path generatedFile = findGeneratedFile(outputDir, ".md");
        String content = Files.readString(generatedFile);

        assertThat(content).contains("## Test Table");
        assertThat(content).contains("| Column A |");
        assertThat(content).contains("---");
    }

    @Test
    void execute_accepts_md_as_format_alias() throws Exception {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("out");

        ReportMojo mojo = new ReportMojo();
        setField(mojo, "format", "md");
        setField(mojo, "inputDirectory", inputDir.toFile());
        setField(mojo, "outputDirectory", outputDir.toFile());

        mojo.execute();

        assertThat(findGeneratedFile(outputDir, ".md")).exists();
    }

    @Test
    void execute_accepts_adoc_as_format_alias() throws Exception {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("out");

        ReportMojo mojo = new ReportMojo();
        setField(mojo, "format", "adoc");
        setField(mojo, "inputDirectory", inputDir.toFile());
        setField(mojo, "outputDirectory", outputDir.toFile());

        mojo.execute();

        assertThat(findGeneratedFile(outputDir, ".adoc")).exists();
    }

    @Test
    void execute_fails_when_format_is_invalid() throws IOException {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("out");

        ReportMojo mojo = new ReportMojo();
        setField(mojo, "format", "invalid-format");
        setField(mojo, "inputDirectory", inputDir.toFile());
        setField(mojo, "outputDirectory", outputDir.toFile());

        assertThatThrownBy(mojo::execute)
                .isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("Unknown format");
    }

    @Test
    void execute_uses_builtin_template_when_no_template_directory_provided() throws Exception {
        Path inputDir = setupInputDirectory(tempDir);
        Path outputDir = tempDir.resolve("out");

        ReportMojo mojo = new ReportMojo();
        setField(mojo, "format", "asciidoc");
        setField(mojo, "inputDirectory", inputDir.toFile());
        setField(mojo, "outputDirectory", outputDir.toFile());

        mojo.execute();

        Path generatedFile = findGeneratedFile(outputDir, ".adoc");
        String content = Files.readString(generatedFile);

        assertThat(content).startsWith("==");
        assertThat(content).contains("[%header,cols=");
        assertThat(content).contains("|===");
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

    private Path findGeneratedFile(Path outputDir, String extension) throws IOException {
        try (var files = Files.list(outputDir)) {
            return files.filter(p -> p.toString().endsWith(extension))
                    .findFirst()
                    .orElseThrow();
        }
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
