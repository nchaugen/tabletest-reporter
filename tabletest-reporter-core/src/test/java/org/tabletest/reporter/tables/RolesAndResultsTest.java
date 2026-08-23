package org.tabletest.reporter.tables;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.junit.Description;
import org.tabletest.junit.Scenario;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.junit.Lines;
import org.tabletest.reporter.junit.TableTestPublisher;
import org.tabletest.reporter.support.PublishedTable;
import org.tabletest.reporter.support.SampleRun;

import java.nio.file.Path;
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
        A published table carries more than its text. The reporter marks which column names the
        scenario, and which column holds the expectation. It also records how each row fared when
        it ran.

        A mark needs somewhere to live, and the three formats put one in different places. HTML
        sets a class on the cell. AsciiDoc puts a role prefix on the value. Markdown has nowhere
        at all. The rules below therefore print the cell each format published, and never name the
        mark. A reader can then see the difference.

        Each rule names the sample test class it reads. Its rows are the columns, or the rows, of
        that class's table.
        """)
class RolesAndResultsTest {

    @TempDir
    Path workingDir;

    @DisplayName("Marks a column by where it sits and how its header ends")
    @Description("""
            A table can hold more columns than the test method has parameters. Its first column then
            holds the scenario name. The reporter marks that column as the scenario, and never passes
            it to the test.

            A column whose header ends in a question mark holds what the row expected. Every other
            column is an input, and carries no mark.

            These rows read LeapYearSample: Scenario | Year | Leap?, over a method taking year and
            leap.
            """)
    @TableTest("""
        Scenario                     | Column   | In markdown? | In AsciiDoc?                | In HTML?
        The column naming the row    | Scenario | Scenario     | '[.scenario]#++Scenario++#' | '<th class="cell scenario"><span class="literal">Scenario</span></th>'
        A column the test is given   | Year     | Year         | '++Year++'                  | '<th class="cell"><span class="literal">Year</span></th>'
        The column ending in a query | Leap?    | Leap?        | '[.expectation]#++Leap?++#' | '<th class="cell expectation"><span class="literal">Leap?</span></th>'
        """)
    void marksTheScenarioAndExpectationColumns(String column, String inMarkdown, String inAsciiDoc, String inHtml) {
        assertThat(headerCellOf(LeapYearSample.class, "markdown", column)).isEqualTo(inMarkdown);
        assertThat(headerCellOf(LeapYearSample.class, "asciidoc", column)).isEqualTo(inAsciiDoc);
        assertThat(headerCellOf(LeapYearSample.class, "html", column)).isEqualTo(inHtml);
    }

    @DisplayName("Follows a @Scenario annotation to the column it names")
    @Description("""
            The first column holds the scenario by convention, not by requirement. A test can take
            every column as a parameter and still name one of them the scenario. Annotate that
            parameter with @Scenario. The mark then follows the annotation, and not the position.

            These rows read AnnotatedScenarioSample: Year | Case | Leap?, whose second parameter
            carries the annotation.
            """)
    @TableTest("""
        Scenario                          | Column | In markdown? | In AsciiDoc?                | In HTML?
        A column before the annotated one | Year   | Year         | '++Year++'                  | '<th class="cell"><span class="literal">Year</span></th>'
        The annotated column              | Case   | Case         | '[.scenario]#++Case++#'     | '<th class="cell scenario"><span class="literal">Case</span></th>'
        The expectation is unaffected     | Leap?  | Leap?        | '[.expectation]#++Leap?++#' | '<th class="cell expectation"><span class="literal">Leap?</span></th>'
        """)
    void marksTheColumnOfAnAnnotatedParameter(String column, String inMarkdown, String inAsciiDoc, String inHtml) {
        assertThat(headerCellOf(AnnotatedScenarioSample.class, "markdown", column))
                .isEqualTo(inMarkdown);
        assertThat(headerCellOf(AnnotatedScenarioSample.class, "asciidoc", column))
                .isEqualTo(inAsciiDoc);
        assertThat(headerCellOf(AnnotatedScenarioSample.class, "html", column)).isEqualTo(inHtml);
    }

    @DisplayName("Chooses the expectation column by the header pattern")
    @Description("""
            The question mark is the default alone. The JUnit configuration parameter
            tabletest.reporter.expectation.pattern holds the pattern, and the reporter matches it
            against the whole header. A project that words its expectations differently therefore says
            so once.

            A pattern you set replaces the default, and does not add to it. Under a pattern of your
            own, a header ending in a question mark is an ordinary column again.

            These rows read ExpectationPatternSample: Scenario | Year | Leap? | Expected note.
            """)
    @TableTest("""
        Scenario                           | Expectation pattern | Column        | In markdown?  | In AsciiDoc?                        | In HTML?
        A query header, by default         |                     | Leap?         | Leap?         | '[.expectation]#++Leap?++#'         | '<th class="cell expectation"><span class="literal">Leap?</span></th>'
        A wordy header, by default         |                     | Expected note | Expected note | '++Expected note++'                 | '<th class="cell"><span class="literal">Expected note</span></th>'
        A query header, under your pattern | '^Expected.*'       | Leap?         | Leap?         | '++Leap?++'                         | '<th class="cell"><span class="literal">Leap?</span></th>'
        A wordy header, under your pattern | '^Expected.*'       | Expected note | Expected note | '[.expectation]#++Expected note++#' | '<th class="cell expectation"><span class="literal">Expected note</span></th>'
        """)
    void marksTheColumnMatchingTheExpectationPattern(
            String expectationPattern, String column, String inMarkdown, String inAsciiDoc, String inHtml) {
        assertThat(headerCellOfRunWith(ExpectationPatternSample.class, expectationPattern, "markdown", column))
                .isEqualTo(inMarkdown);
        assertThat(headerCellOfRunWith(ExpectationPatternSample.class, expectationPattern, "asciidoc", column))
                .isEqualTo(inAsciiDoc);
        assertThat(headerCellOfRunWith(ExpectationPatternSample.class, expectationPattern, "html", column))
                .isEqualTo(inHtml);
    }

    @DisplayName("Records a row's verdict on every cell of it")
    @Description("""
            The reporter generates the report from a test run, so the report states what happened, and
            not only what somebody wrote. Every cell of a row carries that row's verdict. A reader can
            then see whether the spec still holds.

            The Year cell stands for the whole row. The rule asserts that every cell of the row agrees
            with it.

            These rows read LeapYearSample, whose century row claims the wrong answer on purpose.
            """)
    @TableTest("""
        Scenario         | Row                      | In markdown? | In AsciiDoc?          | In HTML?
        A row that held  | A year divisible by four | 2004         | '[.passed]#++2004++#' | '<td class="cell passed"><span class="literal">2004</span></td>'
        A row that broke | A century year           | 1900         | '[.failed]#++1900++#' | '<td class="cell failed"><span class="literal">1900</span></td>'
        """)
    void marksEveryCellOfARowWithItsVerdict(String row, String inMarkdown, String inAsciiDoc, String inHtml) {
        assertThat(cellOf(LeapYearSample.class, "markdown", row)).isEqualTo(inMarkdown);
        assertThat(cellOf(LeapYearSample.class, "asciidoc", row)).isEqualTo(inAsciiDoc);
        assertThat(cellOf(LeapYearSample.class, "html", row)).isEqualTo(inHtml);

        String verdict = inHtml.contains("failed") ? "failed" : "passed";
        assertThat(verdictOf(LeapYearSample.class, "asciidoc", row)).isEqualTo(verdict);
        assertThat(verdictOf(LeapYearSample.class, "html", row)).isEqualTo(verdict);
        assertThat(verdictOf(LeapYearSample.class, "markdown", row)).isNull();
    }

    @DisplayName("Marks a set that expands the row into one run per value")
    @Description("""
            A set in a cell means one of two things. Where the parameter is not a set, the row runs
            once for each value. Where the parameter is a set, the test takes the whole set as one
            argument.

            The published table shows no parameters, so the two cells read alike. The reporter marks
            the cell that expands, and that mark is the one thing telling a reader the two apart.
            Markdown carries no marks at all, so a markdown reader cannot tell them apart.

            The AsciiDoc column holds the attribute line alone. AsciiDoc opens a bulleted block below
            a cell that holds a collection, and that line carries the marks.

            These rows read ValueSetSample: Any year | Known leap years, over a method taking an int
            and a Set.
            """)
    @TableTest("""
        Scenario                   | Column           | In markdown?   | In AsciiDoc?          | In HTML?
        A set that expands the row | Any year         | '{2004, 2008}' | '[.passed.value-set]' | '<td class="cell passed value-set"><ul class="coll set"><li><span class="literal">2004</span></li><li><span class="literal">2008</span></li></ul></td>'
        A set the test receives    | Known leap years | '{2000, 2004}' | '[.passed]'           | '<td class="cell passed"><ul class="coll set"><li><span class="literal">2000</span></li><li><span class="literal">2004</span></li></ul></td>'
        """)
    void marksASetThatExpandsTheRow(String column, String inMarkdown, String inAsciiDoc, String inHtml) {
        assertThat(columnCellOf(ValueSetSample.class, "markdown", column)).isEqualTo(inMarkdown);
        assertThat(columnCellOf(ValueSetSample.class, "asciidoc", column)).isEqualTo(inAsciiDoc);
        assertThat(columnCellOf(ValueSetSample.class, "html", column)).isEqualTo(inHtml);
    }

    @DisplayName("Publishes a broken row's message below the table")
    @Description("""
            The report lists a broken row again below the table, with the message that row failed on.
            A reader of the spec therefore meets what the code did, and not what the row claimed.

            Every format publishes that list, each below a heading of its own and fenced its own way.
            The message inside is the same text in all three, so one column serves for all three.

            These rows read LeapYearSample.
            """)
    @TableTest("""
        Scenario         | Row                      | Message below the table?
        A row that held  | A year divisible by four |
        A row that broke | A century year           | ['expected: "Yes"', ' but was: "No"']
        """)
    void publishesABrokenRowsMessageBelowTheTable(String row, @Lines List<String> messageBelowTheTable) {
        for (String format : List.of("markdown", "asciidoc", "html")) {
            assertThat(publishedTableOf(LeapYearSample.class, format).failureMessageOf(row))
                    .describedAs("the message published in %s", format)
                    .isEqualTo(messageBelowTheTable);
        }
    }

    private String headerCellOf(Class<?> sampleClass, String format, String column) {
        return publishedTableOf(sampleClass, format).headerCellOf(column);
    }

    /** The Year cell of the named row — one cell standing for a row whose cells all agree. */
    private String cellOf(Class<?> sampleClass, String format, String row) {
        return publishedTableOf(sampleClass, format).cellOf(row, "Year");
    }

    /** The named column's cell in the sample's only row. */
    private String columnCellOf(Class<?> sampleClass, String format, String column) {
        return publishedTableOf(sampleClass, format).cellOf("Years that leap", column);
    }

    private String verdictOf(Class<?> sampleClass, String format, String row) {
        return publishedTableOf(sampleClass, format).verdictOf(row);
    }

    private PublishedTable publishedTableOf(Class<?> sampleClass, String format) {
        return PublishedTable.of(SampleRun.outputFor(sampleClass, workingDir), format, workingDir);
    }

    /** The published header of a run configured with an expectation pattern of the project's own. */
    private String headerCellOfRunWith(Class<?> sampleClass, String expectationPattern, String format, String column) {
        Map<String, String> configuration = expectationPattern == null
                ? Map.of()
                : Map.of("tabletest.reporter.expectation.pattern", expectationPattern);
        return PublishedTable.of(SampleRun.outputFor(sampleClass, configuration, workingDir), format, workingDir)
                .headerCellOf(column);
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

    /** One column expands its set into runs; the other is a Set the test receives whole. */
    @ExtendWith(TableTestPublisher.class)
    static class ValueSetSample {

        @TableTest("""
            Scenario        | Any year     | Known leap years | Leap?
            Years that leap | {2004, 2008} | {2000, 2004}     | Yes
            """)
        void leapYears(int year, Set<Integer> knownLeapYears, String leap) {
            assertThat(knownLeapYears.contains(year) || isLeap(year) ? "Yes" : "No")
                    .isEqualTo(leap);
        }
    }
}
