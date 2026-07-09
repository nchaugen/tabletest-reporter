package org.tabletest.reporter.rendering;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.reporter.TableTestReporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tabletest.reporter.BuiltInFormat.HTML;

/**
 * Verifies the persistent sidebar: every page carries the whole-report link tree, with each
 * href relative to that page's own directory and the page's own entry marked current, over a
 * three-level tree (root package → class index → table).
 */
class SidebarRenderingTest {

    @TempDir
    Path tempDir;

    @Test
    void table_page_shows_the_whole_tree_relative_to_itself_with_itself_current() throws IOException {
        Path outDir = generateReport();

        Document table = HtmlValidator.parse(
                Files.readString(outDir.resolve("calendar-calculations").resolve("leap-year-rules.html")));

        assertThat(table.select("aside.sidebar a.sidebar-home").attr("href")).isEqualTo("../index.html");
        assertThat(table.select("aside.sidebar .nav-item.index > a").attr("href"))
                .isEqualTo("index.html");
        assertThat(table.select("aside.sidebar .nav-item.table > a").attr("href"))
                .isEqualTo("leap-year-rules.html");
        assertThat(table.select("aside.sidebar a[aria-current=page]").text())
                .isEqualTo("Leap Year Rules with Single Example");
    }

    @Test
    void root_index_shows_the_tree_relative_to_root_with_home_current() throws IOException {
        Path outDir = generateReport();

        Document root = HtmlValidator.parse(Files.readString(outDir.resolve("index.html")));

        assertThat(root.select("aside.sidebar a.sidebar-home[aria-current=page]")
                        .attr("href"))
                .isEqualTo("index.html");
        assertThat(root.select("aside.sidebar .nav-item.index > a").attr("href"))
                .isEqualTo("calendar-calculations/index.html");
        assertThat(root.select("aside.sidebar .nav-item.table > a").attr("href"))
                .isEqualTo("calendar-calculations/leap-year-rules.html");
    }

    @Test
    void a_menu_button_controls_the_off_canvas_drawer() throws IOException {
        Path outDir = generateReport();

        Document page = HtmlValidator.parse(
                Files.readString(outDir.resolve("calendar-calculations").resolve("leap-year-rules.html")));

        assertThat(page.select("button#nav-toggle").attr("aria-controls")).isEqualTo("site-nav");
        assertThat(page.select("aside#site-nav.sidebar")).isNotEmpty();
        assertThat(page.select(".nav-backdrop")).isNotEmpty();
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
            "title": "Leap Year Rules with Single Example"
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
