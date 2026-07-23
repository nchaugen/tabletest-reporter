package org.tabletest.reporter.junit;

import com.github.slugify.Slugify;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Documents the behaviour of the Slugify library with different naming conventions.
 * This informs what additional transformation logic we need for filename generation.
 */
class SlugifyBehaviourTest {

    private static final Slugify SLUGIFIER = Slugify.builder().build();

    @Description("""
        Characterises the Slugify library so a version change cannot alter filename
        generation unnoticed. The non-ASCII rows record what the library does today,
        not what it ideally should: Slugify strips diacritics but drops letters it has
        no ASCII fold for, so 'ß' and 'Æ/Ø' vanish rather than becoming 'ss'/'ae'/'oe'.
        Open: a name written wholly in a non-Latin script slugs to the empty string,
        which cannot serve as a filename — see the note on Slugger.
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
    void shouldDocumentSlugifyBehaviour(String input, String expected) {
        assertThat(SLUGIFIER.slugify(input)).isEqualTo(expected);
    }
}
