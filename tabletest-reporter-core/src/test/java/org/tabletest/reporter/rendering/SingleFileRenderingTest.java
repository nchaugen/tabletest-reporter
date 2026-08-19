package org.tabletest.reporter.rendering;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.tabletest.reporter.BuiltInFormat.HTML;

/**
 * The single-file mode rules, over a report of one class with two tables. {@link PublishedReport}
 * generates it in either mode, so a rule can contrast what each one writes.
 */
@DisplayName("Single-file mode")
@Description("""
        A report is normally a directory of pages. Single-file mode collapses it into one
        self-contained HTML file that can be mailed, attached to a build, or opened from a memory
        stick with nothing beside it. The rules below are read off a report built from one test
        class, org.example.CalendarTest, with two tables — leapYear and monthLength.
        """)
class SingleFileRenderingTest {

    /** The report the rules below are read off. */
    private static final List<String> PUBLISHED_TABLES =
            List.of("org.example.CalendarTest#leapYear", "org.example.CalendarTest#monthLength");

    @TempDir
    Path workingDir;

    @DisplayName("Single-file mode writes one file where the default writes a tree")
    @Description("""
            The default report writes a page per index and per table, plus the search index as a
            script beside them. Single-file mode writes index.html and nothing else — the same
            report, with every page and the search index folded into it.
            """)
    @TableTest("""
        Scenario           | Single file | Files written?
        The default report | false       | ['calendar-test/index.html', 'calendar-test/leap-year.html', 'calendar-test/month-length.html', 'index.html', 'tabletest-search-index.js']
        Single-file mode   | true        | ['index.html']
        """)
    void writesOneFileWhereTheDefaultWritesATree(boolean singleFile, List<String> filesWritten) {
        Path output = PublishedReport.outputOf("html", singleFile, PUBLISHED_TABLES, workingDir);

        assertThat(PublishedReport.filesIn(output)).isEqualTo(filesWritten);
    }

    @DisplayName("The one file reaches for nothing outside itself")
    @Description("""
            Everything the report needs is in the file: no stylesheet or script is linked, no
            address is fetched, and the search index that is a sibling script in the default
            report is inlined instead. This is what makes the file work offline and survive being
            moved on its own.
            """)
    @TableTest("""
        Scenario                           | Text                          | Found in the file?
        An insecure address                | 'http://'                     | false
        A secure address                   | 'https://'                    | false
        A linked script                    | '<script src='                | false
        The search index as a sibling file | 'tabletest-search-index.js'   | false
        The search index, inlined          | 'window.TableTestSearchIndex' | true
        """)
    void reachesForNothingOutsideItself(String text, boolean foundInTheFile) {
        assertThat(singleFileText().contains(text)).isEqualTo(foundInTheFile);
    }

    @DisplayName("Every table becomes a section the report's own links point at")
    @Description("""
            A table that was a page of its own becomes a section of the one file, named after the
            path it had. The sidebar and the search results point at that name as an in-page
            anchor, so navigation that crossed files in the default report scrolls within this one.
            """)
    @TableTest("""
        Scenario         | Page URL                    | Section holding the table?  | Link to it from the sidebar?
        The first table  | /calendar-test/leap-year    | calendar-test__leap-year    | #calendar-test__leap-year
        The second table | /calendar-test/month-length | calendar-test__month-length | #calendar-test__month-length
        """)
    void becomesASectionTheReportsOwnLinksPointAt(String pageUrl, String section, String link) {
        assertThat(PublishedReport.pageAt(pageUrl, PUBLISHED_TABLES, workingDir).select("table"))
                .isNotEmpty();

        Document singleFile = singleFilePage();

        assertThat(singleFile.select("section#" + section + " table")).isNotEmpty();
        assertThat(singleFile.select("aside.sidebar .nav-item.table > a").eachAttr("href"))
                .contains(link);
    }

    @DisplayName("A section is headed at its depth in the report, and no deeper than six")
    @Description("""
            The one file has to carry the outline the tree carried, so a section is headed one
            level below the section it sits in. HTML stops at h6, so a report deep enough to need
            an h7 keeps its deepest sections at h6 rather than emitting a heading no browser
            knows. The second class below keeps the report's root shallow, so the packages named
            in each row are levels the reader walks down.
            """)
    @TableTest("""
        Scenario                      | Published tables                                                                | Section heading?
        The class in the root package | ['org.example.CalendarTest#leapYear', 'org.example.other.Trivia#facts']         | h3
        One package deeper            | ['org.example.a.CalendarTest#leapYear', 'org.example.other.Trivia#facts']       | h4
        Two packages deeper           | ['org.example.a.b.CalendarTest#leapYear', 'org.example.other.Trivia#facts']     | h5
        Three packages deeper         | ['org.example.a.b.c.CalendarTest#leapYear', 'org.example.other.Trivia#facts']   | h6
        Four packages deeper, past h6 | ['org.example.a.b.c.d.CalendarTest#leapYear', 'org.example.other.Trivia#facts'] | h6
        """)
    void headsASectionAtItsDepthAndNoDeeperThanSix(List<String> publishedTables, String sectionHeading) {
        assertThat(sectionHeadingOf(singleFilePageFor(publishedTables))).isEqualTo(sectionHeading);
    }

