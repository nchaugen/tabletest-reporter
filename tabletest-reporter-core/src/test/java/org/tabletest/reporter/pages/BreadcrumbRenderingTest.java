package org.tabletest.reporter.pages;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.support.PublishedReport;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The breadcrumb rules, over a report of one class with one table. {@link PublishedReport}
 * generates it, so a row names the page by the URL a reader would be at.
 */
@DisplayName("Breadcrumbs")
@Description("""
        Every page except the root opens with the trail of pages above it. A reader who arrived
        from a search result can then see where they are, and climb out.

        The rules below read a report of one test class,
        com.example.orders.OrderTest, whose only table is items. That report is three pages deep:
        the package orders, the class page order-test, and the table page items.
        """)
class BreadcrumbRenderingTest {

    /** The report every rule below is read off. */
    private static final List<String> PUBLISHED_TABLES = List.of("com.example.orders.OrderTest#items");

    @TempDir
    Path workingDir;

    @DisplayName("Heads a page with every page above it, and the page itself last")
    @Description("""
            Each page above is a link, relative to the directory the page sits in. The page itself
            closes the trail as text, and not as a link. The root page has nothing above it, so it
            carries no trail at all.
            """)
    @TableTest("""
        Scenario      | Page URL          | Pages above?             | Their links?                    | This page?
        A table page  | /order-test/items | ['orders', 'order-test'] | ['../index.html', 'index.html'] | items
        A class page  | /order-test       | ['orders']               | ['../index.html']               | order-test
        The root page | /                 | []                       | []                              |
        """)
    void namesEveryPageAboveAndItselfLast(
            String pageUrl, List<String> pagesAbove, List<String> theirLinks, String thisPage) {
        Document page = pageAt(pageUrl);

        assertThat(page.select("nav.breadcrumbs a").eachText()).isEqualTo(pagesAbove);
        assertThat(page.select("nav.breadcrumbs a").eachAttr("href")).isEqualTo(theirLinks);
        assertThat(currentPageOf(page)).isEqualTo(thisPage);
    }

    private Document pageAt(String url) {
        return PublishedReport.pageAt(url, PUBLISHED_TABLES, workingDir);
    }

    /** The page the breadcrumbs mark as current, or null where there is no trail. */
    private static String currentPageOf(Document page) {
        var current = page.select("nav.breadcrumbs [aria-current=page]");
        return current.isEmpty() ? null : current.text();
    }
}
