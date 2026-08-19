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
        return HtmlValidator.parse(read(fileAt(url, HTML, generate(HTML, publishedTables, workingDir))));
    }

    /** The lines of the page at the given report URL, rendered in the named format. */
    static List<String> linesAt(String url, String formatName, List<String> publishedTables, Path workingDir) {
        Format format = formatNamed(formatName);
        return read(fileAt(url, format, generate(format, publishedTables, workingDir)))
                .lines()
                .toList();
    }

    private static Format formatNamed(String formatName) {
        return Arrays.stream(BuiltInFormat.values())
                .filter(format -> format.formatName().equals(formatName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No built-in format named " + formatName));
    }

    private static Path generate(Format format, List<String> publishedTables, Path workingDir) {
        Path inputDirectory = PublishedRun.outputFor(publishedTables, workingDir);
        Path outputDirectory = createTempDirectory(workingDir);
        new TableTestReporter().report(format, inputDirectory, outputDirectory);
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
