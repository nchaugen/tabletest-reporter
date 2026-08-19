package org.tabletest.reporter.rendering;

import org.jsoup.nodes.Document;
import org.tabletest.reporter.BuiltInFormat;
import org.tabletest.reporter.Format;
import org.tabletest.reporter.PublishedRun;
import org.tabletest.reporter.TableTestReporter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.tabletest.reporter.BuiltInFormat.HTML;

/**
 * Generates a report for a set of published tables and opens one of its pages, so a rule about
 * what a page carries can name the page by the URL a reader would be at.
 */
final class PublishedReport {

    private PublishedReport() {}

    /** The HTML page at the given report URL, in a report built from the given published tables. */
    static Document pageAt(String url, List<String> publishedTables, Path workingDir) {
        return HtmlValidator.parse(read(fileAt(url, HTML, generate(HTML, false, publishedTables, workingDir))));
    }

    /** The lines of the page at the given report URL, rendered in the named format. */
    static List<String> linesAt(String url, String formatName, List<String> publishedTables, Path workingDir) {
        Format format = formatNamed(formatName);
        return read(fileAt(url, format, generate(format, false, publishedTables, workingDir)))
                .lines()
                .toList();
    }

    /**
     * The output directory of a report generated in the named format, so a rule can state what
     * was written rather than what one page says.
     */
    static Path outputOf(String formatName, boolean singleFile, List<String> publishedTables, Path workingDir) {
        return generate(formatNamed(formatName), singleFile, publishedTables, workingDir);
    }

    /** The files a report wrote, as paths relative to its output directory, in sorted order. */
    static List<String> filesIn(Path outputDirectory) {
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
    static String textOf(Path file) {
        return read(file);
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
