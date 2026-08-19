package org.tabletest.reporter.tables;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.junit.Description;
import org.tabletest.junit.Scenario;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.junit.TableTestPublisher;
import org.tabletest.reporter.support.PublishedTable;
import org.tabletest.reporter.support.SampleRun;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The role and result rules, read off real runs of the sample classes below. Roles are decided
 * during a run by code a hand-built context cannot reach, so {@link SampleRun} runs the class and
 * the report is generated from what it published.
 */
@DisplayName("Roles and results")
@Description("""
        A published table is not just its text: the reporter marks which column names the scenario
        and which holds the expectation, and records how each row fared when it ran. A mark needs
        somewhere to live — HTML gives it a class on the cell and AsciiDoc a role on the value,
        while markdown has nowhere to put one and shows the values alone. The rules below carry a
        column per format for that reason. Each names the sample test class it is read off, and its
        rows are the columns or the rows of that class's table.
        """)
class RolesAndResultsTest {

    @TempDir
    Path workingDir;

    @DisplayName("Marks the column naming the scenario and the column holding the expectation")
    @Description("""
            A table with more columns than the test method has parameters spends its first column
            on the scenario name, so that column is marked as the scenario and never passed to the
            test. A column whose header ends in a question mark holds what the row expected. Every
            other column is an input and carries no mark. Read off LeapYearSample:
            Scenario | Year | Leap?, over a method taking year and leap.
            """)
    @TableTest("""
        Scenario                     | Column   | Mark in HTML? | Mark in AsciiDoc? | Mark in markdown?
        The column naming the row    | Scenario | scenario      | scenario          |
        A column the test is given   | Year     |               |                   |
        The column ending in a query | Leap?    | expectation   | expectation       |
        """)
    void marksTheScenarioAndExpectationColumns(
            String column, String markInHtml, String markInAsciiDoc, String markInMarkdown) {
        assertThat(markOf(LeapYearSample.class, "html", column)).isEqualTo(markInHtml);
        assertThat(markOf(LeapYearSample.class, "asciidoc", column)).isEqualTo(markInAsciiDoc);
        assertThat(markOf(LeapYearSample.class, "markdown", column)).isEqualTo(markInMarkdown);
    }

    @DisplayName("Marks the column of a parameter annotated @Scenario, wherever it sits")
    @Description("""
            Spending the first column is a convention, not a requirement. A test that takes every
            column as a parameter can still name one of them the scenario by annotating it with
            @Scenario, and the mark follows the annotation rather than the position. Read off
            AnnotatedScenarioSample: Year | Case | Leap?, whose second parameter is annotated.
            """)
    @TableTest("""
        Scenario                          | Column | Mark in HTML? | Mark in AsciiDoc? | Mark in markdown?
        A column before the annotated one | Year   |               |                   |
        The annotated column              | Case   | scenario      | scenario          |
        The expectation is unaffected     | Leap?  | expectation   | expectation       |
        """)
    void marksTheColumnOfAnAnnotatedParameter(
            String column, String markInHtml, String markInAsciiDoc, String markInMarkdown) {
        assertThat(markOf(AnnotatedScenarioSample.class, "html", column)).isEqualTo(markInHtml);
        assertThat(markOf(AnnotatedScenarioSample.class, "asciidoc", column)).isEqualTo(markInAsciiDoc);
        assertThat(markOf(AnnotatedScenarioSample.class, "markdown", column)).isEqualTo(markInMarkdown);
    }

    @DisplayName("Marks a column as the expectation when its header matches the expectation pattern")
    @Description("""
            The question mark is only the default. The pattern is the JUnit configuration
            parameter tabletest.reporter.expectation.pattern, matched against the whole header, so
            a project that words its expectations differently can say so once. Setting it replaces
            the default rather than adding to it. Which column gets the mark is decided before any
            format renders it, so this rule is read off the HTML page alone; where each format then
            puts the mark is the rule above. Read off ExpectationPatternSample:
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
        PublishedTable published = publishedTableOfRunWith(ExpectationPatternSample.class, expectationPattern);

        assertThat("expectation".equals(published.markOf(column))).isEqualTo(holds);
    }

    @DisplayName("Publishes each row with the verdict of the scenario it ran, and a broken one's message")
    @Description("""
            The report is generated from a test run, so it publishes what happened, not only what
            was written: every cell of a row is marked with that row's verdict, and a row that
            broke is listed again below the table with the message it failed on. A spec whose rows
            are green is a spec the code still satisfies. The verdict needs somewhere to live and
            so follows the formats, but the message is published below the table by all three, in
            the same words — which is why one column serves for it. Read off LeapYearSample, whose
            century row claims the wrong answer on purpose.
            """)
    @TableTest("""
        Scenario         | Row                      | Verdict in HTML? | Verdict in AsciiDoc? | Verdict in markdown? | Message below the table?
        A row that held  | A year divisible by four | passed           | passed               |                      |
        A row that broke | A century year           | failed           | failed               |                      | ['expected: "Yes"', ' but was: "No"']
        """)
    void carriesTheVerdictOfTheScenarioItRan(
            String row,
            String verdictInHtml,
            String verdictInAsciiDoc,
            String verdictInMarkdown,
            List<String> messageBelowTheTable) {
        assertThat(verdictOf(LeapYearSample.class, "html", row)).isEqualTo(verdictInHtml);
        assertThat(verdictOf(LeapYearSample.class, "asciidoc", row)).isEqualTo(verdictInAsciiDoc);
        assertThat(verdictOf(LeapYearSample.class, "markdown", row)).isEqualTo(verdictInMarkdown);

        for (String format : List.of("html", "asciidoc", "markdown")) {
            assertThat(publishedTableOf(LeapYearSample.class, format).failureMessageOf(row))
                    .describedAs("the message published in %s", format)
                    .isEqualTo(messageBelowTheTable);
        }
    }

    private String markOf(Class<?> sampleClass, String format, String column) {
        return publishedTableOf(sampleClass, format).markOf(column);
    }

    private String verdictOf(Class<?> sampleClass, String format, String row) {
        return publishedTableOf(sampleClass, format).verdictOf(row);
    }

    private PublishedTable publishedTableOf(Class<?> sampleClass, String format) {
        return PublishedTable.of(SampleRun.outputFor(sampleClass, workingDir), format, workingDir);
    }

    /** The HTML page of a run configured with an expectation pattern of the project's own. */
    private PublishedTable publishedTableOfRunWith(Class<?> sampleClass, String expectationPattern) {
        Map<String, String> configuration = expectationPattern == null
                ? Map.of()
                : Map.of("tabletest.reporter.expectation.pattern", expectationPattern);
        return PublishedTable.of(SampleRun.outputFor(sampleClass, configuration, workingDir), "html", workingDir);
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
