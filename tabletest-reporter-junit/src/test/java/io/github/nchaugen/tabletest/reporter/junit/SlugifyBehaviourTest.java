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
        """)
    void shouldDocumentSlugifyBehaviour(String input, String expected) {
        assertThat(SLUGIFIER.slugify(input)).isEqualTo(expected);
    }
}
