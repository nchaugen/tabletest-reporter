package org.tabletest.reporter.pages;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.junit.TableTestPublisher;
import org.tabletest.reporter.support.PublishedReport;
import org.tabletest.reporter.support.SampleRun;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The verdict an HTML page states, read off a report of a run that really passed and failed.
 * A verdict comes from rows that ran, so the fixture is a run rather than a page of data.
 */
@DisplayName("Pass/fail rollup")
@Description("""
        Every HTML page states how many of its scenarios pass, so a reader arriving at a spec
        can see whether the code still does what the page says. A table page counts its own
        rows; an index page sums the pages below it.

        The rules below are read off a report of a run of two test classes. AdditionSample
        publishes the table sums, whose two rows both pass. SubtractionSample publishes the
        table differences, whose two rows include one that claims the wrong answer. The report
        root is the index above the two class pages.
        """)
class PassFailRollupTest {

    @TempDir
    Path workingDir;

    @DisplayName("States on an HTML page how many of its scenarios pass")
    @Description("""
            A page with a broken scenario leads with the count of what is broken, because that is
            what a reader needs first. A page where everything passes says so in one line.

            An index page never counts a scenario twice and never counts one that is not below it,
            so the root's numbers are the two class pages added together.
            """)
    @TableTest("""
        Scenario                       | Page URL                        | Verdict?
        A table where every row passes | /addition-sample/sums           | All 2 scenarios hold
        A table with a broken row      | /subtraction-sample/differences | 1 of 2 scenarios broken
        The index above one table      | /addition-sample                | All 2 scenarios hold
        The index above both classes   | /                               | 1 of 4 scenarios broken
        """)
    void states_on_an_html_page_how_many_of_its_scenarios_pass(String pageUrl, String verdict) {
        Document page = PublishedReport.pageOfRun(pageUrl, runOfBothSamples(), workingDir);

        assertThat(page.select("p.verdict").text()).isEqualTo(verdict);
    }

    @DisplayName("Says nothing about a page where no scenario ran")
    @Description("""
            A verdict comes from rows that ran. A page built from a table that carries no row
            results — output the reporter was handed rather than a run it saw — therefore states
            nothing at all, instead of claiming every scenario holds.

            These two rows are read off a second report, of one class, com.example.orders.OrderTest,
            whose table items was published without results.
            """)
    @TableTest("""
        Scenario                     | Page URL          | Verdict?
        A table page with no results | /order-test/items | ''
        The index page above it      | /order-test       | ''
        """)
    void says_nothing_about_a_page_where_no_scenario_ran(String pageUrl, String verdict) {
        Document page = PublishedReport.pageAt(pageUrl, List.of("com.example.orders.OrderTest#items"), workingDir);

        assertThat(page.select("p.verdict").text()).isEqualTo(verdict);
    }

    private Path runOfBothSamples() {
        return SampleRun.outputFor(List.of(AdditionSample.class, SubtractionSample.class), workingDir);
    }

    /**
     * Run only through {@link SampleRun} — a static nested class is not picked up by the
     * surrounding test run, so it publishes no page of its own.
     */
    @ExtendWith(TableTestPublisher.class)
    static class AdditionSample {

        @TableTest("""
            Scenario          | Augend | Addend | Sum?
            Two whole numbers | 2      | 3      | 5
            Adding nothing    | 7      | 0      | 7
            """)
        void sums(int augend, int addend, int sum) {
            assertThat(augend + addend).isEqualTo(sum);
        }
    }

    /** The second row claims the wrong answer deliberately, so the run records a real failure. */
    @ExtendWith(TableTestPublisher.class)
    static class SubtractionSample {

        @TableTest("""
            Scenario          | Minuend | Subtrahend | Difference?
            Two whole numbers | 5       | 3          | 2
            A wrong answer    | 9       | 4          | 6
            """)
        void differences(int minuend, int subtrahend, int difference) {
            assertThat(minuend - subtrahend).isEqualTo(difference);
        }
    }
}
