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
 * Verifies that HTML pages carry a breadcrumb trail of their ancestors, with each link
 * relative to the page's own directory and the current page marked, over a three-level
 * tree (root package → class index → table).
 */
class BreadcrumbRenderingTest {

    @TempDir
    Path tempDir;

    @Test
    void table_page_links_every_ancestor_relative_to_itself() throws IOException {
        Path outDir = generateReport();

        Document table = HtmlValidator.parse(
                Files.readString(outDir.resolve("calendar-calculations").resolve("leap-year-rules.html")));

        assertThat(table.select("nav.breadcrumbs a").eachAttr("href")).containsExactly("../index.html", "index.html");
        assertThat(table.select("nav.breadcrumbs a").eachText()).containsExactly("example", "Calendar");
        assertThat(table.select("nav.breadcrumbs [aria-current=page]").text())
                .isEqualTo("Leap Year Rules with Single Example");
    }

    @Test
    void class_index_links_up_to_the_root_and_marks_itself_current() throws IOException {
        Path outDir = generateReport();

        Document index = HtmlValidator.parse(
                Files.readString(outDir.resolve("calendar-calculations").resolve("index.html")));

        assertThat(index.select("nav.breadcrumbs a").eachAttr("href")).containsExactly("../index.html");
        assertThat(index.select("nav.breadcrumbs a").eachText()).containsExactly("example");
        assertThat(index.select("nav.breadcrumbs [aria-current=page]").text()).isEqualTo("Calendar");
    }

    @Test
    void root_index_has_no_breadcrumbs() throws IOException {
        Path outDir = generateReport();

        Document root = HtmlValidator.parse(Files.readString(outDir.resolve("index.html")));

        assertThat(root.select("nav.breadcrumbs")).isEmpty();
    }

    private Path generateReport() throws IOException {
        Path inDir = Files.createDirectories(tempDir.resolve("in"));
        Path classDir = Files.createDirectories(inDir.resolve("org.example.CalendarCalculations"));
        Files.writeString(classDir.resolve("TABLETEST-calendar-calculations.yaml"), """
            "className": "org.example.CalendarCalculations"
            "slug": "calendar-calculations"
            "title": "Calendar"
            "description": "Various rules for calendar calculations."
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
