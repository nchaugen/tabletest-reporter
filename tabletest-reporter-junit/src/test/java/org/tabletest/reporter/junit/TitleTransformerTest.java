package org.tabletest.reporter.junit;

import org.junit.jupiter.api.extension.ExtendWith;
import org.tabletest.junit.TableTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(TableTestPublisher.class)
class TitleTransformerTest {
    @TableTest("""
        Input             | Expected Title?
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
    void shouldTransformNonDisplayNameToTitle(String input, String expectedTitle) {
        assertEquals(expectedTitle, TitleTransformer.toTitle(input));
    }

    @TableTest("""
        Method Name With Parameters                                   | Expected Title?
        testMethod(java.lang.String)                                  | Test Method
        validateInput(java.lang.String, java.lang.Integer)            | Validate Input
        processData(java.util.List, java.util.Map)                    | Process Data
        handleComplexTypes(java.util.List<java.lang.String>)          | Handle Complex Types
        multipleGenericParams(java.util.Map<K, V>, java.util.List<T>) | Multiple Generic Params
        simpleTest()                                                  | Simple Test
        parseHTMLDocument(java.lang.String, boolean)                  | Parse HTML Document
        """)
    void shouldStripParameterTypesFromMethodNames(String input, String expectedTitle) {
        String methodName = input.contains("(") ? input.substring(0, input.indexOf('(')) : input;
        assertEquals(expectedTitle, TitleTransformer.toTitle(methodName));
    }
}
