package com.example;

import org.tabletest.junit.TableTest;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Failing Test Without Scenario")
class FailingWithoutScenarioTest {

    @DisplayName("Mixed results without scenario column")
    @TableTest("""
            A | B | Sum?
            1 | 2 | 3
            5 | 7 | 999
            3 | 4 | 7
            """)
    void mixedResultsWithoutScenario(int a, int b, int sum) {
        assertEquals(sum, a + b);
    }
}
