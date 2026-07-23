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
        Pins slug generation so a change cannot alter filenames unnoticed. Most rows were
        characterised against the Slugify library this replaced and reproduce it exactly.
        The exceptions are the letters Slugify dropped for want of an ASCII form: those now
        follow one rule — ligatures expand to their component letters, stroked letters fold
        to their base letter, and 'þ' takes a digraph because it has no Latin base at all.
        Letters that already fold under NFD ('ü ö ä é å ñ') are deliberately left alone,
        since re-mapping them would move slugs that already work as published URLs.
        Compatibility forms — 'ﬁ', fullwidth letters, superscripts, circled and Roman
        numerals — now reduce to the characters they stand for rather than being dropped.
        A symbol standing for letters lands as those letters with no word break of its own,
        which is why 'Widget™ test' reads 'widgettm-test'.
        A name written wholly in a non-Latin script still reduces to the empty string here;
        Slugger layers a fallback above this fold rather than teaching the fold to handle it.
        """)
    @TableTest("""
        Scenario                     | Input                 | Result?
        CamelCase PascalCase         | LeapYearRules         | leapyearrules
        CamelCase multi-word         | TestClassName         | testclassname
        CamelCase starting lowercase | simpleTest            | simpletest
        Acronym at beginning         | XMLParser             | xmlparser
        Acronym in middle            | parseHTMLDocument     | parsehtmldocument
        Acronym at beginning caps    | HTTPSConnection       | httpsconnection
        Underscore lowercase         | leap_year_rules       | leap_year_rules
        Underscore multi-word        | test_method_name      | test_method_name
        Underscore simple            | simple_test           | simple_test
        Spaces multiple words        | Leap Year Rules       | leap-year-rules
        Spaces with punctuation      | A Custom Test Title!  | a-custom-test-title
        Spaces two words             | table test            | table-test
        Mixed underscore PascalCase  | Leap_Year_Rules       | leap_year_rules
        Mixed underscore camelCase   | TestClass_MethodName  | testclass_methodname
        Mixed space camelCase        | parse HTML document   | parse-html-document
        Special char at-sign         | test@example.com      | test-example-com
        Special char percentage      | 100% coverage         | 100-coverage
        Special char colon           | user:admin            | user-admin
        Empty string                 | ''                    | ''
        Single char lowercase        | a                     | a
        Single char uppercase        | A                     | a
        Numeric only                 | 123                   | 123
        CamelCase with number        | test123Method         | test123method
        Latin diacritics             | naïve façade          | naive-facade
        Ring and umlaut              | Ångström              | angstrom
        Spanish tilde                | año español           | ano-espanol
        Ligature sharp s             | Grüße aus München     | grusse-aus-munchen
        Ligature ae and oe           | Cœur Æther            | coeur-aether
        Nordic letters               | ÆØÅ æøå               | aeoa-aeoa
        Stroked l                    | Łódź Wrocław          | lodz-wroclaw
        Stroked d                    | Đakovo                | dakovo
        Thorn and eth                | Þingvellir Norðurland | thingvellir-nordurland
        Accented ligature            | Ǽgir Ǿrn              | aegir-orn
        Ligature fi and fl           | ﬁle ﬂow               | file-flow
        Fullwidth letters            | Ｆｕｌｌｗｉｄｔｈ    | fullwidth
        Superscript digit            | x² area               | x2-area
        Circled digit                | ① first               | 1-first
        Roman numeral                | Chapter Ⅻ             | chapter-xii
        Trademark sign               | Widget™ test          | widgettm-test
        Vulgar fraction              | ½ cup                 | 12-cup
        Greek script                 | Ελληνικά              | ''
        Cyrillic script              | Москва                | ''
        CJK script                   | 日本語のテスト        | ''
        Emoji                        | emoji 🎉 party        | emoji-party
        Em dash                      | Test — em dash        | test-em-dash
        Curly quotes                 | quotes “curly”        | quotes-curly
        Diacritics with underscore   | Ünïcödé_Mïxed         | unicode_mixed
        """)
    void shouldReduceNameToAsciiSlug(String input, String expected) {
        assertThat(AsciiSlug.of(input)).isEqualTo(expected);
    }
}
