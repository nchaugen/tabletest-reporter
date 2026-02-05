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
import org.gradle.api.artifacts.Dependency;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TableTestReporterPluginTest {

    @TempDir
    Path projectDir;

    private Project project;
    private Path buildDir;

    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build();
        project.getPluginManager().apply(TableTestReporterPlugin.class);
        buildDir = project.getLayout().getBuildDirectory().get().getAsFile().toPath();
    }

    private TableTestReporterExtension extension() {
        return project.getExtensions().getByType(TableTestReporterExtension.class);
    }

    private ReportTableTestsTask reportTask() {
        return (ReportTableTestsTask) project.getTasks().getByName("reportTableTests");
    }

    private ListFormatsTask listFormatsTask() {
        return (ListFormatsTask) project.getTasks().getByName("listTableTestReportFormats");
    }

    private org.gradle.api.tasks.testing.Test testTask() {
        return (org.gradle.api.tasks.testing.Test) project.getTasks().getByName("test");
    }

    private Path outputDir() {
        return buildDir.resolve("generated-docs").resolve("tabletest");
    }

    @Test
    void reportTask_generates_output_from_minimal_yaml() throws IOException {
        Path inputRoot = buildDir.resolve("junit-jupiter");
        Path testClassDir = inputRoot.resolve("org.example.CalendarTest");
        Path tableDir = testClassDir.resolve("leapYearRules(java.time.Year, boolean)");
        Files.createDirectories(tableDir);

        Files.writeString(testClassDir.resolve("TABLETEST-calendar-test.yaml"), """
            "className": "org.example.CalendarTest"
            "slug": "calendar-test"
            "title": "Calendar"
            "description": "Various rules for calendar calculations."
            "tableTests":
              - "path": "leapYearRules(java.time.Year, boolean)/TABLETEST-leap-year-rules.yaml"
                "methodName": "leapYearRules"
                "slug": "leap-year-rules"
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

        reportTask().run();

        assertThat(outputDir().resolve("index.adoc")).exists();
        assertThat(outputDir().resolve("calendar-test")).isDirectory();
        assertThat(outputDir().resolve("calendar-test").resolve("leap-year-rules.adoc"))
                .exists();
    }

    @Test
    void reportTask_uses_custom_template_when_template_dir_provided() throws IOException {
        setupInputDirectory(buildDir);
        Path templateDir = setupCustomTemplateDirectory(projectDir);
        extension().getTemplateDir().set(templateDir.toFile());

        reportTask().run();

        String content = Files.readString(findGeneratedTableFile(outputDir(), ".adoc"));
        assertThat(content).contains("CUSTOM HEADER");
        assertThat(content).contains("Custom template content");
        assertThat(content).contains("CUSTOM FOOTER");
        assertThat(content).doesNotContain("[%header,cols=");
    }

    @Test
    void reportTask_fails_when_template_directory_does_not_exist() throws IOException {
        setupInputDirectory(buildDir);
        extension().getTemplateDir().set(projectDir.resolve("nonexistent").toFile());

        assertThatThrownBy(() -> reportTask().run()).hasMessageContaining("Template directory does not exist");
    }

    @Test
    void reportTask_fails_when_template_directory_is_not_a_directory() throws IOException {
        setupInputDirectory(buildDir);
        Path notADirectory = projectDir.resolve("file.txt");
        Files.writeString(notADirectory, "not a directory");
        extension().getTemplateDir().set(notADirectory.toFile());

        assertThatThrownBy(() -> reportTask().run()).hasMessageContaining("Template path is not a directory");
    }

    @Test
    void reportTask_generates_markdown_when_format_is_markdown() throws IOException {
        setupInputDirectory(buildDir);
        extension().getFormat().set("markdown");

        reportTask().run();

        String content = Files.readString(findGeneratedTableFile(outputDir(), ".md"));
        assertThat(content).contains("## Test Table");
        assertThat(content).contains("| Column A |");
        assertThat(content).contains("---");
    }

    @Test
    void reportTask_accepts_md_as_format_alias() throws IOException {
        setupInputDirectory(buildDir);
        extension().getFormat().set("md");

        reportTask().run();

        assertThat(findGeneratedIndexFile(outputDir(), ".md")).exists();
    }

    @Test
    void reportTask_accepts_adoc_as_format_alias() throws IOException {
        setupInputDirectory(buildDir);
        extension().getFormat().set("adoc");

        reportTask().run();

        assertThat(findGeneratedIndexFile(outputDir(), ".adoc")).exists();
    }

    @Test
    void reportTask_fails_when_format_is_invalid() throws IOException {
        setupInputDirectory(buildDir);
        extension().getFormat().set("invalid-format");

        assertThatThrownBy(() -> reportTask().run()).hasMessageContaining("Unknown format");
    }

    @Test
    void reportTask_uses_builtin_template_when_no_template_dir_provided() throws IOException {
        setupInputDirectory(buildDir);

        reportTask().run();

        String content = Files.readString(findGeneratedTableFile(outputDir(), ".adoc"));
        assertThat(content).startsWith("==");
        assertThat(content).contains("[%header,cols=");
        assertThat(content).contains("|===");
    }

    @Test
    void listFormatsTask_is_registered() {
        assertThat(listFormatsTask()).isNotNull();
    }

    @Test
    void listFormatsTask_executes_successfully() {
        listFormatsTask().run();
    }

    @Test
    void listFormatsTask_executes_with_custom_template_dir() throws IOException {
        Path templateDir = setupCustomTemplateDirectory(projectDir);
        extension().getTemplateDir().set(templateDir.toFile());

        listFormatsTask().run();
    }

    @Test
    void listFormatsTask_handles_invalid_template_dir_gracefully() {
        extension().getTemplateDir().set(projectDir.resolve("nonexistent").toFile());

        listFormatsTask().run();
    }

    @Test
    void listFormatsTask_handles_template_dir_that_is_file() throws IOException {
        Path notADirectory = projectDir.resolve("file.txt");
        Files.writeString(notADirectory, "not a directory");
        extension().getTemplateDir().set(notADirectory.toFile());

        listFormatsTask().run();
    }

    @Test
    void plugin_adds_tabletest_reporter_junit_to_testImplementation() {
        project.getPluginManager().apply("java");

        Set<String> dependencyNotations =
                project.getConfigurations().getByName("testImplementation").getDependencies().stream()
                        .map(TableTestReporterPluginTest::formatDependency)
                        .collect(Collectors.toSet());

        assertThat(dependencyNotations)
                .anyMatch(notation -> notation.startsWith("io.github.nchaugen:tabletest-reporter-junit:"));
    }

    @Test
    void plugin_sets_autodetection_system_property_on_test_task() {
        project.getPluginManager().apply("java");

        Object propertyValue = testTask().getSystemProperties().get("junit.jupiter.extensions.autodetection.enabled");

        assertThat(propertyValue).isEqualTo("true");
    }

    private static String formatDependency(Dependency dependency) {
        return dependency.getGroup() + ":" + dependency.getName() + ":" + dependency.getVersion();
    }

    private Path setupInputDirectory(Path buildDir) throws IOException {
        Path inputRoot = buildDir.resolve("junit-jupiter");
        Path testClassDir = inputRoot.resolve("org.example.CalendarTest");
        Files.createDirectories(testClassDir);

        Files.writeString(testClassDir.resolve("TABLETEST-calendar-test.yaml"), """
            "className": "org.example.CalendarTest"
            "slug": "calendar-test"
            "title": "Test Table Class"
            "tableTests":
              - "path": "TABLETEST-test.yaml"
                "methodName": "test"
                "slug": "test"
            """);

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

    private Path findGeneratedTableFile(Path outputDir, String extension) throws IOException {
        try (var files = Files.walk(outputDir)) {
            return files.filter(p -> p.toString().endsWith(extension))
                    .filter(p -> !p.getFileName().toString().startsWith("index."))
                    .findFirst()
                    .orElseThrow();
        }
    }

    private Path findGeneratedIndexFile(Path outputDir, String extension) throws IOException {
        try (var files = Files.walk(outputDir)) {
            return files.filter(p -> p.getFileName().toString().equals("index" + extension))
                    .findFirst()
                    .orElseThrow();
        }
    }
}
