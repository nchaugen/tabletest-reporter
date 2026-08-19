package org.tabletest.reporter.rendering;

import org.jsoup.nodes.Document;
import org.tabletest.reporter.PublishedRun;
import org.tabletest.reporter.TableTestReporter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.tabletest.reporter.BuiltInFormat.HTML;

/**
 * Generates an HTML report for a set of published tables and opens one of its pages, so a rule
 * about what every page carries can name the page by the URL a reader would be at.
 */
final class PublishedReport {

    private PublishedReport() {}

    /** The page at the given report URL, in a report built from the given published tables. */
    static Document pageAt(String url, List<String> publishedTables, Path workingDir) {
        Path outputDirectory = generate(publishedTables, workingDir);
        return HtmlValidator.parse(read(fileAt(url, outputDirectory)));
    }

    private static Path generate(List<String> publishedTables, Path workingDir) {
        Path inputDirectory = PublishedRun.outputFor(publishedTables, workingDir);
        Path outputDirectory = createTempDirectory(workingDir);
        new TableTestReporter().report(HTML, inputDirectory, outputDirectory);
        return outputDirectory;
    }

    /** An index page is the directory's own {@code index.html}; every other page is a file. */
    private static Path fileAt(String url, Path outputDirectory) {
        Path page = outputDirectory.resolve(url.replaceFirst("^/", ""));
        Path index = page.resolve("index.html");
        return Files.isRegularFile(index) ? index : page.resolveSibling(page.getFileName() + ".html");
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
