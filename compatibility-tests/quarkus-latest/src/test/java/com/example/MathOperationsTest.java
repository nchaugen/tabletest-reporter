package com.example;

import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Math Operations")
@Description("Basic mathematical operations")
class MathOperationsTest {

    @DisplayName("Subtraction")
    @Description("Test subtraction of two numbers")
    @TableTest("""
        a  | b | difference?
        10 | 3 | 7
        5  | 5 | 0
        """)
    void testSubtraction(int a, int b, int expectedDifference) {
        assertEquals(expectedDifference, a - b);
    }

    @DisplayName("Division")
    @Description("Test division of two numbers")
    @TableTest("""
        a  | b | quotient?
        10 | 2 | 5
        9  | 3 | 3
        """)
    void testDivision(int a, int b, int expectedQuotient) {
        assertEquals(expectedQuotient, a / b);
    }
}
