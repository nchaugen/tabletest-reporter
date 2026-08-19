package org.tabletest.reporter.rendering;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.reporter.TableTestReporter;
import org.tabletest.reporter.support.HtmlValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tabletest.reporter.BuiltInFormat.HTML;

/**
 * Verifies the whole-report search asset: a single shared search index emitted once to the
 * output root, linked from every page by a depth-relative prefix (never root-absolute, so it
 * serves from any subpath), and a search box on every page pointed at that same shared index.
 */
class SearchRenderingTest {

    @TempDir
    Path tempDir;

    @Test
    void the_shared_search_index_is_emitted_once_to_the_output_root() throws IOException {
        Path outDir = generateReport();

        Path asset = outDir.resolve("tabletest-search-index.js");
        assertThat(asset).exists();
        String content = Files.readString(asset);
        assertThat(content).startsWith("window.TableTestSearchIndex = [");
        assertThat(content).contains("\"calendar-calculations/leap-year-rules.html\"");
        assertThat(content).contains("Leap Year Rules");
    }

    @Test
    void the_root_page_links_the_index_and_searches_from_the_output_root() throws IOException {
        Document root = parse(generateReport().resolve("index.html"));

        assertThat(root.select("script[src]").attr("src")).isEqualTo("tabletest-search-index.js");
        assertThat(root.select("input#report-search").attr("data-asset-root")).isEqualTo("");
    }

    @Test
    void a_nested_page_links_the_index_by_a_depth_relative_prefix() throws IOException {
        Document table = parse(generateReport().resolve("calendar-calculations").resolve("leap-year-rules.html"));

        assertThat(table.select("script[src]").attr("src")).isEqualTo("../tabletest-search-index.js");
        assertThat(table.select("input#report-search").attr("data-asset-root")).isEqualTo("../");
    }

    @Test
    void every_page_reference_stays_relative_for_subpath_hosting() throws IOException {
        Path outDir = generateReport();

        for (Path page : new Path[] {
            outDir.resolve("index.html"),
            outDir.resolve("calendar-calculations").resolve("leap-year-rules.html")
        }) {
            assertThat(Files.readString(page)).doesNotContain("http://").doesNotContain("https://");
        }
    }

    private static Document parse(Path page) throws IOException {
        return HtmlValidator.parse(Files.readString(page));
    }

    private Path generateReport() throws IOException {
        Path inDir = Files.createDirectories(tempDir.resolve("in"));
        Path classDir = Files.createDirectories(inDir.resolve("org.example.CalendarCalculations"));
        Files.writeString(classDir.resolve("TABLETEST-calendar-calculations.yaml"), """
            "className": "org.example.CalendarCalculations"
            "slug": "calendar-calculations"
            "title": "Calendar"
            "tableTests":
              - "path": "leapYear(int)/TABLETEST-leap-year-rules.yaml"
                "methodName": "leapYear"
                "slug": "leap-year-rules"
            """);
        Path methodDir = Files.createDirectories(classDir.resolve("leapYear(int)"));
        Files.writeString(methodDir.resolve("TABLETEST-leap-year-rules.yaml"), """
            "title": "Leap Year Rules"
            "headers":
              - "value": "Year"
              - "value": "Is Leap Year?"
            "rows":
              - - "value": "2004"
                - "value": "Yes"
            """);

        Path outDir = Files.createDirectory(tempDir.resolve("out"));
        new TableTestReporter().report(HTML, inDir, outDir);
        return outDir;
    }
}
