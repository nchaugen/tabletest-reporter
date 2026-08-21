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
            letters is one word, so an acronym survives. A trailing capital run that ends the name
            joins the word before it, which is why getHTTPSURL reads as it does.

            An underscore separates words the same way, and a name already written with spaces is
            left alone.
            """)
    @TableTest("""
        Name              | Title?
        LeapYearRules     | Leap Year Rules
        XMLParser         | XML Parser
        parseHTMLDocument | Parse HTML Document
        HTTPSConnection   | HTTPS Connection
        simpleTest        | Simple Test
        A                 | A
        AB                | AB
        ABC               | ABC
        AbcDef            | Abc Def
        ABCDef            | ABC Def
        MyHTTPServer      | My HTTP Server
        getHTTPSURL       | Get HTTPSURL
        IOError           | IO Error
        SimpleClassName   | Simple Class Name
        snake_name        | Snake name
        name with spaces  | name with spaces
        ""                | ""
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
        """)
    @TableTest("""
        Scenario                     | Name                    | URL?
        CamelCase PascalCase         | LeapYearRules           | leap-year-rules
        CamelCase multi-word         | TestClassName           | test-class-name
        CamelCase starting lowercase | simpleTest              | simple-test
        Acronym at beginning         | XMLParser               | xml-parser
        Acronym in middle            | parseHTMLDocument       | parse-html-document
        Acronym at beginning caps    | HTTPSConnection         | https-connection
        Acronym to acronym           | URLToHTMLConverter      | url-to-html-converter
        Snake_case lowercase         | leap_year_rules         | leap-year-rules
        Snake_case multi-word        | test_method_name        | test-method-name
        Snake_case simple            | simple_test             | simple-test
        Spaces multiple words        | Leap Year Rules         | leap-year-rules
        Spaces with punctuation      | A Custom Test Title!    | a-custom-test-title
        Spaces two words             | table test              | table-test
        Spaces title case            | User Authentication     | user-authentication
        Special char at-sign         | test@example.com        | test-example-com
        Special char percentage      | 100% coverage           | 100-coverage
        Special char colon           | user:admin              | user-admin
        Single word uppercase        | Test                    | test
        Single word lowercase        | test                    | test
        Single word acronym          | XML                     | xml
        Empty string                 | ''                      | ''
        Single char lowercase        | a                       | a
        Single char uppercase        | A                       | a
        Numeric only                 | 123                     | 123
        CamelCase with number inline | test123Method           | test123-method
        Snake_case with number       | test_123_method         | test-123-method
        Acronym with number          | UTF8Encoder             | utf8-encoder
        CamelCase number at start    | base64Encode            | base64-encode
        Mixed space and underscore   | test_method with spaces | test-method-with-spaces
        Mixed snake and camel        | Test_Method_Name        | test-method-name
        Mixed snake and acronym      | XML_Parser              | xml-parser
        Accented letters             | naïve façade            | naive-facade
        Ligature letters             | Grüße aus München       | grusse-aus-munchen
        Nordic letters               | ÆØÅ                     | aeoa
        Thorn without Latin base     | Þingvellir              | thingvellir
        Compatibility ligature       | ﬁle ﬂow                 | file-flow
        Greek script                 | Ελληνικά                | ελληνικά
        Cyrillic script              | Москва                  | москва
        CJK script                   | 日本語のテスト          | 日本語のテスト
        Cyrillic backtick name       | Москва основана в 1147  | москва-основана-в-1147
        Cyrillic camelCase           | проверкаИмени           | проверка-имени
        No letters or digits         | '!!!'                   | unnamed-00008001
        """)
    void turnsANameIntoTheUrlOfItsPage(String name, String url) {
        assertThat(Slugger.slugify(name)).isEqualTo(url);
    }
}
