package org.tabletest.reporter.formats;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.junit.Lines;
import org.tabletest.reporter.junit.NamedLines;
import org.tabletest.reporter.junit.TableTestPublisher;
import org.tabletest.reporter.support.PublishedReport;
import org.tabletest.reporter.support.SampleRun;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The custom template rules, read off a real report of {@link CalendarSample} generated with a
 * template directory of one file.
 */
@DisplayName("Custom templates")
@Description("""
        Pebble templates render a report, and a project can supply its own. Point the reporter at
        a template directory, and the reporter uses the files in it in place of, or on top of,
        the built-in templates.

        The rules below are read off a report of one test class named Calendar. That class holds
        one table named Leap years, with the columns Year and Leap? and one row, 2004 and Yes. A
        report that small lets a rule state a whole page.
        """)
class CustomTemplateTest {

    @TempDir
    Path workingDir;

    @DisplayName("Uses your template when its file name matches the page")
    @Description("""
            One template renders one page in one format. The reporter has seven slots for them:
            a table and an index page in markdown, in asciidoc and in html, and the single-file
            html page. The rows below use the markdown slots.

            Reach for the hyphen form when your template extends the built-in one, because a
            template cannot extend itself.
            """)
    @TableTest("""
        Scenario                     | Your template directory                                          | Table page?                                                                  | Index page?
        No template of your own      | [:]                                                              | ['## Leap years', '', '| Year | Leap? |', '| --- | --- |', '| 2004 | Yes |'] | ['# Calendar', '', '* [Leap years](./leap-years.md)']
        The name of the table page   | [table.md.peb: ['# {{ title }} of note', 'Written by hand.']]    | ['# Leap years of note', 'Written by hand.']                                 | ['# Calendar', '', '* [Leap years](./leap-years.md)']
        A hyphen before that name    | [my-table.md.peb: ['# {{ title }} of note', 'Written by hand.']] | ['# Leap years of note', 'Written by hand.']                                 | ['# Calendar', '', '* [Leap years](./leap-years.md)']
        That name without the hyphen | [mytable.md.peb: ['# {{ title }} of note', 'Written by hand.']]  | ['## Leap years', '', '| Year | Leap? |', '| --- | --- |', '| 2004 | Yes |'] | ['# Calendar', '', '* [Leap years](./leap-years.md)']
        The name of the index page   | [index.md.peb: ['# {{ title }} of note', 'Written by hand.']]    | ['## Leap years', '', '| Year | Leap? |', '| --- | --- |', '| 2004 | Yes |'] | ['# Calendar of note', 'Written by hand.']
        """)
    void usesATemplateNamedForThePageItRenders(
            @NamedLines Map<String, List<String>> yourTemplateDirectory,
            @Lines List<String> tablePage,
            @Lines List<String> indexPage) {
        Path templates = templateDirectoryHolding(yourTemplateDirectory);

        assertThat(tablePageWith(templates, "markdown")).isEqualTo(tablePage);
        assertThat(indexPageWith(templates, "markdown")).isEqualTo(indexPage);
    }

    @DisplayName("Picks one template when several could render the page")
    @Description("""
            A directory can hold more than one candidate for the same page. The reporter has to
            settle on one, and it does so the same way every run, so a report never changes because
            a file was added beside another.

            The alphabetical part carries no meaning of its own — it is there to make the choice
            deterministic, not to give an ordering you should design around. A directory holding two
            templates for one page is usually a mistake rather than a decision.
            """)
    @TableTest("""
        Scenario                           | Your template directory                                      | Table page?
        Two names that both match the page | [b-table.md.peb: ['# From B'], a-table.md.peb: ['# From A']] | ['# From A']
        The page's own name beside a match | [table.md.peb: ['# Exact'], a-table.md.peb: ['# From A']]    | ['# Exact']
        """)
    void picks_one_template_when_several_could_render_the_page(
            @NamedLines Map<String, List<String>> yourTemplateDirectory, @Lines List<String> tablePage) {
        Path templates = templateDirectoryHolding(yourTemplateDirectory);

        assertThat(tablePageWith(templates, "markdown")).isEqualTo(tablePage);
    }

