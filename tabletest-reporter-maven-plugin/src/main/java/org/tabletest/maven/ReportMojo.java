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
package org.tabletest.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.tabletest.reporter.InputDirectories;
import org.tabletest.reporter.InputDirectoryResolver;
import org.tabletest.reporter.ReportConfiguration;
import org.tabletest.reporter.ReportConfigurationResolver;
import org.tabletest.reporter.ReportOptions;
import org.tabletest.reporter.ReportResult;
import org.tabletest.reporter.TableTestReporter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Mojo(name = "report", defaultPhase = LifecyclePhase.SITE, threadSafe = true)
public final class ReportMojo extends AbstractMojo {

    @Parameter(property = "tabletest.report.format", defaultValue = "asciidoc")
    private String format;

    @Parameter(property = "tabletest.report.inputDirectory")
    private File inputDirectory;

    /** Several input directories, merged into one spec — for a multi-module build. */
    @Parameter(property = "tabletest.report.inputDirectories")
    private File[] inputDirectories;

    @Parameter(
            property = "tabletest.report.outputDirectory",
            defaultValue = "${project.build.directory}/generated-docs/tabletest")
    private File outputDirectory;

    @Parameter(property = "tabletest.report.templateDirectory")
    private File templateDirectory;

    @Parameter(property = "tabletest.report.indexDepth", defaultValue = "infinite")
    private String indexDepth;

    @Parameter(property = "tabletest.report.configFile", defaultValue = "${project.basedir}/tabletest-reporter.yaml")
    private File configFile;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File baseDirectory;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File buildDirectory;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            final Path out = outputDirectory.toPath();
            final Path baseDir = toPath(baseDirectory);
            final Path buildDir = toPath(buildDirectory, baseDir.resolve("target"));
            final List<Path> fallbacks = List.of(buildDir.resolve("junit-jupiter"));
            final Path junitDir = SurefireConfigurationParametersReader.resolveOutputDir(project, baseDir)
                    .orElse(null);

            List<Path> in = resolveInputDirectories(fallbacks, baseDir, junitDir);

            ReportConfiguration config = ReportConfigurationResolver.resolve(
                    new ReportOptions(format, toPath(templateDirectory), indexDepth, null, toPath(configFile)));
            ReportResult result = new TableTestReporter(config).report(in, out);
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

    /**
     * The directories to report from: the explicitly configured list when there is one — a module
     * that has not been built is skipped with a warning rather than failing the spec — otherwise the
     * single directory the usual detection finds.
     */
    private List<Path> resolveInputDirectories(List<Path> fallbacks, Path baseDir, Path junitDir)
            throws MojoFailureException {
        if (inputDirectories == null || inputDirectories.length == 0) {
            return List.of(resolveInputDirectory(toPath(inputDirectory), fallbacks, baseDir, junitDir));
        }
        InputDirectories inputs = InputDirectories.resolve(
                Stream.of(inputDirectories).map(File::toPath).toList(), baseDir);
        if (inputs.isEmpty()) {
            throw new MojoFailureException(inputs.formatMissingInputMessage());
        }
        if (!inputs.missing().isEmpty()) {
            getLog().warn(inputs.formatSkippedInputMessage());
        }
        return inputs.present();
    }

    private Path resolveInputDirectory(Path configuredInputDir, List<Path> fallbacks, Path baseDir, Path junitDir)
            throws MojoFailureException {
        InputDirectoryResolver.Result inputResult =
                InputDirectoryResolver.resolve(configuredInputDir, fallbacks, baseDir, junitDir);
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
}
