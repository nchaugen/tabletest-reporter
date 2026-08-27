package org.tabletest.reporter.structure;

import org.junit.jupiter.api.DisplayName;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.junit.Slugger;
import org.tabletest.reporter.junit.TitleTransformer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The naming rules, read against the public API the extension names a page with. Lives in the
 * report-structure test package rather than beside {@link Slugger} in the extension's own, so
 * these rules join the structure group of the published spec instead of forming a group of their
 * own. Both classes under test are public, so the package is free to choose.
 */
@DisplayName("Page names")
@Description("""
        A page needs two names: the title a reader sees, and the file name the URL ends in. Both
        come from the test that produced the page. A class or method that carries a @DisplayName
        supplies its own title, and the rules below are what happens when it does not.
        """)
class PageNamingTest {

    @DisplayName("Titles a page after the test that made it")
    @Description("""
            The name is read as words and written back with a space between them. A run of capital
            letters is one word, so an acronym survives. A run that ends the name joins the word
            before it, which is why getHTTPSURL reads as it does.

            Only the first letter of the title is capitalised. No other letter changes case, which
            is why the underscore name reads Snake name and not Snake Name. A name already written
            with spaces is left alone.
            """)
    @TableTest("""
        Name              | Title?
        LeapYearRules     | Leap Year Rules
        simpleTest        | Simple Test
        XMLParser         | XML Parser
        parseHTMLDocument | Parse HTML Document
        getHTTPSURL       | Get HTTPSURL
        test123Method     | Test123 Method
        A                 | A
        ABC               | ABC
        snake_name        | Snake name
        name with spaces  | name with spaces
        ''                | ''
                          |
        """)
    void titlesAPageAfterTheTestThatMadeIt(String name, String title) {
        assertThat(TitleTransformer.toTitle(name)).isEqualTo(title);
    }

    @DisplayName("Turns a name into the URL of its page")
    @Description("""
        A name that keeps a letter through the ASCII fold slugs exactly as it always has.
        A name that does not — one written in a script the fold has no answer for — keeps
        its own characters instead of collapsing to an unusable empty filename, and a name
        with no letters or digits anywhere falls back to a stable hash so that two of them
        still get two filenames.

        Words are separated before the fold runs. A name that holds a space or an underscore
        is separated there and is not also split at its capitals.
        """)
    @TableTest("""
        Scenario                    | Name                    | URL?
        CamelCase                   | LeapYearRules           | leap-year-rules
        An acronym beside a word    | parseHTMLDocument       | parse-html-document
        A digit before a word       | test123Method           | test123-method
        A single letter             | A                       | a
        Snake case                  | leap_year_rules         | leap-year-rules
        Snake case with capitals    | TestClass_MethodName    | testclass-methodname
        Spaces                      | Leap Year Rules         | leap-year-rules
        Spaces and underscores      | test_method with spaces | test-method-with-spaces
        Punctuation inside the name | test@example.com        | test-example-com
        Punctuation ending the name | A Custom Test Title!    | a-custom-test-title
        Accented letters            | naïve façade            | naive-facade
        Ligature letters            | Grüße aus München       | grusse-aus-munchen
        Nordic letters              | ÆØÅ                     | aeoa
        Thorn without Latin base    | Þingvellir              | thingvellir
        Compatibility ligature      | ﬁle ﬂow                 | file-flow
        Greek script                | Ελληνικά                | ελληνικά
        Cyrillic script             | Москва                  | москва
        CJK script                  | 日本語のテスト          | 日本語のテスト
        Cyrillic camelCase          | проверкаИмени           | проверка-имени
        Digits and no letters       | 123                     | 123
        No letters or digits        | '!!!'                   | unnamed-00008001
        Another with neither        | '???'                   | unnamed-0000f45f
        An empty name               | ''                      | ''
        No name at all              |                         |
        """)
    void turnsANameIntoTheUrlOfItsPage(String name, String url) {
        assertThat(Slugger.slugify(name)).isEqualTo(url);
    }
}