    @DisplayName("Lets your template extend a built-in one and fill its blocks")
    @Description("""
            Replacing a template means rewriting the page. Extending one means naming the built-in
            template and filling only the blocks it leaves open. The reporter then keeps rendering
            the table itself.

            There are three such blocks: frontMatter above the page, title in place of the generated
            heading, and footer below it.

            A template that fills one block reads:

            {% extends "table.md.peb" %}
            {% block frontMatter %}
            …
            {% endblock %}
            """)
    @TableTest("""
        Scenario                    | Block       | Filled with                      | Page?
        Front matter above the page | frontMatter | ['---', 'layout: report', '---'] | ['---', 'layout: report', '---', '## Leap years', '', '| Year | Leap? |', '| --- | --- |', '| 2004 | Yes |']
        A heading of your own       | title       | ['# {{ title }} of note']        | ['# Leap years of note', '', '', '| Year | Leap? |', '| --- | --- |', '| 2004 | Yes |']
        A footer below the page     | footer      | ['', '_Generated by us_']        | ['## Leap years', '', '| Year | Leap? |', '| --- | --- |', '| 2004 | Yes |', '', '_Generated by us_']
        """)
    void fillsTheBlocksTheBuiltInTemplateLeaves(
            String block, @Lines List<String> filledWith, @Lines List<String> page) {
        Path templates = templateDirectoryHolding(
                "my-table.md.peb", extending("table.md.peb", block, String.join("\n", filledWith)));

        assertThat(tablePageWith(templates, "markdown")).isEqualTo(page);
    }

    @DisplayName("Leaves the same three blocks in every built-in template")
    @Description("""
            The blocks are not a property of one template. A table page and an index page leave the
            same three, in every format the reporter generates.

            The one template below is written once per format and page, filling all three, and lands
            in the same places each time. Its title block holds {{ title }} of note, with no markup
            of its own. The same line can therefore be looked for whichever format the page is
            written in.
            """)
    @TableTest("""
        Scenario             | Format   | Page  | Opens with?                      | Titled?            | Ends with?
        A table in markdown  | markdown | table | ['---', 'layout: report', '---'] | Leap years of note | _Generated by us_
        A table in asciidoc  | asciidoc | table | ['---', 'layout: report', '---'] | Leap years of note | _Generated by us_
        An index in markdown | markdown | index | ['---', 'layout: report', '---'] | Calendar of note   | _Generated by us_
        An index in asciidoc | asciidoc | index | ['---', 'layout: report', '---'] | Calendar of note   | _Generated by us_
        """)
    void leavesTheSameThreeBlocksInEveryTemplate(
            String format, String page, @Lines List<String> opensWith, String titled, String endsWith) {
        String builtIn = page + "." + extensionOf(format) + ".peb";
        Path templates = templateDirectoryHolding("my-" + builtIn, extendingAllThreeBlocksOf(builtIn));

        List<String> lines = pageWith(templates, format, page.equals("index"));

        assertThat(lines.subList(0, opensWith.size())).isEqualTo(opensWith);
        assertThat(lines).contains(titled);
        assertThat(lines.getLast()).isEqualTo(endsWith);
    }

    @DisplayName("Lets your template add to the built-in stylesheet")
    @Description("""
            The HTML report carries its stylesheet inside the file, so a role a test declares has
            nowhere to be styled from. The extra_stylesheet block is that place: what it holds is
            written after the built-in stylesheet, which stays where it is.

            Read off a report of BinSample, whose Bins column is a list.
            """)
    @TableTest("""
        Scenario                | Extra stylesheet        | Your rule in the page? | Built-in stylesheet kept?
        No template of your own |                         | false                  | true
        A rule of your own      | '.cell { color: red; }' | true                   | true
        """)
    void addsToTheBuiltInStylesheet(String extraStylesheet, boolean yourRuleInThePage, boolean builtInKept) {
        List<String> page = htmlPageWith(extraStylesheet);

        assertThat(page.stream().anyMatch(line -> line.contains("color: red"))).isEqualTo(yourRuleInThePage);
        assertThat(page.stream().anyMatch(line -> line.contains("--font-mono"))).isEqualTo(builtInKept);
    }

