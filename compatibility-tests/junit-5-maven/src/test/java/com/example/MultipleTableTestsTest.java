package com.example;

import io.github.nchaugen.tabletest.junit.TableTest;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Multiple TableTests")
class MultipleTableTestsTest {

    @DisplayName("Addition")
    @TableTest("""
            A | B | Sum?
            1 | 2 | 3
            5 | 7 | 12
            """)
    void testAddition(int a, int b, int sum) {
        assertEquals(sum, a + b);
    }

    @DisplayName("Subtraction")
    @TableTest("""
            A  | B | Difference?
            10 | 3 | 7
            5  | 5 | 0
            """)
    void testSubtraction(int a, int b, int difference) {
        assertEquals(difference, a - b);
    }

    @DisplayName("Multiplication")
    @TableTest("""
            A | B | Product?
            2 | 3 | 6
            4 | 5 | 20
            """)
    void testMultiplication(int a, int b, int product) {
        assertEquals(product, a * b);
    }
}
