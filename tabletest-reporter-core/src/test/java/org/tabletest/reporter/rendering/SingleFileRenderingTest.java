package org.tabletest.reporter.rendering;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.reporter.BuiltInFormat;
import org.tabletest.reporter.TableTestReporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.tabletest.reporter.BuiltInFormat.HTML;

/**
 * Verifies single-file assembly: the whole report collapsed into one self-contained HTML file —
 * every table inlined as an anchored section, the sidebar and search targeting in-page anchors,
 * the search index inlined, and no sibling files written.
 */
class SingleFileRenderingTest {

    @TempDir
    Path tempDir;

    @Test
    void writes_a_single_file_and_no_sibling_assets() throws IOException {
        Path outDir = generateSingleFile();

        assertThat(outDir.resolve("index.html")).exists();
        assertThat(outDir.resolve("tabletest-search-index.js")).doesNotExist();
        try (var files = Files.walk(outDir)) {
            long htmlFiles = files.filter(Files::isRegularFile).count();
            assertThat(htmlFiles).isEqualTo(1);
        }
    }

    @Test
    void inlines_every_table_as_an_anchored_section() throws IOException {
        Document doc = parse(generateSingleFile().resolve("index.html"));

        assertThat(doc.select("section#calendar-calculations__leap-year-rules")).isNotEmpty();
        assertThat(doc.select("section#calendar-calculations__month-lengths")).isNotEmpty();
        assertThat(doc.select("section.report-section table")).hasSize(2);
        assertThat(doc.text()).contains("2004").contains("February");
    }

    @Test
    void sidebar_and_search_target_in_page_anchors() throws IOException {
        Path file = generateSingleFile().resolve("index.html");
        Document doc = parse(file);

        assertThat(doc.select("aside.sidebar .nav-item.table > a").eachAttr("href"))
                .contains("#calendar-calculations__leap-year-rules", "#calendar-calculations__month-lengths");
        String html = Files.readString(file);
        assertThat(html).contains("window.TableTestSearchIndex");
        assertThat(html).contains("\"#calendar-calculations__leap-year-rules\"");
    }

    @Test
    void is_self_contained_with_no_external_or_sibling_references() throws IOException {
        String html = Files.readString(generateSingleFile().resolve("index.html"));

        assertThat(html).doesNotContain("http://").doesNotContain("https://");
        assertThat(html).doesNotContain("<script src=");
        assertThat(html).doesNotContain("tabletest-search-index.js");
    }

    @Test
    void rejects_single_file_mode_for_non_html_formats() throws IOException {
        Path inDir = fixture();
        Path outDir = Files.createDirectory(tempDir.resolve("out-md"));

        assertThatThrownBy(() -> new TableTestReporter().report(BuiltInFormat.MARKDOWN, inDir, outDir, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("html");
    }

    private static Document parse(Path page) throws IOException {
        return HtmlValidator.parse(Files.readString(page));
    }

    private Path generateSingleFile() throws IOException {
        Path outDir = Files.createDirectory(tempDir.resolve("out"));
        new TableTestReporter().report(HTML, fixture(), outDir, true);
        return outDir;
    }

    private Path fixture() throws IOException {
        Path inDir = Files.createDirectories(tempDir.resolve("in"));
        if (Files.exists(inDir.resolve("org.example.CalendarCalculations"))) {
            return inDir;
        }
        Path classDir = Files.createDirectories(inDir.resolve("org.example.CalendarCalculations"));
        Files.writeString(classDir.resolve("TABLETEST-calendar-calculations.yaml"), """
            "className": "org.example.CalendarCalculations"
            "slug": "calendar-calculations"
            "title": "Calendar"
            "tableTests":
              - "path": "leapYear(int)/TABLETEST-leap-year-rules.yaml"
                "methodName": "leapYear"
                "slug": "leap-year-rules"
              - "path": "monthLength(int)/TABLETEST-month-lengths.yaml"
                "methodName": "monthLength"
                "slug": "month-lengths"
            """);
        Path leapDir = Files.createDirectories(classDir.resolve("leapYear(int)"));
        Files.writeString(leapDir.resolve("TABLETEST-leap-year-rules.yaml"), """
            "title": "Leap Year Rules"
            "headers":
              - "value": "Year"
              - "value": "Is Leap Year?"
            "rows":
              - - "value": "2004"
                - "value": "Yes"
            """);
        Path monthDir = Files.createDirectories(classDir.resolve("monthLength(int)"));
        Files.writeString(monthDir.resolve("TABLETEST-month-lengths.yaml"), """
            "title": "Month Lengths"
            "headers":
              - "value": "Month"
              - "value": "Days"
            "rows":
              - - "value": "February"
                - "value": "28"
            """);
        return inDir;
    }
}
