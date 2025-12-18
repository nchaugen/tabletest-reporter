package com.example;

import io.github.nchaugen.tabletest.junit.Description;
import io.github.nchaugen.tabletest.junit.TableTest;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Calculator Tests")
@Description("Basic arithmetic operations")
class CalculatorTest {

    @DisplayName("Addition")
    @Description("Test addition of two numbers")
    @TableTest("""
        a | b | sum?
        1 | 2 | 3
        5 | 7 | 12
        """)
    void testAddition(int a, int b, int expectedSum) {
        assertEquals(expectedSum, a + b);
    }

    @DisplayName("Multiplication")
    @Description("Test multiplication of two numbers")
    @TableTest("""
        a | b | product?
        2 | 3 | 6
        4 | 5 | 20
        """)
    void testMultiplication(int a, int b, int expectedProduct) {
        assertEquals(expectedProduct, a * b);
    }
}
