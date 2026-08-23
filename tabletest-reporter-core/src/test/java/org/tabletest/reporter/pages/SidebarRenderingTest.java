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
        Every page carries the whole report as a link tree. A reader can therefore reach any rule
        from any page, without going back to an index first.

        The rules below are read off a report built from one test class,
        com.example.orders.OrderTest, whose only table is items. That report is three pages deep:
        the package orders, the class page order-test, and the table page items.
        """)
class SidebarRenderingTest {

    /** The report every rule below is read off. */
    private static final List<String> PUBLISHED_TABLES = List.of("com.example.orders.OrderTest#items");

    @TempDir
    Path workingDir;

    @DisplayName("Carries the whole report on every page, marking where you are")
    @Description("""
            The entries are the same wherever the reader stands. Two things move. The mark moves to
            the page the reader is on, and the links point at new targets. Each link is relative to
            the directory of that page, so a report serves from any location.
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

    private static final String SIDEBAR_ENTRIES = "aside.sidebar a.sidebar-home, aside.sidebar a.nav-row";

    /** A deeper report, so a rule about the trail has more than one entry above the page. */
    private static final List<String> NESTED_TABLES =
            List.of("com.example.orders.OrderTest#items", "com.example.orders.pricing.PricingTest#discounts");

    @DisplayName("Marks the whole trail down to the page you are on")
    @Description("""
            A page deep in the report is one entry in a long tree. A mark on that entry alone
            highlights a single leaf, and leaves the reader to work out which feature holds it.
            The sidebar therefore marks every entry above the page as well.

            These rows read a second, deeper report, of two test classes:
            com.example.orders.OrderTest, whose table is items, and
            com.example.orders.pricing.PricingTest, whose table is discounts. The report root is
            the package orders, so the entries below it are order-test and pricing.
            """)
    @TableTest("""
        Scenario                | Page URL                        | Trail entries?          | Current entry?
        A page at the top level | /order-test                     | []                      | order-test
        A page one step below   | /order-test/items               | [order-test]            | items
        A page two steps below  | /pricing/pricing-test/discounts | [pricing, pricing-test] | discounts
        """)
    void marks_the_whole_trail_down_to_the_current_page(
            String pageUrl, List<String> trailEntries, String currentEntry) {
        Document page = PublishedReport.pageAt(pageUrl, NESTED_TABLES, workingDir);

        assertThat(page.select("aside.sidebar a.nav-row.ancestor").eachText()).isEqualTo(trailEntries);
        assertThat(page.select("aside.sidebar a[aria-current=page]").text()).isEqualTo(currentEntry);
    }

    @DisplayName("Arrives with the branch holding your page unfolded")
    @Description("""
            An entry that holds pages is a fold. The sidebar of a large spec is therefore a short
            list, and not every rule at once. A reader who follows a link into the middle of a
            report would then have to unfold a path back to where they already are. The sidebar
            writes the folds on the trail to their page open.

            These rows read the deeper report described above. Each fold takes the name of the
            entry that carries it. Only an entry with pages under it is a fold: order-test and
            pricing sit below the root, and pricing-test sits below pricing.
            """)
    @TableTest("""
        Scenario                | Page URL                        | Unfolded branches?
        A page at the top level | /order-test                     | [order-test]
        A page one step below   | /order-test/items               | [order-test]
        A page two steps below  | /pricing/pricing-test/discounts | [pricing, pricing-test]
        """)
    void arrives_with_the_branch_holding_your_page_unfolded(String pageUrl, List<String> unfoldedBranches) {
        Document page = PublishedReport.pageAt(pageUrl, NESTED_TABLES, workingDir);

        assertThat(page.select("aside.sidebar details.nav-branch[open] > summary > a.nav-row")
                        .eachText())
                .isEqualTo(unfoldedBranches);
    }

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
