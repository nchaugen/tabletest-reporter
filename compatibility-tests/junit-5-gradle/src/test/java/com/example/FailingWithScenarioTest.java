package com.example;

import io.github.nchaugen.tabletest.junit.Scenario;
import io.github.nchaugen.tabletest.junit.TableTest;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Failing Test With Scenario")
class FailingWithScenarioTest {

    @DisplayName("Mixed results with scenario column")
    @TableTest("""
            Scenario      | A | B | Sum?
            Passing       | 1 | 2 | 3
            Failing       | 5 | 7 | 999
            Also passing  | 3 | 4 | 7
            """)
    void mixedResultsWithScenario(@Scenario String scenario, int a, int b, int sum) {
        assertEquals(sum, a + b, "Failed for scenario: " + scenario);
    }
}
