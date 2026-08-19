package org.tabletest.reporter.rendering;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.junit.Description;
import org.tabletest.junit.Scenario;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.SampleRun;
import org.tabletest.reporter.junit.TableTestPublisher;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The role and result rules, read off real runs of the sample classes below. Roles are decided
 * during a run by code a hand-built context cannot reach, so {@link SampleRun} runs the class and
 * the report is generated from what it published.
 */
@DisplayName("Roles and results")
@Description("""
        A published table is not just its text: the reporter marks which column names the scenario
        and which holds the expectation, and records how each row fared when it ran. In HTML those
        marks are classes on the cell, which is what the rules below read; markdown has nowhere to
        put them and shows the values alone. Each rule names the sample test class it is read off,
        and its rows are the columns or the rows of that class's table.
        """)
class RolesAndResultsTest {

    @TempDir
    Path workingDir;

    @DisplayName("A table marks the column naming the scenario and the column holding the expectation")
    @Description("""
            A table with more columns than the test method has parameters spends its first column
            on the scenario name, so that column is marked as the scenario and never passed to the
            test. A column whose header ends in a question mark holds what the row expected. Every
            other column is an input and carries no mark. Read off LeapYearSample:
            Scenario | Year | Leap?, over a method taking year and leap.
            """)
    @TableTest("""
        Scenario                     | Column   | Marked as?
        The column naming the row    | Scenario | scenario
        A column the test is given   | Year     |
        The column ending in a query | Leap?    | expectation
        """)
    void marksTheScenarioAndExpectationColumns(String column, String markedAs) {
        assertThat(columnMarkOf(publishedTableOf(LeapYearSample.class), column)).isEqualTo(markedAs);
    }

    @DisplayName("A parameter annotated @Scenario marks its column, wherever it sits")
    @Description("""
            Spending the first column is a convention, not a requirement. A test that takes every
            column as a parameter can still name one of them the scenario by annotating it with
            @Scenario, and the mark follows the annotation rather than the position. Read off
            AnnotatedScenarioSample: Year | Case | Leap?, whose second parameter is annotated.
            """)
    @TableTest("""
        Scenario                          | Column | Marked as?
        A column before the annotated one | Year   |
        The annotated column              | Case   | scenario
        The expectation is unaffected     | Leap?  | expectation
        """)
    void marksTheColumnOfAnAnnotatedParameter(String column, String markedAs) {
        assertThat(columnMarkOf(publishedTableOf(AnnotatedScenarioSample.class), column))
                .isEqualTo(markedAs);
    }

    @DisplayName("A column holds the expectation when its header matches the expectation pattern")
    @Description("""
            The question mark is only the default. The pattern is the JUnit configuration
            parameter tabletest.reporter.expectation.pattern, matched against the whole header, so
            a project that words its expectations differently can say so once. Setting it replaces
            the default rather than adding to it. Read off ExpectationPatternSample:
            Scenario | Year | Leap? | Expected note.
            """)
    @TableTest("""
        Scenario              | Expectation pattern | Column        | Holds the expectation?
        The default pattern   |                     | Leap?         | true
        The default pattern   |                     | Expected note | false
        A pattern of your own | '^Expected.*'       | Leap?         | false
        A pattern of your own | '^Expected.*'       | Expected note | true
        """)
    void marksTheColumnMatchingTheExpectationPattern(String expectationPattern, String column, boolean holds) {
        Document published = publishedTableOf(ExpectationPatternSample.class, expectationPattern);

        assertThat("expectation".equals(columnMarkOf(published, column))).isEqualTo(holds);
    }

    @DisplayName("A row carries the verdict of the scenario it ran, and a broken one its message")
    @Description("""
            The report is generated from a test run, so it publishes what happened, not only what
            was written: every cell of a row is marked with that row's verdict, and a row that
            broke is listed again below the table with the message it failed on. A spec whose rows
            are green is a spec the code still satisfies. Read off LeapYearSample, whose century
            row claims the wrong answer on purpose.
            """)
    @TableTest("""
        Scenario         | Row                      | Verdict? | Message published?
        A row that held  | A year divisible by four | passed   |
        A row that broke | A century year           | failed   | ['expected: "Yes"', ' but was: "No"']
        """)
    void carriesTheVerdictOfTheScenarioItRan(String row, String verdict, List<String> messagePublished) {
        Document published = publishedTableOf(LeapYearSample.class);

        assertThat(verdictOf(published, row)).isEqualTo(verdict);
        assertThat(messageOf(published, row)).isEqualTo(messagePublished);
    }