    @DisplayName("Single-file mode is offered for HTML only")
    @Description("""
            The other formats have no way to be self-contained — an AsciiDoc or markdown page
            carries its structure in files and links, not in one document — so asking for a single
            file in either is refused rather than quietly ignored, and the message names the
            format that was asked for.
            """)
    @TableTest("""
        Scenario         | Format   | Error message?
        The one that can | html     |
        AsciiDoc cannot  | asciidoc | Single-file mode is currently supported only for the html format, not asciidoc
        Markdown cannot  | markdown | Single-file mode is currently supported only for the html format, not markdown
        """)
    void isOfferedForHtmlOnly(String format, String errorMessage) {
        Throwable thrown = catchThrowable(() -> PublishedReport.outputOf(format, true, PUBLISHED_TABLES, workingDir));

        assertThat(thrown == null ? null : thrown.getMessage()).isEqualTo(errorMessage);
    }

    /** The heading element the leap-year section is titled with, whatever its depth. */
    private static String sectionHeadingOf(Document singleFile) {
        Element title =
                singleFile.select("section[id$=__leap-year] > .section-title").first();
        return title == null ? null : title.tagName();
    }

    private String singleFileText() {
        return PublishedReport.textOf(singleFileOf(PUBLISHED_TABLES));
    }

    private Document singleFilePage() {
        return singleFilePageFor(PUBLISHED_TABLES);
    }

    private Document singleFilePageFor(List<String> publishedTables) {
        return HtmlValidator.parse(PublishedReport.textOf(singleFileOf(publishedTables)));
    }

    private Path singleFileOf(List<String> publishedTables) {
        return PublishedReport.outputOf("html", true, publishedTables, workingDir)
                .resolve("index" + HTML.extension());
    }

    /**
     * Conformance rather than a rule: a failing row and its message belong to the row-roles
     * feature, and the fixture has to declare the roles and results a real run would have
     * recorded. Kept here because it is the single-file assembly of them that is at stake — the
     * failures block must sit inside the section's own outline, never jump back up to an h2.
     */
    @Test
    void inlines_failing_verdict_and_broken_scenarios_for_failed_tables() throws IOException {
        Document doc = HtmlValidator.parse(Files.readString(generateFrom(failingFixture())));

        assertThat(doc.select("section.report-section.table.failed")).isNotEmpty();
        assertThat(doc.select("p.verdict.fail").text()).contains("1 of 2 scenarios broken");
        assertThat(doc.select("tr.failed-row")).isNotEmpty();
        assertThat(doc.select("section.failures details summary").text()).contains("1900 is not leap");
        assertThat(doc.select("section.failures pre").text()).contains("expected: <No> but was: <Yes>");
        assertThat(doc.select("h2:contains(Broken scenarios)")).isEmpty();
        assertThat(doc.select("section.failures > h6").text()).contains("Broken scenarios");
    }

    private Path generateFrom(Path inDir) throws IOException {
        Path outDir = Files.createTempDirectory(workingDir, "out");
        new org.tabletest.reporter.TableTestReporter().report(HTML, inDir, outDir, true);
        return outDir.resolve("index.html");
    }

    /** A tree deep enough to clamp the heading, with a table holding one broken scenario. */
    private Path failingFixture() throws IOException {
        Path inDir = Files.createDirectories(workingDir.resolve("in-failing"));
        Path classDir = Files.createDirectories(inDir.resolve("org.example.deep.a.b.c.CalendarCalculations"));
        Files.writeString(classDir.resolve("TABLETEST-calendar-calculations.yaml"), """
            "className": "org.example.deep.a.b.c.CalendarCalculations"
            "slug": "calendar-calculations"
            "title": "Calendar"
            "tableTests":
              - "path": "leapYear(int)/TABLETEST-leap-year-rules.yaml"
                "methodName": "leapYear"
                "slug": "leap-year-rules"
            """);
        Path leapDir = Files.createDirectories(classDir.resolve("leapYear(int)"));
        Files.writeString(leapDir.resolve("TABLETEST-leap-year-rules.yaml"), """
            "title": "Leap Year Rules"
            "headers":
              - "value": "Year"
              - "value": "Is Leap Year?"
                "roles": ["expectation"]
            "rows":
              - - "value": "2004"
                - "value": "Yes"
                  "roles": ["expectation", "passed"]
              - - "value": "1900"
                - "value": "Yes"
                  "roles": ["expectation", "failed"]
            "rowResults":
              - "rowIndex": 1
                "passed": true
                "displayName": "[1] 2004 is leap"
              - "rowIndex": 2
                "passed": false
                "displayName": "[2] 1900 is not leap"
                "errorMessage": "expected: <No> but was: <Yes>"
            """);

        Path triviaDir = Files.createDirectories(inDir.resolve("org.example.other.Trivia"));
        Files.writeString(triviaDir.resolve("TABLETEST-trivia.yaml"), """
            "className": "org.example.other.Trivia"
            "slug": "trivia"
            "title": "Trivia"
            "tableTests":
              - "path": "facts()/TABLETEST-facts.yaml"
                "methodName": "facts"
                "slug": "facts"
            """);
        Path factsDir = Files.createDirectories(triviaDir.resolve("facts()"));
        Files.writeString(factsDir.resolve("TABLETEST-facts.yaml"), """
            "title": "Facts"
            "headers":
              - "value": "Fact"
            "rows":
              - - "value": "Earth orbits the Sun"
            """);
        return inDir;
    }
}
