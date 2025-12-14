package io.github.nchaugen.tabletest.reporter.junit;

import com.github.slugify.Slugify;
import io.github.nchaugen.tabletest.junit.TableTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Documents the behaviour of the Slugify library with different naming conventions.
 * This informs what additional transformation logic we need for filename generation.
 */
class SlugifyBehaviourTest {

    private static final Slugify SLUGIFIER = Slugify.builder().build();

    @TableTest("""
        Scenario                     | Input                 | Result?
        camelCase PascalCase         | LeapYearRules         | leapyearrules
        camelCase multi-word         | TestClassName         | testclassname
        camelCase starting lowercase | simpleTest            | simpletest
        acronym at beginning         | XMLParser             | xmlparser
        acronym in middle            | parseHTMLDocument     | parsehtmldocument
        acronym at beginning caps    | HTTPSConnection       | httpsconnection
        underscore lowercase         | leap_year_rules       | leap_year_rules
        underscore multi-word        | test_method_name      | test_method_name
        underscore simple            | simple_test           | simple_test
        spaces multiple words        | Leap Year Rules       | leap-year-rules
        spaces with punctuation      | A Custom Test Title!  | a-custom-test-title
        spaces two words             | table test            | table-test
        mixed underscore PascalCase  | Leap_Year_Rules       | leap_year_rules
        mixed underscore camelCase   | TestClass_MethodName  | testclass_methodname
        mixed space camelCase        | parse HTML document   | parse-html-document
        special char at-sign         | test@example.com      | test-example-com
        special char percentage      | 100% coverage         | 100-coverage
        special char colon           | user:admin            | user-admin
        empty string                 | ''                    | ''
        single char lowercase        | a                     | a
        single char uppercase        | A                     | a
        numeric only                 | 123                   | 123
        camelCase with number        | test123Method         | test123method
        """)
    void shouldDocumentSlugifyBehaviour(String input, String expected) {
        assertThat(SLUGIFIER.slugify(input)).isEqualTo(expected);
    }
}
