package io.github.nchaugen.tabletest.reporter.junit;

import io.github.nchaugen.tabletest.junit.TableTest;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(TableTestPublisher.class)
class TitleTransformerTest {

    @TableTest("""
        Input                  | Expected Title?
        LeapYearRules          | Leap Year Rules
        XMLParser              | XML Parser
        parseHTMLDocument      | Parse HTML Document
        HTTPSConnection        | HTTPS Connection
        simpleTest             | Simple Test
        A                      | A
        AB                     | AB
        ABC                    | ABC
        AbcDef                 | Abc Def
        ABCDef                 | ABC Def
        MyHTTPServer           | My HTTP Server
        getHTTPSURL            | Get HTTPSURL
        IOError                | IO Error
        SimpleClassName        | Simple Class Name
        """)
    void shouldTransformCamelCaseToTitle(String input, String expectedTitle) {
        assertEquals(expectedTitle, TitleTransformer.toTitle(input));
    }

    @TableTest("""
        Input | Expected?
        ""    | ""
        """)
    void shouldHandleEmptyString(String input, String expected) {
        assertEquals(expected, TitleTransformer.toTitle(input));
    }

    @org.junit.jupiter.api.Test
    void shouldHandleNull() {
        assertEquals(null, TitleTransformer.toTitle(null));
    }
}
