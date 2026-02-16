package com.example;

import org.tabletest.junit.TableTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Disabled TableTest Method")
class DisabledTableTestTest {

    @DisplayName("Enabled test")
    @TableTest("""
            A | B | Sum?
            1 | 2 | 3
            """)
    void enabledTest(int a, int b, int sum) {
        assertEquals(sum, a + b);
    }

    @Disabled("This test is disabled")
    @DisplayName("Disabled test")
    @TableTest("""
            A | B | Sum?
            1 | 2 | 999
            """)
    void disabledTest(int a, int b, int sum) {
        assertEquals(sum, a + b);
    }
}
