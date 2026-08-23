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
        A site generator reads an AsciiDoc or Markdown page. The generator decides how that page
        looks and where it sits in the site. Front matter is what a project tells the generator.
        Declare it once, in the frontMatter section of the tabletest-reporter.yaml sidecar. The
        reporter then writes it above every page of a text report. An HTML page carries none,
        because it is a finished page and not source for a generator.

        Both rules below read a report of two test classes in one package,
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
            Each format takes the shape its own tooling reads. Markdown fences a YAML block.
            AsciiDoc writes document attributes. The keys hold the order you declared them in, so
            a reader meets the block as you wrote it.

            The reporter writes a value as it stands, where YAML reads that value back as the same
            text. It quotes a value where YAML would not. Two values need the quotes: a value that
            holds a colon, and a value a reader takes for a number or a boolean.
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

    @DisplayName("Fills a value asked for by token, under whatever key you name")
    @Description("""
            The reporter knows three values a site generator cannot work out. Ask for one by its
            token: $title, $position or $timestamp. Write the token as the value, never as the key.
            Generators do not agree on what to call a position. Hugo calls it weight, Docusaurus
            sidebar_position, a Jekyll theme nav_order, and Antora page-weight. Antora exposes a
            custom attribute under no other prefix.

            A page's position is the place the spec metadata declares. A generator that orders
            pages by that value lists them in the reading order the project chose, and not
            alphabetically.

            The rows below are Markdown. Every text format writes the same values. $timestamp
            carries the run timestamp, which has no fixed value to show here.
            """)
    @TableTest("""
        Scenario                            | Declared                                | Page URL      | Page opens with?
        First page of the reading order     | ['title: $title', 'weight: $position']  | /order-test   | ['---', 'title: order-test', 'weight: 1', '---', '# order-test']
        Second page of the reading order    | ['title: $title', 'weight: $position']  | /product-test | ['---', 'title: product-test', 'weight: 2', '---', '# product-test']
        A key named for another generator   | ['sidebar_position: $position']         | /order-test   | ['---', 'sidebar_position: 1', '---', '# order-test']
        A page with no position of its own  | ['weight: $position', 'layout: report'] | /             | ['---', 'layout: report', '---', '# orders']
        A key named for a value, without it | ['title: a title of my own']            | /order-test   | ['---', 'title: a title of my own', '---', '# order-test']
        """)
    void fills_a_value_asked_for_by_token_under_whatever_key_you_name(
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
