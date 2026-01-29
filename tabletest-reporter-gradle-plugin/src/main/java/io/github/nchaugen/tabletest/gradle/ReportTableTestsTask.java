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

import io.github.nchaugen.tabletest.reporter.Format;
import io.github.nchaugen.tabletest.reporter.FormatResolver;
import io.github.nchaugen.tabletest.reporter.InputDirectoryResolver;
import io.github.nchaugen.tabletest.reporter.JunitDirParser;
import io.github.nchaugen.tabletest.reporter.ReportResult;
import io.github.nchaugen.tabletest.reporter.TableTestReporter;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Gradle task for generating documentation from TableTest YAML outputs.
 * <p>
 * Reads YAML files produced by the TableTest JUnit extension and generates
 * human-readable documentation in AsciiDoc or Markdown format.
 */
@CacheableTask
public abstract class ReportTableTestsTask extends DefaultTask {

    private final Property<String> format;
    private final DirectoryProperty inputDir;
    private final DirectoryProperty outputDir;
    private final DirectoryProperty templateDir;
    private final Property<String> junitOutputDir;

    /**
     * Creates a new task instance with default configuration.
     */
    @Inject
    public ReportTableTestsTask() {
        this.format = getProject().getObjects().property(String.class);
        this.inputDir = getProject().getObjects().directoryProperty();
        this.outputDir = getProject().getObjects().directoryProperty();
        this.templateDir = getProject().getObjects().directoryProperty();
        this.junitOutputDir = getProject().getObjects().property(String.class);
        setGroup("documentation");
        setDescription("Generates AsciiDoc or Markdown documentation from TableTest YAML outputs");
    }

    /**
     * Returns the output format property.
     *
     * @return property for specifying output format (asciidoc or markdown)
     */
    @org.gradle.api.tasks.Optional
    @Input
    public Property<String> getFormat() {
        return format;
    }

    /**
     * Returns the input directory property.
     *
     * @return property for directory containing TableTest YAML files
     */
    @org.gradle.api.tasks.Optional
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public DirectoryProperty getInputDir() {
        return inputDir;
    }

    /**
     * Returns the output directory property.
     *
     * @return property for directory where generated documentation will be written
     */
    @OutputDirectory
    public DirectoryProperty getOutputDir() {
        return outputDir;
    }

    /**
     * Returns the template directory property.
     *
     * @return property for optional custom template directory
     */
    @org.gradle.api.tasks.Optional
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public DirectoryProperty getTemplateDir() {
        return templateDir;
    }

    @org.gradle.api.tasks.Optional
    @Input
    public Property<String> getJunitOutputDir() {
        return junitOutputDir;
    }

    /**
     * Executes the task to generate documentation from TableTest YAML files.
     *
     * @throws GradleException if input directory does not exist or report generation fails
     */
    @TaskAction
    public void run() {
        final String fmt = format.getOrElse("asciidoc");
        final Path defaultInput = getProject()
                .getLayout()
                .getBuildDirectory()
                .dir("junit-jupiter")
                .get()
                .getAsFile()
                .toPath();
        final Path configuredInput = Optional.ofNullable(toPath(inputDir))
                .filter(path -> !isSamePath(path, defaultInput))
                .orElse(null);
        final Path out = outputDir.get().getAsFile().toPath();

        final Path baseDir = getProject().getProjectDir().toPath();
        final String junitOutputDirValue = junitOutputDir.getOrNull();
        final Path junitDir = JunitDirParser.parse(baseDir, junitOutputDirValue).orElse(null);

        Path in = resolveInputDirectory(configuredInput, List.of(defaultInput), baseDir, junitDir);

        Format reportFormat = FormatResolver.resolve(fmt, toPath(templateDir));

        try {
            ReportResult result = createReporter().report(reportFormat, in, out);
            logResult(result);
        } catch (Exception e) {
            throw new GradleException("Failed to generate TableTest report: " + e.getMessage(), e);
        }
    }

    private @Nullable Path toPath(DirectoryProperty property) {
        return Optional.ofNullable(property)
                .filter(DirectoryProperty::isPresent)
                .map(dir -> dir.get().getAsFile().toPath())
                .orElse(null);
    }

    private static Path resolveInputDirectory(
            Path configuredInput, List<Path> fallbackCandidates, Path baseDir, Path junitDir) {
        InputDirectoryResolver.Result inputResult =
                InputDirectoryResolver.resolve(configuredInput, fallbackCandidates, baseDir, junitDir);
        return Optional.ofNullable(inputResult.path())
                .filter(Files::exists)
                .orElseThrow(() -> new GradleException(inputResult.formatMissingInputMessage()));
    }

    private void logResult(ReportResult result) {
        if (result.filesGenerated() == 0) {
            getLogger().warn(result.message());
        } else {
            getLogger().lifecycle("Generated {} documentation file(s)", result.filesGenerated());
        }
    }

    private TableTestReporter createReporter() {
        return Optional.of(templateDir)
                .filter(DirectoryProperty::isPresent)
                .map(dir -> dir.get().getAsFile().toPath())
                .map(this::validateTemplateDirectory)
                .map(TableTestReporter::new)
                .orElseGet(TableTestReporter::new);
    }

    private Path validateTemplateDirectory(Path templateDirectory) {
        if (!Files.exists(templateDirectory)) {
            throw new GradleException("Template directory does not exist: " + templateDirectory.toAbsolutePath());
        }
        if (!Files.isDirectory(templateDirectory)) {
            throw new GradleException("Template path is not a directory: " + templateDirectory.toAbsolutePath());
        }
        return templateDirectory;
    }

    private static boolean isSamePath(Path left, Path right) {
        return left.toAbsolutePath().normalize().equals(right.toAbsolutePath().normalize());
    }
}