    /** The HTML table page of BinSample, rendered with the extra_stylesheet block filled. */
    private List<String> htmlPageWith(String extraStylesheet) {
        Path templates = extraStylesheet == null
                ? null
                : templateDirectoryHolding(
                        "my-table.html.peb", extending("table.html.peb", "extra_stylesheet", extraStylesheet));
        return PublishedReport.pageLinesOf(
                SampleRun.outputFor(BinSample.class, workingDir), "html", templates, false, workingDir);
    }

    private static String extensionOf(String format) {
        return format.equals("markdown") ? "md" : "adoc";
    }

    /** A template naming a built-in one and filling one of the blocks it leaves. */
    private static String extending(String builtInTemplate, String blockName, String filledWith) {
        return extendsLine(builtInTemplate) + block(blockName, filledWith);
    }

    /** The same, filling all three blocks the built-in templates leave. */
    private static String extendingAllThreeBlocksOf(String builtInTemplate) {
        return extendsLine(builtInTemplate)
                + block("frontMatter", "---\nlayout: report\n---")
                + block("title", "{{ title }} of note")
                + block("footer", "\n_Generated by us_");
    }

    private static String extendsLine(String builtInTemplate) {
        return "{%% extends \"%s\" %%}\n".formatted(builtInTemplate);
    }

    private static String block(String blockName, String filledWith) {
        return """
            {%% block %s %%}
            %s
            {%% endblock %%}
            """.formatted(blockName, filledWith);
    }

    /** A directory holding the files a row shows, or no directory at all when it shows none. */
    private Path templateDirectoryHolding(Map<String, List<String>> files) {
        if (files.isEmpty()) {
            return null;
        }
        try {
            Path directory = Files.createTempDirectory(workingDir, "templates");
            for (Map.Entry<String, List<String>> file : files.entrySet()) {
                Files.writeString(directory.resolve(file.getKey()), String.join("\n", file.getValue()));
            }
            return directory;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The directory holding one template file, for the rules that name a single file inline. */
    private Path templateDirectoryHolding(String fileName, String content) {
        return templateDirectoryHolding(Map.of(fileName, List.of(content)));
    }

    private List<String> tablePageWith(Path templates, String format) {
        return pageWith(templates, format, false);
    }

    private List<String> indexPageWith(Path templates, String format) {
        return pageWith(templates, format, true);
    }

    private List<String> pageWith(Path templates, String format, boolean index) {
        return PublishedReport.pageLinesOf(
                SampleRun.outputFor(CalendarSample.class, workingDir), format, templates, index, workingDir);
    }

    /**
     * Run only through {@link SampleRun} — a static nested class is not picked up by the
     * surrounding test run, so it publishes no page of its own.
     */
    @DisplayName("Calendar")
    @ExtendWith(TableTestPublisher.class)
    static class CalendarSample {

        @DisplayName("Leap years")
        @TableTest("""
            Year | Leap?
            2004 | Yes
            """)
        void leapYears(int year, String leap) {
            assertThat(year % 4 == 0 ? "Yes" : "No").isEqualTo(leap);
        }
    }

    /** A table with a list cell and a cell whose whitespace is significant. */
    @DisplayName("Bins")
    @ExtendWith(TableTestPublisher.class)
    static class BinSample {

        @DisplayName("Bins by waste")
        @TableTest("""
            Waste  | Bins?
            'a  b' | [paper, glass]
            """)
        void bins(String waste, List<String> bins) {
            assertThat(bins).hasSize(2);
        }
    }
}
