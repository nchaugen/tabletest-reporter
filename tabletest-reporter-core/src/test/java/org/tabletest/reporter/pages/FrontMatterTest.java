package org.tabletest.reporter.pages;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.junit.Lines;
import org.tabletest.reporter.support.PublishedReport;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Front matter")
@Description("""
        An AsciiDoc or Markdown page is read by a site generator, which decides how the page looks
        and where it sits. Front matter is how a project tells that generator what it needs to know.
        It is declared once, in the frontMatter section of the tabletest-reporter.yaml sidecar, and
        written above every page of a text report. HTML gets none — it is a finished page, not
        source for a generator.

        Both rules below are read off a report built from two test classes in one package,
        com.example.orders.OrderTest and com.example.orders.ProductTest, with one table each.
        """)
public class FrontMatterTest {

    /** The report both rules are read off. */
    private static final List<String> PUBLISHED_TABLES =
            List.of("com.example.orders.OrderTest#items", "com.example.orders.ProductTest#price");

    @TempDir
    Path workingDir;

    @DisplayName("Writes the declared front matter above a text page")
    @Description("""
            Each format is given the shape its own tooling reads: Markdown fences a YAML block,
            AsciiDoc writes document attributes. The keys keep the order they were declared in, so
            the block reads as it was written.

            A value is written as it stands wherever YAML reads it back as the same text, and quoted
            where it would not be — a value carrying a colon, or one a reader would take for a
            number or a boolean.
            """)
    @TableTest("""
        Scenario                   | Format   | Declared                           | Page opens with?
        A section of literal keys  | markdown | ['layout: report', 'type: docs']   | ['---', 'layout: report', 'type: docs', '---', '# orders']
        A section of literal keys  | asciidoc | ['layout: report', 'type: docs']   | [':layout: report', ':type: docs', '= ++orders++']
        A value YAML would misread | markdown | ['version: "2.0"', 'note: "a: b"'] | ['---', 'version: "2.0"', 'note: "a: b"', '---', '# orders']
        No front matter declared   | markdown |                                    | ['# orders']
        No front matter declared   | asciidoc |                                    | ['= ++orders++']
        """)
    void writes_the_declared_front_matter_above_a_text_page(
            String format, @Lines List<String> declared, @Lines List<String> pageOpensWith) {
        assertThat(PublishedReport.linesAt("/", format, sidecarWith(declared), PUBLISHED_TABLES, workingDir))
                .startsWith(pageOpensWith.toArray(String[]::new));
    }

    @DisplayName("Fills the keys it is asked to derive for each page")
    @Description("""
            Three keys carry a value the reporter knows and the site generator does not, and
            declaring one as true asks for it: title, weight and generated. The position is the one
            the spec metadata declares, so a generator ordering pages by weight lists them in the
            reading order the project chose rather than alphabetically.

            The rows below are Markdown; the values are the same whichever text format writes them.
            The third derived key, generated, carries the run timestamp and so has no fixed value to
            show here.
            """)
    @TableTest("""
        Scenario                           | Declared                           | Page URL      | Page opens with?
        First page of the reading order    | ['title: true', 'weight: true']    | /order-test   | ['---', 'title: order-test', 'weight: 1', '---', '# order-test']
        Second page of the reading order   | ['title: true', 'weight: true']    | /product-test | ['---', 'title: product-test', 'weight: 2', '---', '# product-test']
        A page with no position of its own | ['weight: true', 'layout: report'] | /             | ['---', 'layout: report', '---', '# orders']
        """)
    void fills_the_keys_it_is_asked_to_derive_for_each_page(
            @Lines List<String> declared, String pageUrl, @Lines List<String> pageOpensWith) {
        assertThat(PublishedReport.linesAt(pageUrl, "markdown", sidecarWith(declared), PUBLISHED_TABLES, workingDir))
                .startsWith(pageOpensWith.toArray(String[]::new));
    }

    /** The sidecar file a row declares, as a frontMatter section of its own. */
    private static String sidecarWith(List<String> declared) {
        if (declared == null) {
            return "";
        }
        return "frontMatter:\n  " + String.join("\n  ", declared) + "\n";
    }
}