    private Document publishedTableOf(Class<?> sampleClass) {
        return PublishedReport.tablePageOf(SampleRun.outputFor(sampleClass, workingDir), workingDir);
    }

    private Document publishedTableOf(Class<?> sampleClass, String expectationPattern) {
        Map<String, String> configuration = expectationPattern == null
                ? Map.of()
                : Map.of("tabletest.reporter.expectation.pattern", expectationPattern);
        return PublishedReport.tablePageOf(SampleRun.outputFor(sampleClass, configuration, workingDir), workingDir);
    }

    /** The mark the published header carries beyond being a cell, or null where it carries none. */
    private static String columnMarkOf(Document published, String column) {
        Element header = published.select("thead th").stream()
                .filter(cell -> cell.text().equals(column))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The published table has no column " + column));
        Set<String> marks = new LinkedHashSet<>(header.classNames());
        marks.remove("cell");
        return marks.isEmpty() ? null : String.join(" ", marks);
    }

    /** The verdict every cell of the named row is marked with. */
    private static String verdictOf(Document published, String row) {
        Set<String> verdicts = rowNamed(published, row).select("td").stream()
                .flatMap(cell -> cell.classNames().stream())
                .filter(mark -> mark.equals("passed") || mark.equals("failed"))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return verdicts.size() == 1 ? verdicts.iterator().next() : String.join(" and ", verdicts);
    }

    /**
     * The lines of the message published below the table for the named row, or null where it did
     * not break.
     */
    private static List<String> messageOf(Document published, String row) {
        return published.select("section.failures details").stream()
                .filter(broken -> broken.select("summary").text().endsWith(row))
                .map(broken -> broken.select("pre").text().lines().toList())
                .findFirst()
                .orElse(null);
    }

    private static Element rowNamed(Document published, String row) {
        return published.select("tbody tr").stream()
                .filter(cells -> cells.select("td").first().text().equals(row))
                .findFirst()
                .orElseThrow(() -> new AssertionError("The published table has no row " + row));
    }

    /**
     * Run only through {@link SampleRun} — a static nested class is not picked up by the
     * surrounding test run, so it publishes no page of its own. The century row claims the wrong
     * answer deliberately, so the run records one verdict of each kind.
     */
    @ExtendWith(TableTestPublisher.class)
    static class LeapYearSample {

        @TableTest("""
            Scenario                 | Year | Leap?
            A year divisible by four | 2004 | Yes
            A century year           | 1900 | Yes
            """)
        void leapYears(int year, String leap) {
            assertThat(isLeap(year) ? "Yes" : "No").isEqualTo(leap);
        }
    }

    /** Every column is a parameter, and the scenario column is named by its annotation. */
    @ExtendWith(TableTestPublisher.class)
    static class AnnotatedScenarioSample {

        @TableTest("""
            Year | Case                     | Leap?
            2004 | A year divisible by four | Yes
            """)
        void leapYears(int year, @Scenario String testCase, String leap) {
            assertThat(testCase).isNotBlank();
            assertThat(isLeap(year) ? "Yes" : "No").isEqualTo(leap);
        }
    }

    /** Two columns a pattern can choose between: one ends in a query, one opens with a word. */
    @ExtendWith(TableTestPublisher.class)
    static class ExpectationPatternSample {

        @TableTest("""
            Scenario                 | Year | Leap? | Expected note
            A year divisible by four | 2004 | Yes   | Divisible by four
            """)
        void leapYears(int year, String leap, String expectedNote) {
            assertThat(expectedNote).isNotBlank();
            assertThat(isLeap(year) ? "Yes" : "No").isEqualTo(leap);
        }
    }

    private static boolean isLeap(int year) {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    }
}
