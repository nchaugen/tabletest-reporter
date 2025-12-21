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
package io.github.nchaugen.tabletest.gradle;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TableTestReporterPluginTest {

    @TempDir
    Path projectDir;

    private Project project;

    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder()
            .withProjectDir(projectDir.toFile())
            .build();

        project.getPluginManager().apply(TableTestReporterPlugin.class);
    }

    @Test
    void reportTask_generates_output_from_minimal_yaml() throws IOException {
        // Arrange: create minimal input structure under build/junit-jupiter
        Path buildDir = project.getLayout().getBuildDirectory().get().getAsFile().toPath();
        Path inputRoot = buildDir.resolve("junit-jupiter");
        Path testClassDir = inputRoot.resolve("org.example.CalendarTest");
        Path tableDir = testClassDir.resolve("leapYearRules(java.time.Year, boolean)");
        Files.createDirectories(tableDir);

        Files.writeString(testClassDir.resolve("TABLETEST-calendar-test.yaml"), """
            "title": "Calendar"
            "description": "Various rules for calendar calculations."
            """
        );

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
            """
        );

        // Act: run the task directly
        ReportTableTestsTask task = (ReportTableTestsTask) project.getTasks().getByName("reportTableTests");
        task.run();

        // Assert: default AsciiDoc outputs exist under build/generated-docs/tabletest
        Path outRoot = buildDir.resolve("generated-docs").resolve("tabletest");
        assertThat(outRoot.resolve("index.adoc")).exists();
        assertThat(outRoot.resolve("calendar-test")).isDirectory();
        assertThat(outRoot.resolve("calendar-test").resolve("leap-year-rules.adoc")).exists();
    }

    @Test
    void reportTask_uses_custom_template_when_template_dir_provided() throws IOException {
        Path buildDir = project.getLayout().getBuildDirectory().get().getAsFile().toPath();
        Path inputRoot = setupInputDirectory(buildDir);
        Path templateDir = setupCustomTemplateDirectory(projectDir);

        TableTestReporterExtension ext = project.getExtensions().getByType(TableTestReporterExtension.class);
        ext.getTemplateDir().set(templateDir.toFile());

        ReportTableTestsTask task = (ReportTableTestsTask) project.getTasks().getByName("reportTableTests");
        task.run();

        Path generatedFile = findGeneratedFile(buildDir.resolve("generated-docs").resolve("tabletest"), ".adoc");
        String content = Files.readString(generatedFile);

        assertThat(content).contains("CUSTOM HEADER");
        assertThat(content).contains("Custom template content");
        assertThat(content).contains("CUSTOM FOOTER");
        assertThat(content).doesNotContain("[%header,cols=");
    }

    @Test
    void reportTask_fails_when_template_directory_does_not_exist() throws IOException {
        Path buildDir = project.getLayout().getBuildDirectory().get().getAsFile().toPath();
        setupInputDirectory(buildDir);
        Path nonexistentDir = projectDir.resolve("nonexistent");

        TableTestReporterExtension ext = project.getExtensions().getByType(TableTestReporterExtension.class);
        ext.getTemplateDir().set(nonexistentDir.toFile());

        ReportTableTestsTask task = (ReportTableTestsTask) project.getTasks().getByName("reportTableTests");

        assertThatThrownBy(task::run)
            .hasMessageContaining("Template directory does not exist");
    }

    @Test
    void reportTask_fails_when_template_directory_is_not_a_directory() throws IOException {
        Path buildDir = project.getLayout().getBuildDirectory().get().getAsFile().toPath();
        setupInputDirectory(buildDir);
        Path notADirectory = projectDir.resolve("file.txt");
        Files.writeString(notADirectory, "not a directory");

        TableTestReporterExtension ext = project.getExtensions().getByType(TableTestReporterExtension.class);
        ext.getTemplateDir().set(notADirectory.toFile());

        ReportTableTestsTask task = (ReportTableTestsTask) project.getTasks().getByName("reportTableTests");

        assertThatThrownBy(task::run)
            .hasMessageContaining("Template path is not a directory");
    }

    @Test
    void reportTask_generates_markdown_when_format_is_markdown() throws IOException {
        Path buildDir = project.getLayout().getBuildDirectory().get().getAsFile().toPath();
        setupInputDirectory(buildDir);

        TableTestReporterExtension ext = project.getExtensions().getByType(TableTestReporterExtension.class);
        ext.getFormat().set("markdown");

        ReportTableTestsTask task = (ReportTableTestsTask) project.getTasks().getByName("reportTableTests");
        task.run();

        Path generatedFile = findGeneratedFile(buildDir.resolve("generated-docs").resolve("tabletest"), ".md");
        String content = Files.readString(generatedFile);

        assertThat(content).contains("## Test Table");
        assertThat(content).contains("| Column A |");
        assertThat(content).contains("---");
    }

    @Test
    void reportTask_accepts_md_as_format_alias() throws IOException {
        Path buildDir = project.getLayout().getBuildDirectory().get().getAsFile().toPath();
        setupInputDirectory(buildDir);

        TableTestReporterExtension ext = project.getExtensions().getByType(TableTestReporterExtension.class);
        ext.getFormat().set("md");

        ReportTableTestsTask task = (ReportTableTestsTask) project.getTasks().getByName("reportTableTests");
        task.run();

        assertThat(findGeneratedFile(buildDir.resolve("generated-docs").resolve("tabletest"), ".md")).exists();
    }

    @Test
    void reportTask_accepts_adoc_as_format_alias() throws IOException {
        Path buildDir = project.getLayout().getBuildDirectory().get().getAsFile().toPath();
        setupInputDirectory(buildDir);

        TableTestReporterExtension ext = project.getExtensions().getByType(TableTestReporterExtension.class);
        ext.getFormat().set("adoc");

        ReportTableTestsTask task = (ReportTableTestsTask) project.getTasks().getByName("reportTableTests");
        task.run();

        assertThat(findGeneratedFile(buildDir.resolve("generated-docs").resolve("tabletest"), ".adoc")).exists();
    }

    @Test
    void reportTask_fails_when_format_is_invalid() throws IOException {
        Path buildDir = project.getLayout().getBuildDirectory().get().getAsFile().toPath();
        setupInputDirectory(buildDir);

        TableTestReporterExtension ext = project.getExtensions().getByType(TableTestReporterExtension.class);
        ext.getFormat().set("invalid-format");

        ReportTableTestsTask task = (ReportTableTestsTask) project.getTasks().getByName("reportTableTests");

        assertThatThrownBy(task::run)
            .hasMessageContaining("Unknown format");
    }

    @Test
    void reportTask_uses_builtin_template_when_no_template_dir_provided() throws IOException {
        Path buildDir = project.getLayout().getBuildDirectory().get().getAsFile().toPath();
        setupInputDirectory(buildDir);

        ReportTableTestsTask task = (ReportTableTestsTask) project.getTasks().getByName("reportTableTests");
        task.run();

        Path generatedFile = findGeneratedFile(buildDir.resolve("generated-docs").resolve("tabletest"), ".adoc");
        String content = Files.readString(generatedFile);

        assertThat(content).startsWith("==");
        assertThat(content).contains("[%header,cols=");
        assertThat(content).contains("|===");
    }

    @Test
    void listFormatsTask_is_registered() {
        ListFormatsTask task = (ListFormatsTask) project.getTasks().getByName("listTableTestReportFormats");
        assertThat(task).isNotNull();
    }

    @Test
    void listFormatsTask_executes_successfully() {
        ListFormatsTask task = (ListFormatsTask) project.getTasks().getByName("listTableTestReportFormats");
        task.run();
    }

    @Test
    void listFormatsTask_executes_with_custom_template_dir() throws IOException {
        Path templateDir = setupCustomTemplateDirectory(projectDir);

        TableTestReporterExtension ext = project.getExtensions().getByType(TableTestReporterExtension.class);
        ext.getTemplateDir().set(templateDir.toFile());

        ListFormatsTask task = (ListFormatsTask) project.getTasks().getByName("listTableTestReportFormats");
        task.run();
    }

    @Test
    void listFormatsTask_handles_invalid_template_dir_gracefully() {
        Path nonexistentDir = projectDir.resolve("nonexistent");

        TableTestReporterExtension ext = project.getExtensions().getByType(TableTestReporterExtension.class);
        ext.getTemplateDir().set(nonexistentDir.toFile());

        ListFormatsTask task = (ListFormatsTask) project.getTasks().getByName("listTableTestReportFormats");
        task.run();
    }

    @Test
    void listFormatsTask_handles_template_dir_that_is_file() throws IOException {
        Path notADirectory = projectDir.resolve("file.txt");
        Files.writeString(notADirectory, "not a directory");

        TableTestReporterExtension ext = project.getExtensions().getByType(TableTestReporterExtension.class);
        ext.getTemplateDir().set(notADirectory.toFile());

        ListFormatsTask task = (ListFormatsTask) project.getTasks().getByName("listTableTestReportFormats");
        task.run();
    }

    private Path setupInputDirectory(Path buildDir) throws IOException {
        Path inputRoot = buildDir.resolve("junit-jupiter");
        Path testClassDir = inputRoot.resolve("org.example.CalendarTest");
        Files.createDirectories(testClassDir);

        Files.writeString(testClassDir.resolve("TABLETEST-test.yaml"), """
            "title": "Test Table"
            "headers":
            - "value": "Column A"
            "rows": []
            """);
        return inputRoot;
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
            return files
                .filter(p -> p.toString().endsWith(extension))
                .findFirst()
                .orElseThrow();
        }
    }
}
