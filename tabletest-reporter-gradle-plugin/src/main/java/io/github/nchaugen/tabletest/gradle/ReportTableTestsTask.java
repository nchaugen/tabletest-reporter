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

import io.github.nchaugen.tabletest.reporter.ReportFormat;
import io.github.nchaugen.tabletest.reporter.TableTestReporter;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;

@CacheableTask
public abstract class ReportTableTestsTask extends DefaultTask {

    private final Property<String> format;
    private final DirectoryProperty inputDir;
    private final DirectoryProperty outputDir;

    @Inject
    public ReportTableTestsTask() {
        this.format = getProject().getObjects().property(String.class);
        this.inputDir = getProject().getObjects().directoryProperty();
        this.outputDir = getProject().getObjects().directoryProperty();
        setGroup("documentation");
        setDescription("Generates AsciiDoc or Markdown documentation from TableTest YAML outputs");
    }

    @Optional
    @Input
    public Property<String> getFormat() {
        return format;
    }

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public DirectoryProperty getInputDir() {
        return inputDir;
    }

    @OutputDirectory
    public DirectoryProperty getOutputDir() {
        return outputDir;
    }

    @TaskAction
    public void run() {
        final String fmt = format.getOrElse("asciidoc");
        final ReportFormat reportFormat = toFormat(fmt);

        final Path in = inputDir.get().getAsFile().toPath();
        final Path out = outputDir.get().getAsFile().toPath();

        if (!Files.exists(in)) {
            throw new GradleException("Input directory does not exist: " + in.toAbsolutePath());
        }

        try {
            new TableTestReporter().report(reportFormat, in, out);
        } catch (Exception e) {
            throw new GradleException("Failed to generate TableTest report: " + e.getMessage(), e);
        }
    }

    private static ReportFormat toFormat(String str) {
        if (str == null || str.isBlank()) return ReportFormat.ASCIIDOC;
        switch (str.trim().toLowerCase()) {
            case "adoc":
            case "asciidoc":
            case "asciidoctor":
                return ReportFormat.ASCIIDOC;
            case "md":
            case "markdown":
                return ReportFormat.MARKDOWN;
            default:
                throw new GradleException("Unknown format: " + str + ". Use 'asciidoc' or 'markdown'.");
        }
    }
}
