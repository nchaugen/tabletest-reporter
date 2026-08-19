package org.tabletest.reporter.pages;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.support.PublishedReport;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The sidebar rules, over a report of one class with one table. {@link PublishedReport}
 * generates it, so a row names the page by the URL a reader would be at.
 */
@DisplayName("Sidebar navigation")
@Description("""
        Every page carries the whole report as a link tree, so a reader can reach any rule from
        any page without going back to an index first. The rules below are read off a report
        built from one test class, com.example.orders.OrderTest, whose only table is items —
        three pages deep: the package orders, the class page order-test, and the table page items.
        """)
class SidebarRenderingTest {

    /** The report every rule below is read off. */
    private static final List<String> PUBLISHED_TABLES = List.of("com.example.orders.OrderTest#items");

    @TempDir
    Path workingDir;

    @DisplayName("Carries the whole report on every page, with the page's own entry marked")
    @Description("""
            The entries are the same wherever the reader is standing; what moves is which one is
            marked as the page they are on, and where the links point — each is relative to the
            directory that page sits in, so the report can be served from any location.
            """)
    @TableTest("""
        Scenario      | Page URL          | Sidebar entries?                  | Their links?                                                     | Current entry?
        The root page | /                 | ['orders', 'order-test', 'items'] | ['index.html', 'order-test/index.html', 'order-test/items.html'] | orders
        A class page  | /order-test       | ['orders', 'order-test', 'items'] | ['../index.html', 'index.html', 'items.html']                    | order-test
        A table page  | /order-test/items | ['orders', 'order-test', 'items'] | ['../index.html', 'index.html', 'items.html']                    | items
        """)
    void carriesTheWholeReportWithItsOwnEntryMarked(
            String pageUrl, List<String> sidebarEntries, List<String> theirLinks, String currentEntry) {
        Document page = pageAt(pageUrl);

        assertThat(page.select(SIDEBAR_ENTRIES).eachText()).isEqualTo(sidebarEntries);
        assertThat(page.select(SIDEBAR_ENTRIES).eachAttr("href")).isEqualTo(theirLinks);
        assertThat(page.select("aside.sidebar a[aria-current=page]").text()).isEqualTo(currentEntry);
    }

    private static final String SIDEBAR_ENTRIES = "aside.sidebar a.sidebar-home, aside.sidebar .nav-item > a";

    @Test
    void a_menu_button_controls_the_off_canvas_drawer() {
        Document page = pageAt("/order-test/items");

        assertThat(page.select("button#nav-toggle").attr("aria-controls")).isEqualTo("site-nav");
        assertThat(page.select("aside#site-nav.sidebar")).isNotEmpty();
        assertThat(page.select(".nav-backdrop")).isNotEmpty();
    }

    private Document pageAt(String url) {
        return PublishedReport.pageAt(url, PUBLISHED_TABLES, workingDir);
    }
}
