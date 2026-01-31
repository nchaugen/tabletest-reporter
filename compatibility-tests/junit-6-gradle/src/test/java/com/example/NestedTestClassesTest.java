package com.example;

import io.github.nchaugen.tabletest.junit.TableTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Nested Test Classes")
class NestedTestClassesTest {

    @DisplayName("Outer test")
    @TableTest("""
            A | B | Sum?
            1 | 2 | 3
            """)
    void outerTest(int a, int b, int sum) {
        assertEquals(sum, a + b);
    }

    @Nested
    @DisplayName("Inner tests")
    class InnerTests {

        @DisplayName("Inner test")
        @TableTest("""
                A | B | Product?
                2 | 3 | 6
                """)
        void innerTest(int a, int b, int product) {
            assertEquals(product, a * b);
        }
    }
}
