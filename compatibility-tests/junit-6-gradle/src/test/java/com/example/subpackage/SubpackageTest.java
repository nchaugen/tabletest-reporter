package com.example.subpackage;

import org.tabletest.junit.TableTest;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Subpackage Test")
class SubpackageTest {

    @DisplayName("Test in subpackage")
    @TableTest("""
            A | B | Sum?
            1 | 2 | 3
            4 | 5 | 9
            """)
    void testInSubpackage(int a, int b, int sum) {
        assertEquals(sum, a + b);
    }
}
