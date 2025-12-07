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

import io.github.nchaugen.tabletest.reporter.ReportFormat;
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
    version = {"tabletest-reporter CLI"}
)
public final class TableTestReporterCli implements Callable<Integer> {

    @Option(names = {"-f", "--format"},
        description = "Report format: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})",
        defaultValue = "asciidoc")
    private String format;

    @Option(names = {"-i", "--input"},
        description = "Input directory containing TableTest YAML files (default: <buildDir>/junit-jupiter)",
        defaultValue = "")
    private String inputDirArg;

    @Option(names = {"-o", "--output"},
        description = "Output directory for generated documentation (default: <buildDir>/generated-docs/tabletest)",
        defaultValue = "")
    private String outputDirArg;

    public static void main(String[] args) {
        int exit = new CommandLine(new TableTestReporterCli()).execute(args);
        System.exit(exit);
    }

    @Override
    public Integer call() {
        try {
            Path buildDir = resolveBuildDir();
            Path in = inputDirArg == null || inputDirArg.isBlank()
                ? buildDir.resolve("junit-jupiter")
                : Path.of(inputDirArg);
            Path out = outputDirArg == null || outputDirArg.isBlank()
                ? buildDir.resolve("generated-docs").resolve("tabletest")
                : Path.of(outputDirArg);

            if (!Files.exists(in)) {
                System.err.printf("Input directory does not exist: %s%n", in.toAbsolutePath());
                return 2;
            }

            ReportFormat reportFormat = toFormat(format);
            new TableTestReporter().report(reportFormat, in, out);
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 2;
        } catch (Exception e) {
            System.err.printf("Failed to generate report: %s%n", e.getMessage());
            return 1;
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

    private static Path resolveBuildDir() {
        Path mavenTarget = Path.of("target");
        if (Files.isDirectory(mavenTarget)) return mavenTarget;
        Path gradleBuild = Path.of("build");
        if (Files.isDirectory(gradleBuild)) return gradleBuild;
        // Fallback to Maven-style if neither exists
        return mavenTarget;
    }
}
