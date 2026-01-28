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

import io.github.nchaugen.tabletest.reporter.Format;
import io.github.nchaugen.tabletest.reporter.FormatResolver;
import io.github.nchaugen.tabletest.reporter.InputDirectoryResolver;
import io.github.nchaugen.tabletest.reporter.ReportResult;
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
import java.util.List;
import java.util.Optional;

@Mojo(name = "report", defaultPhase = LifecyclePhase.SITE, threadSafe = true)
public final class ReportMojo extends AbstractMojo {

    @Parameter(property = "tabletest.report.format", defaultValue = "asciidoc")
    private String format;

    @Parameter(property = "tabletest.report.inputDirectory")
    private File inputDirectory;

    @Parameter(
            property = "tabletest.report.outputDirectory",
            defaultValue = "${project.build.directory}/generated-docs/tabletest")
    private File outputDirectory;

    @Parameter(property = "tabletest.report.templateDirectory")
    private File templateDirectory;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File baseDirectory;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File buildDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            final Path out = outputDirectory.toPath();
            final Path baseDir = toPath(baseDirectory);
            final Path buildDir = toPath(buildDirectory, baseDir.resolve("target"));
            final List<Path> fallbacks = List.of(buildDir.resolve("junit-jupiter"));

            Path in = resolveInputDirectory(toPath(inputDirectory), fallbacks, baseDir);

            Format reportFormat = FormatResolver.resolve(format, toPath(templateDirectory));
            ReportResult result = createReporter().report(reportFormat, in, out);
            logResult(result);
        } catch (MojoFailureException e) {
            // Propagate user/config failures as-is without wrapping
            throw e;
        } catch (IllegalArgumentException e) {
            throw new MojoFailureException(e.getMessage(), e);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate TableTest report", e);
        }
    }

    private Path resolveInputDirectory(Path configuredInputDir, List<Path> fallbacks, Path baseDir)
            throws MojoFailureException {
        InputDirectoryResolver.Result inputResult =
                InputDirectoryResolver.resolve(configuredInputDir, fallbacks, baseDir, null);
        return Optional.ofNullable(inputResult.path())
                .filter(Files::exists)
                .orElseThrow(() -> new MojoFailureException(inputResult.formatMissingInputMessage()));
    }

    private static Path toPath(File file) {
        return toPath(file, null);
    }

    private static Path toPath(File file, Path defaultPath) {
        return Optional.ofNullable(file).map(File::toPath).orElse(defaultPath);
    }

    private void logResult(ReportResult result) {
        if (result.filesGenerated() == 0) {
            getLog().warn(result.message());
        } else {
            getLog().info("Generated " + result.filesGenerated() + " documentation file(s)");
        }
    }

    private TableTestReporter createReporter() throws MojoFailureException {
        Path templateDir = toPath(templateDirectory);
        if (templateDir == null) {
            return new TableTestReporter();
        }
        if (!Files.exists(templateDir)) {
            throw new MojoFailureException("Template directory does not exist: " + templateDir.toAbsolutePath());
        }
        if (!Files.isDirectory(templateDir)) {
            throw new MojoFailureException("Template path is not a directory: " + templateDir.toAbsolutePath());
        }

        return new TableTestReporter(templateDir);
    }
}
