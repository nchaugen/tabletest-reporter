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
import io.github.nchaugen.tabletest.reporter.TableTestReporter;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import javax.inject.Inject;

import java.nio.file.Files;
import java.nio.file.Path;

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

    /**
     * Creates a new task instance with default configuration.
     */
    @Inject
    public ReportTableTestsTask() {
        this.format = getProject().getObjects().property(String.class);
        this.inputDir = getProject().getObjects().directoryProperty();
        this.outputDir = getProject().getObjects().directoryProperty();
        this.templateDir = getProject().getObjects().directoryProperty();
        setGroup("documentation");
        setDescription("Generates AsciiDoc or Markdown documentation from TableTest YAML outputs");
    }

    /**
     * Returns the output format property.
     *
     * @return property for specifying output format (asciidoc or markdown)
     */
    @Optional
    @Input
    public Property<String> getFormat() {
        return format;
    }

    /**
     * Returns the input directory property.
     *
     * @return property for directory containing TableTest YAML files
     */
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
    @Optional
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public DirectoryProperty getTemplateDir() {
        return templateDir;
    }

    /**
     * Executes the task to generate documentation from TableTest YAML files.
     *
     * @throws GradleException if input directory does not exist or report generation fails
     */
    @TaskAction
    public void run() {
        final String fmt = format.getOrElse("asciidoc");
        final Path in = inputDir.get().getAsFile().toPath();
        final Path out = outputDir.get().getAsFile().toPath();

        if (!Files.exists(in)) {
            throw new GradleException("Input directory does not exist: " + in.toAbsolutePath());
        }

        Path templateDirectory =
                templateDir.isPresent() ? templateDir.get().getAsFile().toPath() : null;
        Format reportFormat = FormatResolver.resolve(fmt, templateDirectory);

        try {
            TableTestReporter reporter = createReporter();
            reporter.report(reportFormat, in, out);
        } catch (Exception e) {
            throw new GradleException("Failed to generate TableTest report: " + e.getMessage(), e);
        }
    }

    private TableTestReporter createReporter() {
        if (!templateDir.isPresent()) {
            return new TableTestReporter();
        }

        Path templateDirectory = templateDir.get().getAsFile().toPath();
        if (!Files.exists(templateDirectory)) {
            throw new GradleException("Template directory does not exist: " + templateDirectory.toAbsolutePath());
        }
        if (!Files.isDirectory(templateDirectory)) {
            throw new GradleException("Template path is not a directory: " + templateDirectory.toAbsolutePath());
        }

        return new TableTestReporter(templateDirectory);
    }
}
