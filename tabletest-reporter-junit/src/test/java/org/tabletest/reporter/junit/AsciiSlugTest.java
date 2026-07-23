package org.tabletest.reporter.junit;

import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Characterises slug generation for the naming conventions test names arrive in.
 * This informs what additional transformation logic Slugger layers on top.
 */
class AsciiSlugTest {

    @Description("""
        Pins slug generation so a change cannot alter filenames unnoticed. These rows
        were characterised against the Slugify library this replaced, and reproduce it
        exactly. The non-ASCII rows record what happens today, not what ideally should:
        diacritics fold to the base letter, but a letter with no ASCII form is dropped,
        so 'ß' and 'Æ/Ø' vanish rather than becoming 'ss'/'ae'/'oe'.
        Open: a name written wholly in a non-Latin script slugs to the empty string,
        which cannot serve as a filename — tracked as its own board item.
        """)
    @TableTest("""
        Scenario                     | Input                | Result?
        CamelCase PascalCase         | LeapYearRules        | leapyearrules
        CamelCase multi-word         | TestClassName        | testclassname
        CamelCase starting lowercase | simpleTest           | simpletest
        Acronym at beginning         | XMLParser            | xmlparser
        Acronym in middle            | parseHTMLDocument    | parsehtmldocument
        Acronym at beginning caps    | HTTPSConnection      | httpsconnection
        Underscore lowercase         | leap_year_rules      | leap_year_rules
        Underscore multi-word        | test_method_name     | test_method_name
        Underscore simple            | simple_test          | simple_test
        Spaces multiple words        | Leap Year Rules      | leap-year-rules
        Spaces with punctuation      | A Custom Test Title! | a-custom-test-title
        Spaces two words             | table test           | table-test
        Mixed underscore PascalCase  | Leap_Year_Rules      | leap_year_rules
        Mixed underscore camelCase   | TestClass_MethodName | testclass_methodname
        Mixed space camelCase        | parse HTML document  | parse-html-document
        Special char at-sign         | test@example.com     | test-example-com
        Special char percentage      | 100% coverage        | 100-coverage
        Special char colon           | user:admin           | user-admin
        Empty string                 | ''                   | ''
        Single char lowercase        | a                    | a
        Single char uppercase        | A                    | a
        Numeric only                 | 123                  | 123
        CamelCase with number        | test123Method        | test123method
        Latin diacritics             | naïve façade         | naive-facade
        German sharp s               | Grüße aus München    | grue-aus-munchen
        Nordic letters               | ÆØÅ æøå              | a-a
        Ring and umlaut              | Ångström             | angstrom
        Spanish tilde                | año español          | ano-espanol
        Greek script                 | Ελληνικά             | ''
        Cyrillic script              | Москва               | ''
        CJK script                   | 日本語のテスト       | ''
        Emoji                        | emoji 🎉 party       | emoji-party
        Em dash                      | Test — em dash       | test-em-dash
        Curly quotes                 | quotes “curly”       | quotes-curly
        Diacritics with underscore   | Ünïcödé_Mïxed        | unicode_mixed
        """)
    void shouldReduceNameToAsciiSlug(String input, String expected) {
        assertThat(AsciiSlug.of(input)).isEqualTo(expected);
    }
}
