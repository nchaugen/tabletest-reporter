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

import io.github.nchaugen.tabletest.reporter.ReportFormat;
import io.github.nchaugen.tabletest.reporter.TableTestReporter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Mojo(name = "report", defaultPhase = LifecyclePhase.SITE, threadSafe = true, requiresProject = true)
public final class ReportMojo extends AbstractMojo {

    @Parameter(property = "tabletest.report.format", defaultValue = "asciidoc")
    private String format;

    @Parameter(property = "tabletest.report.inputDirectory", defaultValue = "${project.build.directory}/junit-jupiter")
    private File inputDirectory;

    @Parameter(property = "tabletest.report.outputDirectory", defaultValue = "${project.build.directory}/generated-docs/tabletest")
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            final ReportFormat reportFormat = toFormat(format);
            final Path in = inputDirectory.toPath();
            final Path out = outputDirectory.toPath();

            if (!Files.exists(in)) {
                throw new MojoFailureException("Input directory does not exist: " + in.toAbsolutePath());
            }

            new TableTestReporter().report(reportFormat, in, out);
        } catch (MojoFailureException e) {
            // Propagate user/config failures as-is without wrapping
            throw e;
        } catch (IllegalArgumentException e) {
            throw new MojoFailureException(e.getMessage(), e);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate TableTest report", e);
        }
    }

    private static ReportFormat toFormat(String str) {
        if (str == null || str.isBlank()) return ReportFormat.ASCIIDOC;
        return switch (str.trim().toLowerCase()) {
            case "adoc", "asciidoc", "asciidoctor" -> ReportFormat.ASCIIDOC;
            case "md", "markdown" -> ReportFormat.MARKDOWN;
            default -> throw new IllegalArgumentException("Unknown format: " + str + ". Use 'asciidoc' or 'markdown'.");
        };
    }
}
