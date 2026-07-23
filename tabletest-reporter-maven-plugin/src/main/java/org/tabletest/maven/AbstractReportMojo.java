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
import org.apache.maven.plugins.annotations.Parameter;
import org.tabletest.reporter.InputDirectories;
import org.tabletest.reporter.ReportConfiguration;
import org.tabletest.reporter.ReportConfigurationResolver;
import org.tabletest.reporter.ReportOptions;
import org.tabletest.reporter.ReportResult;
import org.tabletest.reporter.TableTestReporter;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * The report-level options and the report run itself, shared by the plugin's goals: one goal
 * reports a single project, the other aggregates a whole reactor, and they differ only in where the
 * input directories come from.
 */
abstract class AbstractReportMojo extends AbstractMojo {

    @Parameter(property = "tabletest.report.format", defaultValue = "asciidoc")
    protected String format;

    @Parameter(
            property = "tabletest.report.outputDirectory",
            defaultValue = "${project.build.directory}/generated-docs/tabletest")
    protected File outputDirectory;

    @Parameter(property = "tabletest.report.templateDirectory")
    protected File templateDirectory;

    @Parameter(property = "tabletest.report.indexDepth", defaultValue = "infinite")
    protected String indexDepth;

    @Parameter(property = "tabletest.report.configFile", defaultValue = "${project.basedir}/tabletest-reporter.yaml")
    protected File configFile;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    protected File baseDirectory;

    /** Generates the report for the given input directories and logs its outcome. */
    protected void generateReport(List<Path> inputDirs, Path outputDir) {
        ReportConfiguration config = ReportConfigurationResolver.resolve(
                new ReportOptions(format, toPath(templateDirectory), indexDepth, null, toPath(configFile)));
        logResult(new TableTestReporter(config).report(inputDirs, outputDir));
    }

    /** Reports the directories that are not there, without stopping a report the others can still fill. */
    protected void warnAboutMissing(InputDirectories inputs) {
        if (!inputs.missing().isEmpty()) {
            getLog().warn(inputs.formatSkippedInputMessage());
        }
    }

    protected static Path toPath(File file) {
        return toPath(file, null);
    }

    protected static Path toPath(File file, Path defaultPath) {
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
