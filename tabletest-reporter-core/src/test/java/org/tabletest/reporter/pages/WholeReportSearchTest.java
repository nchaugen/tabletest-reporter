package org.tabletest.reporter.pages;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.junit.TableTestPublisher;
import org.tabletest.reporter.support.PublishedReport;
import org.tabletest.reporter.support.PublishedSearchIndex;
import org.tabletest.reporter.support.SampleRun;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The search index a report publishes, read back off the file a reader's browser loads. The
 * matching itself runs in the browser, so what a rule can state is what the index describes.
 */
@DisplayName("Whole-report search")
@Description("""
        A report carries a search box on every page, and it searches the whole report rather than
        the page it sits on. The report writes one index beside its pages,
        tabletest-search-index.js at the output root, and every page loads it. Typing in the box
        replaces the navigation tree with the pages whose title or text holds what was typed,
        ignoring case and matching anywhere in a word. The matching runs in the reader's browser,
        so the rules below state what the index describes rather than what a query returns.
        """)
class WholeReportSearchTest {

    @TempDir
    Path workingDir;

    @DisplayName("Describes every page of the report in one index")
    @Description("""
            One entry per page, whatever the page is: the root index, a feature index below it, and
            a table page below that. Each entry names its page by the path from the output root, so
            a page found from anywhere in the report is reachable from anywhere in the report.

            These rows are read off a report of one class, com.example.orders.OrderTest, whose only
            table is items. The report root is the package orders.
            """)
    @TableTest("""
        Scenario       | Page title | Its path?             | Its type?
        The root index | orders     | index.html            | index
        A class index  | order-test | order-test/index.html | index
        A table page   | items      | order-test/items.html | table
        """)
    void describes_every_page_of_the_report_in_one_index(String pageTitle, String path, String type) {
        Map<String, Object> entry = PublishedSearchIndex.entryTitled(pageTitle, reportOfOneTable());

        assertThat(entry.get("path")).isEqualTo(path);
        assertThat(entry.get("type")).isEqualTo(type);
    }

    @DisplayName("Fills a page's search text from that page alone")
    @Description("""
            A table page is searchable by everything a reader can see on it: its title, its
            description, its column headers and its cell values. An index page is searchable by its
            own title and nothing else, so a feature is not found by a word that appears only in a
            rule below it — the rule's own page is what the reader wants.

            These rows are read off a report of one class, Calendar rules, whose table is Leap year
            rules. That table is described as "Gregorian leap year determination.", its columns are
            Scenario, Year and Is leap year?, and its one row reads: A year divisible by four,
            2004, Yes.
            """)
    @TableTest("""
        Scenario                       | Page                                | Word                | In its search text?
        The table's own title          | calendar-rules/leap-year-rules.html | Leap year rules     | true
        The table's description        | calendar-rules/leap-year-rules.html | Gregorian leap year | true
        A column header                | calendar-rules/leap-year-rules.html | Is leap year?       | true
        A cell value                   | calendar-rules/leap-year-rules.html | 2004                | true
        An index page's own title      | calendar-rules/index.html           | Calendar rules      | true
        A word from the table below it | calendar-rules/index.html           | 2004                | false
        """)
    void fills_a_pages_search_text_from_that_page_alone(String page, String word, boolean inItsSearchText) {
        Map<String, Object> entry = PublishedSearchIndex.entryFor(page, reportOfTheCalendarSample());

        assertThat(entry.get("text").toString().contains(word)).isEqualTo(inItsSearchText);
    }

    /** A report of a table published without results — enough for a rule about which pages exist. */
    private Path reportOfOneTable() {
        return PublishedReport.outputOf("html", false, List.of("com.example.orders.OrderTest#items"), workingDir);
    }

    /** A report of a real run, which is where a title, a description and cell values come from. */
    private Path reportOfTheCalendarSample() {
        return PublishedReport.outputOfRun(SampleRun.outputFor(CalendarSample.class, workingDir), workingDir);
    }

    /**
     * Run only through {@link SampleRun} — a static nested class is not picked up by the
     * surrounding test run, so it publishes no page of its own.
     */
    @DisplayName("Calendar rules")
    @ExtendWith(TableTestPublisher.class)
    static class CalendarSample {

        @DisplayName("Leap year rules")
        @Description("Gregorian leap year determination.")
        @TableTest("""
            Scenario                 | Year | Is leap year?
            A year divisible by four | 2004 | Yes
            """)
        void leapYears(int year, String isLeapYear) {
            assertThat(year % 4 == 0 ? "Yes" : "No").isEqualTo(isLeapYear);
        }
    }
}
