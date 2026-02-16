package com.example;

import org.tabletest.junit.TableTest;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Custom Expectation Pattern")
class CustomExpectationPatternTest {

    @DisplayName("Uses Expected Result column")
    @TableTest("""
            A | B | Expected Result
            1 | 2 | 3
            5 | 7 | 12
            """)
    void testWithExpectedColumn(int a, int b, int expectedResult) {
        assertEquals(expectedResult, a + b);
    }
}
