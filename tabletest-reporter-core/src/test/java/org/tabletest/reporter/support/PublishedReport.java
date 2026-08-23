package org.tabletest.reporter.support;

import org.jsoup.nodes.Document;
import org.tabletest.reporter.BuiltInFormat;
import org.tabletest.reporter.Format;
import org.tabletest.reporter.ReportConfiguration;
import org.tabletest.reporter.ReportConfigurationResolver;
import org.tabletest.reporter.ReportOptions;
import org.tabletest.reporter.TableTestReporter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.tabletest.reporter.BuiltInFormat.HTML;

/**
 * Generates a report for a set of published tables and opens one of its pages, so a rule about
 * what a page carries can name the page by the URL a reader would be at.
 */
public final class PublishedReport {

    private PublishedReport() {}

    /** The HTML page at the given report URL, in a report built from the given published tables. */
    public static Document pageAt(String url, List<String> publishedTables, Path workingDir) {
        return HtmlValidator.parse(read(fileAt(url, HTML, generate(HTML, false, publishedTables, workingDir))));
    }

    /**
     * The HTML page at the given report URL, in a report the build stamped with the given instant.
     * A report reads the clock unless a build pins it, which no rule about the stamp could state.
     */
    public static Document pageAt(String url, Instant generatedAt, List<String> publishedTables, Path workingDir) {
        Path inputDirectory = PublishedRun.outputFor(publishedTables, workingDir);
        Path outputDirectory = createTempDirectory(workingDir);
        new TableTestReporter(stampedWith(generatedAt)).report(inputDirectory, outputDirectory);
        return HtmlValidator.parse(read(fileAt(url, HTML, outputDirectory)));
    }

    private static ReportConfiguration stampedWith(Instant generatedAt) {
        return ReportConfigurationResolver.resolve(new ReportOptions("html", null, null, false, null, generatedAt));
    }

    /** The lines of the page at the given report URL, rendered in the named format. */
    public static List<String> linesAt(String url, String formatName, List<String> publishedTables, Path workingDir) {
        Format format = formatNamed(formatName);
        return read(fileAt(url, format, generate(format, false, publishedTables, workingDir)))
                .lines()
                .toList();
    }

    /**
     * The one table page of a report generated from output the extension itself published — see
     * {@code SampleRun}. Rules about what a real run records read off this page.
     */
    public static Document tablePageOf(Path publishedRunOutput, Path workingDir) {
        return HtmlValidator.parse(read(pageOf(publishedRunOutput, HTML, null, false, workingDir)));
    }

    /**
     * The HTML page at the given report URL, in a report of the output a real run published — so a
     * rule that needs verdicts from rows that really passed or failed can still name its page by
     * the URL a reader would be at.
     */
    public static Document pageOfRun(String url, Path publishedRunOutput, Path workingDir) {
        Path outputDirectory = createTempDirectory(workingDir);
        new TableTestReporter().report(HTML, publishedRunOutput, outputDirectory);
        return HtmlValidator.parse(read(fileAt(url, HTML, outputDirectory)));
    }

    /**
     * The lines of the table page, or of the class index page, of a report generated from output
     * the extension published — rendered in the named format, and with the given directory of
     * templates of the reader's own, or null for the built-in templates alone.
     */
    public static List<String> pageLinesOf(
            Path publishedRunOutput, String formatName, Path templateDirectory, boolean index, Path workingDir) {
        return read(pageOf(publishedRunOutput, formatNamed(formatName), templateDirectory, index, workingDir))
                .lines()
                .toList();
    }

    private static Path pageOf(
            Path publishedRunOutput, Format format, Path templateDirectory, boolean index, Path workingDir) {
        Path outputDirectory = createTempDirectory(workingDir);
        new TableTestReporter(templateDirectory).report(format, publishedRunOutput, outputDirectory);
        return onlyPageIn(outputDirectory, format, index);
    }

    /**
     * A report of one class with one table holds one table page and, below the root, one class
     * index page — enough for a rule to name the page it means with a flag.
     */
    private static Path onlyPageIn(Path outputDirectory, Format format, boolean index) {
        String indexPage = "index" + format.extension();
        try (var paths = Files.walk(outputDirectory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(format.extension()))
                    .filter(file -> file.getFileName().toString().equals(indexPage) == index)
                    .filter(file -> !index || !file.getParent().equals(outputDirectory))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("The report has no such page"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The output directory of a report generated in the named format, so a rule can state what
     * was written rather than what one page says.
     */
    public static Path outputOf(String formatName, boolean singleFile, List<String> publishedTables, Path workingDir) {
        return generate(formatNamed(formatName), singleFile, publishedTables, workingDir);
    }

    /** The files a report wrote, as paths relative to its output directory, in sorted order. */
    public static List<String> filesIn(Path outputDirectory) {
        try (var paths = Files.walk(outputDirectory)) {
            return paths.filter(Files::isRegularFile)
                    .map(file -> outputDirectory.relativize(file).toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The text of a file in a report's output directory. */
    public static String textOf(Path file) {
        return read(file);
    }

    /**
     * The lines of the page at the given report URL, in a report generated with the given
     * {@code tabletest-reporter.yaml} sidecar content — so a rule can show the section it declares
     * and the page that section produces.
     */
    public static List<String> linesAt(
            String url, String formatName, String sidecar, List<String> publishedTables, Path workingDir) {
        Format format = formatNamed(formatName);
        Path inputDirectory = PublishedRun.outputFor(publishedTables, workingDir);
        Path outputDirectory = createTempDirectory(workingDir);
        new TableTestReporter(configuredBy(sidecar, formatName, workingDir)).report(inputDirectory, outputDirectory);
        return read(fileAt(url, format, outputDirectory)).lines().toList();
    }

    private static ReportConfiguration configuredBy(String sidecar, String formatName, Path workingDir) {
        try {
            Path configFile = createTempDirectory(workingDir).resolve("tabletest-reporter.yaml");
            Files.writeString(configFile, sidecar == null ? "" : sidecar);
            return ReportConfigurationResolver.resolve(new ReportOptions(formatName, null, null, false, configFile));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Format formatNamed(String formatName) {
        return Arrays.stream(BuiltInFormat.values())
                .filter(format -> format.formatName().equals(formatName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No built-in format named " + formatName));
    }

    private static Path generate(Format format, boolean singleFile, List<String> publishedTables, Path workingDir) {
        Path inputDirectory = PublishedRun.outputFor(publishedTables, workingDir);
        Path outputDirectory = createTempDirectory(workingDir);
        new TableTestReporter().report(format, inputDirectory, outputDirectory, singleFile);
        return outputDirectory;
    }

    /** An index page is the directory's own index file; every other page is a file beside it. */
    private static Path fileAt(String url, Format format, Path outputDirectory) {
        Path page = outputDirectory.resolve(url.replaceFirst("^/", ""));
        Path index = page.resolve("index" + format.extension());
        return Files.isRegularFile(index) ? index : page.resolveSibling(page.getFileName() + format.extension());
    }

    private static Path createTempDirectory(Path workingDir) {
        try {
            return Files.createTempDirectory(workingDir, "out");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
