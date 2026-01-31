package com.example;

import io.github.nchaugen.tabletest.junit.TableTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled("Entire class is disabled")
@DisplayName("Disabled Class")
class DisabledClassTest {

    @DisplayName("This test should not run")
    @TableTest("""
            A | B | Sum?
            1 | 2 | 999
            """)
    void testThatShouldNotRun(int a, int b, int sum) {
        assertEquals(sum, a + b);
    }
}
