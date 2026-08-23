package org.tabletest.reporter.support;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Reads back the search index a report wrote to its output root, so a rule can state what a
 * reader's browser actually searches rather than what the builder returned. The asset is a
 * JavaScript assignment of a JSON array, and JSON is YAML, so the array parses with the loader
 * already on the test path.
 */
public final class PublishedSearchIndex {

    private static final String ASSET_NAME = "tabletest-search-index.js";

    private PublishedSearchIndex() {}

    /** The entries of the index a report wrote, in the order the report wrote them. */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> of(Path outputDirectory) {
        String assignment = read(outputDirectory.resolve(ASSET_NAME));
        String array = assignment
                .substring(assignment.indexOf('=') + 1, assignment.lastIndexOf(';'))
                .trim();
        return (List<Map<String, Object>>) new Load(LoadSettings.builder().build()).loadFromString(array);
    }

    /** The one entry describing the page at the given root-relative path. */
    public static Map<String, Object> entryFor(String path, Path outputDirectory) {
        return of(outputDirectory).stream()
                .filter(entry -> path.equals(entry.get("path")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The search index describes no page at " + path));
    }

    /** The one entry describing the page with the given title. */
    public static Map<String, Object> entryTitled(String title, Path outputDirectory) {
        return of(outputDirectory).stream()
                .filter(entry -> title.equals(entry.get("title")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The search index describes no page titled " + title));
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
