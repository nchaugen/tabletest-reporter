package io.github.nchaugen.tabletest.reporter.junit;

import io.github.nchaugen.tabletest.junit.TableTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the FilenameTransformer which converts test class and method names
 * to web-friendly kebab-case filenames.
 */
class FilenameTransformerTest {

    @TableTest("""
        Scenario                     | Input                       | Result?
        camelCase PascalCase         | LeapYearRules               | leap-year-rules
        camelCase multi-word         | TestClassName               | test-class-name
        camelCase starting lowercase | simpleTest                  | simple-test
        acronym at beginning         | XMLParser                   | xml-parser
        acronym in middle            | parseHTMLDocument           | parse-html-document
        acronym at beginning caps    | HTTPSConnection             | https-connection
        acronym to acronym           | URLToHTMLConverter          | url-to-html-converter
        snake_case lowercase         | leap_year_rules             | leap-year-rules
        snake_case multi-word        | test_method_name            | test-method-name
        snake_case simple            | simple_test                 | simple-test
        spaces multiple words        | Leap Year Rules             | leap-year-rules
        spaces with punctuation      | A Custom Test Title!        | a-custom-test-title
        spaces two words             | table test                  | table-test
        spaces title case            | User Authentication         | user-authentication
        special char at-sign         | test@example.com            | test-example-com
        special char percentage      | 100% coverage               | 100-coverage
        special char colon           | user:admin                  | user-admin
        single word uppercase        | Test                        | test
        single word lowercase        | test                        | test
        single word acronym          | XML                         | xml
        empty string                 | ''                          | ''
        single char lowercase        | a                           | a
        single char uppercase        | A                           | a
        numeric only                 | 123                         | 123
        camelCase with number inline | test123Method               | test123-method
        snake_case with number       | test_123_method             | test-123-method
        acronym with number          | UTF8Encoder                 | utf8-encoder
        camelCase number at start    | base64Encode                | base64-encode
        mixed space and underscore   | test_method with spaces     | test-method-with-spaces
        mixed snake and camel        | Test_Method_Name            | test-method-name
        mixed snake and acronym      | XML_Parser                  | xml-parser
        """)
    void shouldTransformNames(String input, String expected) {
        assertThat(FilenameTransformer.transform(input)).isEqualTo(expected);
    }
}
