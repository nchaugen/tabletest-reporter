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
package io.github.nchaugen.tabletest.cli;

import io.github.nchaugen.tabletest.reporter.Format;
import io.github.nchaugen.tabletest.reporter.FormatLister;
import io.github.nchaugen.tabletest.reporter.FormatResolver;
import io.github.nchaugen.tabletest.reporter.InputDirectoryResolver;
import io.github.nchaugen.tabletest.reporter.JunitDirParser;
import io.github.nchaugen.tabletest.reporter.ReportResult;
import io.github.nchaugen.tabletest.reporter.TableTestReporter;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "tabletest-reporter",
        description = "Generate AsciiDoc or Markdown documentation from TableTest YAML outputs.",
        mixinStandardHelpOptions = true,
        version = {"tabletest-reporter CLI"})
public final class TableTestReporterCli implements Callable<Integer> {

    @Option(
            names = {"-l", "--list-formats"},
            description = "List all available output formats and exit")
    private boolean listFormats;

    @Option(
            names = {"-f", "--format"},
            description = "Report format: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})",
            defaultValue = "asciidoc")
    private String format;

    @Option(
            names = {"-i", "--input"},
            description =
                    "Input directory containing TableTest YAML files (default: auto-detect or <buildDir>/junit-jupiter)",
            defaultValue = "")
    private String inputDirArg;

    @Option(
            names = {"-o", "--output"},
            description = "Output directory for generated documentation (default: <buildDir>/generated-docs/tabletest)",
            defaultValue = "")
    private String outputDirArg;

    @Option(
            names = {"-t", "--template-dir"},
            description = "Custom template directory for overriding built-in templates")
    private String templateDirArg;

    public static void main(String[] args) {
        int exit = new CommandLine(new TableTestReporterCli()).execute(args);
        System.exit(exit);
    }

    @Override
    public Integer call() {
        if (listFormats) {
            Path templateDir = resolveTemplateDirLenient();
            String formats = FormatLister.listFormats(templateDir);
            System.out.println(formats);
            return 0;
        }

        try {
            final Path buildDir = resolveBuildDir();
            final Path configuredInput = inputDirArg == null || inputDirArg.isBlank() ? null : Path.of(inputDirArg);
            final Path out = outputDirArg == null || outputDirArg.isBlank()
                    ? buildDir.resolve("generated-docs").resolve("tabletest")
                    : Path.of(outputDirArg);

            InputDirectoryResolver.Result inputResult = InputDirectoryResolver.resolve(
                    configuredInput,
                    null,
                    Path.of("."),
                    JunitDirParser.parse(Path.of("."), null).orElse(null));
            Path in = inputResult.path();
            if (in == null || !Files.exists(in)) {
                System.err.println(inputResult.formatMissingInputMessage());
                return 2;
            }

            Path templateDir = resolveTemplateDir();
            Format reportFormat = FormatResolver.resolve(format, templateDir);
            ReportResult result = createReporter(templateDir).report(reportFormat, in, out);
            if (result.filesGenerated() == 0) {
                System.err.println(result.message());
            } else {
                System.out.printf("Generated %d documentation file(s)%n", result.filesGenerated());
            }
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        } catch (Exception e) {
            System.err.printf("Failed to generate report: %s%n", e.getMessage());
            return 1;
        }
    }

    private TableTestReporter createReporter(Path templateDir) {
        return templateDir != null ? new TableTestReporter(templateDir) : new TableTestReporter();
    }

    private Path resolveTemplateDir() {
        if (templateDirArg == null || templateDirArg.isBlank()) {
            return null;
        }

        Path templateDir = Path.of(templateDirArg);
        if (!Files.exists(templateDir)) {
            throw new IllegalArgumentException("Template directory does not exist: " + templateDir.toAbsolutePath());
        }
        if (!Files.isDirectory(templateDir)) {
            throw new IllegalArgumentException("Template path is not a directory: " + templateDir.toAbsolutePath());
        }

        return templateDir;
    }

    private Path resolveTemplateDirLenient() {
        if (templateDirArg == null || templateDirArg.isBlank()) {
            return null;
        }

        Path templateDir = Path.of(templateDirArg);
        if (!Files.exists(templateDir) || !Files.isDirectory(templateDir)) {
            return null;
        }

        return templateDir;
    }

    private static Path resolveBuildDir() {
        Path mavenTarget = Path.of("target");
        if (Files.isDirectory(mavenTarget)) return mavenTarget;
        Path gradleBuild = Path.of("build");
        if (Files.isDirectory(gradleBuild)) return gradleBuild;
        // Fallback to Maven-style if neither exists
        return mavenTarget;
    }
}
