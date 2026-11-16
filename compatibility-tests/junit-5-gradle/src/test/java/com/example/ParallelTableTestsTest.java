package com.example;

import io.github.nchaugen.tabletest.junit.Scenario;
import io.github.nchaugen.tabletest.junit.TableTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises concurrent result collection for two methods on the minimum supported JUnit. */
@DisplayName("Parallel tables")
@Execution(ExecutionMode.CONCURRENT)
@Timeout(20)
class ParallelTableTestsTest {

    private static CountDownLatch started;

    @BeforeAll
    static void prepareConcurrentRows() {
        started = new CountDownLatch(4);
    }

    @DisplayName("First table")
    @TableTest("""
            Scenario   | Value | Expected Value
            First row  | 1     | 1
            Second row | 2     | 2
            """)
    void firstTable(@Scenario String scenario, int value, int expected) throws InterruptedException {
        runRow(scenario, value, expected);
    }

    @DisplayName("Second table")
    @TableTest("""
            Scenario   | Value | Expected Value
            Third row  | 3     | 3
            Fourth row | 4     | 4
            """)
    void secondTable(@Scenario String scenario, int value, int expected) throws InterruptedException {
        runRow(scenario, value, expected);
    }

    private static void runRow(String scenario, int value, int expected) throws InterruptedException {
        started.countDown();
        assertTrue(started.await(10, TimeUnit.SECONDS), "All four rows must start before any row finishes");
        assertEquals(expected, value, scenario);
    }
}
