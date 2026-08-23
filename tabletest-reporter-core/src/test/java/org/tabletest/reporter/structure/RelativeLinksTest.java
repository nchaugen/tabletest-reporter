package org.tabletest.reporter.structure;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.support.PublishedReport;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Where a report's own links point, read off published reports in each format. A link is written
 * relative to the page that holds it, so the report serves from wherever it is put.
 */
@DisplayName("Relative links")
@Description("""
        A report is a tree of files that link to each other. Every one of those links is relative
        to the page holding it, so the report works from a local folder, from a web server root,
        and from a subpath such as a project's GitHub Pages — without being generated again for
        each.

        The rules below are read off a report of two test classes in one package,
        com.example.orders.OrderTest and com.example.orders.ProductTest, with one table each. The
        report root is the package orders, above the two class pages order-test and product-test,
        each above its one table, items and price.
        """)
class RelativeLinksTest {

    /** The report every rule below is read off. */
    private static final List<String> PUBLISHED_TABLES =
            List.of("com.example.orders.OrderTest#items", "com.example.orders.ProductTest#price");

    @TempDir
    Path workingDir;

    @DisplayName("Writes an index page's links relative to that page")
    @Description("""
            The same link is written three times over, once in the syntax of each format and once
            per file extension that format writes. What does not change is where it starts: the
            directory the page holding it sits in, never the root of the report and never the root
            of a server.
            """)
    @TableTest("""
        Scenario                       | Page URL    | Links to   | In HTML?                | In markdown?          | In asciidoc?
        The root index to a class page | /           | order-test | ./order-test/index.html | ./order-test/index.md | ./order-test/index.adoc
        A class index to its table     | /order-test | items      | ./items.html            | ./items.md            | ./items.adoc
        """)
    void writes_an_index_pages_links_relative_to_that_page(
            String pageUrl, String linksTo, String inHtml, String inMarkdown, String inAsciidoc) {
        assertThat(htmlLinkTo(linksTo, pageUrl)).isEqualTo(inHtml);
        assertThat(markdownLinkTo(linksTo, pageUrl)).isEqualTo(inMarkdown);
        assertThat(asciidocLinkTo(linksTo, pageUrl)).isEqualTo(inAsciidoc);
    }

    @DisplayName("Reaches a shared asset by climbing to the output root")
    @Description("""
            The stylesheet and the search index are written once, at the root of the report, and
            every page reaches them from where it sits. A page therefore carries as many steps up
            as it lies deep, which is what lets the whole tree be moved as one.

            The asset below is the search index. HTML alone carries it: the text formats have no
            page chrome to load it from.
            """)
    @TableTest("""
        Scenario              | Page URL          | Path to the search index?
        The root index        | /                 | tabletest-search-index.js
        A class index         | /order-test       | ../tabletest-search-index.js
        A table below a class | /order-test/items | ../tabletest-search-index.js
        """)
    void reaches_a_shared_asset_by_climbing_to_the_output_root(String pageUrl, String pathToTheSearchIndex) {
        Document page = PublishedReport.pageAt(pageUrl, PUBLISHED_TABLES, workingDir);

        assertThat(page.select("script[src]").attr("src")).isEqualTo(pathToTheSearchIndex);
    }

    @DisplayName("Writes every link relative, whatever the format")
    @Description("""
            One root-absolute link is enough to break a report served from a subpath, and it breaks
            it only there — which is the kind of fault that reaches a reader before it reaches the
            build. Every file of a whole report is therefore read back, in each format, and the
            links it holds are collected.

            The one address a report may state whole is the link back to the hosting site, which is
            declared rather than generated. The reports below declare none.
            """)
    @TableTest("""
        Scenario               | Format                     | Links starting at a server root?
        A report of any format | {html, markdown, asciidoc} | []
        """)
    void writes_every_link_relative_whatever_the_format(String format, List<String> rootAbsoluteLinks) {
        assertThat(linksIn(format)).isEqualTo(rootAbsoluteLinks);
    }

    /** Every root-absolute link the report holds, over every file of it. */
    private List<String> linksIn(String format) {
        Path outputDirectory = PublishedReport.outputOf(format, false, PUBLISHED_TABLES, workingDir);
        return PublishedReport.filesIn(outputDirectory).stream()
                .map(file -> PublishedReport.textOf(outputDirectory.resolve(file)))
                .flatMap(text -> matches(LINK, text).stream())
                .filter(link -> link.startsWith("/"))
                .toList();
    }

    /** A link target in any of the three notations: an HTML attribute, a markdown target, an AsciiDoc xref. */
    private static final Pattern LINK = Pattern.compile("(?:href=\"|src=\"|]\\(|xref:)([^\"()\\[]+)");

    private String htmlLinkTo(String pageName, String pageUrl) {
        return PublishedReport.pageAt(pageUrl, PUBLISHED_TABLES, workingDir).select("main a.nav-row").stream()
                .filter(row -> row.text().equals(pageName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The page holds no link to " + pageName))
                .attr("href");
    }

    private String markdownLinkTo(String pageName, String pageUrl) {
        return onlyMatch(Pattern.compile("\\[" + Pattern.quote(pageName) + "]\\(([^)]+)\\)"), "markdown", pageUrl);
    }

    private String asciidocLinkTo(String pageName, String pageUrl) {
        return onlyMatch(
                Pattern.compile("xref:([^\\[]+)\\[\\+\\+" + Pattern.quote(pageName) + "\\+\\+]"), "asciidoc", pageUrl);
    }

    private String onlyMatch(Pattern pattern, String format, String pageUrl) {
        String page = String.join("\n", PublishedReport.linesAt(pageUrl, format, PUBLISHED_TABLES, workingDir));
        List<String> found = matches(pattern, page);
        assertThat(found)
                .as("links matching %s in the %s page at %s", pattern, format, pageUrl)
                .hasSize(1);
        return found.get(0);
    }

    private static List<String> matches(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.results().map(result -> result.group(1)).toList();
    }
}
