package org.tabletest.reporter.pages;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.ContextLoader;
import org.tabletest.reporter.TemplateEngine;
import org.tabletest.reporter.junit.Lines;
import org.tabletest.reporter.support.PublishedReport;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tabletest.reporter.BuiltInFormat.ASCIIDOC;
import static org.tabletest.reporter.BuiltInFormat.MARKDOWN;
import static org.tabletest.reporter.support.AsciiDocValidator.assertValidAsciiDoc;
import static org.tabletest.reporter.support.MarkdownValidator.assertValidMarkdown;

@DisplayName("Index pages")
@Description("""
        Every level of the report has an index page listing what is under it, and the report opens
        on the root one.

        The rule below is read off a report built from two test classes in one package,
        com.example.orders.OrderTest and com.example.orders.ProductTest, with one table each. The
        root index therefore sits above two class pages, and each class page above one table.
        """)
public class IndexTest {

    /** The report the rule below is read off. */
    private static final List<String> PUBLISHED_TABLES =
            List.of("com.example.orders.OrderTest#items", "com.example.orders.ProductTest#price");

    @TempDir
    Path workingDir;

    private final TemplateEngine templateEngine = new TemplateEngine();

    private final Map<String, Object> context = new ContextLoader().fromYaml("""
        "title": "Title of the Test Class"
        "description": "A free-text description explaining what these tables are about."
        "name": "Test Class"
        "contents":
        - "name": "A Table"
          "type": "table"
          "path": "path/to/a_table"
        - "name": "B Table"
          "type": "table"
          "path": "path/to/b_table"
        - "name": "C Table"
          "type": "table"
          "path": "path/to/c_table"
        """);

    @DisplayName("Links an index page to every page beneath it, nested as they are")
    @Description("""
            The whole subtree is listed, not just the level below. One nesting level of the list is
            one level of the report.

            Each link is written in the syntax of the format being generated, and points at the file
            that format writes. A report therefore reads the same whichever of them it is published
            in.
            """)
    @TableTest("""
        Scenario            | Format   | Page URL    | Page content?
        The root index page | asciidoc | /           | ['= ++orders++', '', '* xref:./order-test/index.adoc[++order-test++]', '** xref:./order-test/items.adoc[++items++]', '* xref:./product-test/index.adoc[++product-test++]', '** xref:./product-test/price.adoc[++price++]']
        The root index page | markdown | /           | ['# orders', '', '* [order-test](./order-test/index.md)', '  * [items](./order-test/items.md)', '* [product-test](./product-test/index.md)', '  * [price](./product-test/price.md)']
        A class index page  | asciidoc | /order-test | ['= ++order-test++', '', '* xref:./items.adoc[++items++]']
        A class index page  | markdown | /order-test | ['# order-test', '', '* [items](./items.md)']
        """)
    void linksToEveryPageBeneathIt(String format, String pageUrl, @Lines List<String> pageContent) {
        assertThat(PublishedReport.linesAt(pageUrl, format, PUBLISHED_TABLES, workingDir))
                .isEqualTo(pageContent);
    }

    /** A deeper report, so a rule about folding has a level below the level below the top. */
    private static final List<String> NESTED_TABLES =
            List.of("com.example.orders.OrderTest#items", "com.example.orders.pricing.PricingTest#discounts");

    @DisplayName("Folds an HTML index page below its top level")
    @Description("""
            Every page under an index is written into it, however deep. Only the top level is
            open: an entry that holds pages is a fold, and the reader opens the part they want
            rather than the publisher choosing one depth for every reader. A folded page is
            still on the page, so a browser search and a printed copy both reach it.

            These rows are read off the root index of a second, deeper report, built from two
            test classes: com.example.orders.OrderTest, whose table is items, and
            com.example.orders.pricing.PricingTest, whose table is discounts. The report root is
            the package orders, so its top level is order-test and pricing.
            """)
    @TableTest("""
        Scenario                       | Entry        | Behind a fold?
        A page at the top level        | order-test   | false
        A table below a top-level page | items        | true
        A fold below a top-level fold  | pricing-test | true
        A table two levels down        | discounts    | true
        """)
    void folds_an_html_index_page_below_its_top_level(String entry, boolean behindAFold) {
        Document rootIndex = PublishedReport.pageAt("/", NESTED_TABLES, workingDir);

        assertThat(foldsAround(entryNamed(entry, rootIndex))).isEqualTo(behindAFold);
    }

    /** The entry's own row in the index body, which fails the rule outright if it was not written. */
    private static Element entryNamed(String entry, Document rootIndex) {
        return rootIndex.select("main a.nav-row").stream()
                .filter(row -> row.text().equals(entry))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The index page holds no entry named " + entry));
    }

    /**
     * Whether any fold around the entry is closed. A fold's own row is its summary and shows
     * whether that fold is open or closed, so the walk starts at the item the row sits in.
     */
    private static boolean foldsAround(Element row) {
        return row.closest("li.nav-item").parents().stream()
                .filter(parent -> parent.tagName().equals("details"))
                .anyMatch(parent -> !parent.hasAttr("open"));
    }

    /**
     * A class index carries the title and description its test class declared. Conformance rather
     * than a rule: the report the rule above is read off is built from classes without either.
     */
    @Test
    void supported_in_asciidoc() {
        String rendered = templateEngine.renderIndex(ASCIIDOC, context);

        assertThat(rendered).isEqualTo("""
                = ++Title of the Test Class++

                A free-text description explaining what these tables are about.

                * xref:./path/to/a_table.adoc[++A Table++]
                * xref:./path/to/b_table.adoc[++B Table++]
                * xref:./path/to/c_table.adoc[++C Table++]
                """);
        assertValidAsciiDoc(rendered);
    }

    @Test
    void supported_in_markdown() {
        String rendered = templateEngine.renderIndex(MARKDOWN, context);

        assertThat(rendered).isEqualTo("""
                # Title of the Test Class

                A free-text description explaining what these tables are about.

                * [A Table](./path/to/a_table.md)
                * [B Table](./path/to/b_table.md)
                * [C Table](./path/to/c_table.md)
                """);
        assertValidMarkdown(rendered);
    }
}
