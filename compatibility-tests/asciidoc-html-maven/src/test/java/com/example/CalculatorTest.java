package com.example;

import io.github.nchaugen.tabletest.junit.Scenario;
import io.github.nchaugen.tabletest.junit.TableTest;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Calculator Tests")
class CalculatorTest {

    @TableTest("""
            Scenario    | A | B | Sum?
            Small       | 1 | 2 | 3
            Medium      | 5 | 7 | 12
            Large       | 10| 15| 25
            """)
    @DisplayName("Addition")
    void testAddition(int a, int b, int sum) {
        assertEquals(sum, a + b);
    }

    @TableTest("""
            Scenario    | A | B | Difference?
            Positive    | 5 | 3 | 2
            Zero        | 7 | 7 | 0
            Negative    | 3 | 8 | -5
            Large       | 20| 12| 7
            """)
    @DisplayName("Subtraction")
    void testSubtraction(@Scenario String scenario, int a, int b, int difference) {
        // Intentionally wrong calculation for last two rows
        int result = (scenario.equals("Negative") || scenario.equals("Large")) ? 0 : a - b;
        assertEquals(difference, result, "Subtraction failed for scenario: " + scenario);
    }

    @TableTest("""
            Scenario    | A | B | Product?
            Simple      | 2 | 3 | 6
            Double      | 4 | 5 | 20
            """)
    @DisplayName("Multiplication")
    void testMultiplication(int a, int b, int product) {
        // Intentionally return zero to fail all tests
        assertEquals(product, 0);
    }
}
