package io.github.nchaugen.tabletest.reporter.junit;

import org.tabletest.junit.TableTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the Slugger which converts test class and method names
 * to web-friendly kebab-case filenames.
 */
class SluggerTest {

    @TableTest("""
        Scenario                     | Input                       | Result?
        CamelCase PascalCase         | LeapYearRules               | leap-year-rules
        CamelCase multi-word         | TestClassName               | test-class-name
        CamelCase starting lowercase | simpleTest                  | simple-test
        Acronym at beginning         | XMLParser                   | xml-parser
        Acronym in middle            | parseHTMLDocument           | parse-html-document
        Acronym at beginning caps    | HTTPSConnection             | https-connection
        Acronym to acronym           | URLToHTMLConverter          | url-to-html-converter
        Snake_case lowercase         | leap_year_rules             | leap-year-rules
        Snake_case multi-word        | test_method_name            | test-method-name
        Snake_case simple            | simple_test                 | simple-test
        Spaces multiple words        | Leap Year Rules             | leap-year-rules
        Spaces with punctuation      | A Custom Test Title!        | a-custom-test-title
        Spaces two words             | table test                  | table-test
        Spaces title case            | User Authentication         | user-authentication
        Special char at-sign         | test@example.com            | test-example-com
        Special char percentage      | 100% coverage               | 100-coverage
        Special char colon           | user:admin                  | user-admin
        Single word uppercase        | Test                        | test
        Single word lowercase        | test                        | test
        Single word acronym          | XML                         | xml
        Empty string                 | ''                          | ''
        Single char lowercase        | a                           | a
        Single char uppercase        | A                           | a
        Numeric only                 | 123                         | 123
        CamelCase with number inline | test123Method               | test123-method
        Snake_case with number       | test_123_method             | test-123-method
        Acronym with number          | UTF8Encoder                 | utf8-encoder
        CamelCase number at start    | base64Encode                | base64-encode
        Mixed space and underscore   | test_method with spaces     | test-method-with-spaces
        Mixed snake and camel        | Test_Method_Name            | test-method-name
        Mixed snake and acronym      | XML_Parser                  | xml-parser
        """)
    void shouldSlugifyNames(String input, String expected) {
        assertThat(Slugger.slugify(input)).isEqualTo(expected);
    }
}
