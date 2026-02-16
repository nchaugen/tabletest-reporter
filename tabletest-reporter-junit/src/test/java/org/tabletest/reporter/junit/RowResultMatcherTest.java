package org.tabletest.reporter.junit;

import org.junit.jupiter.api.Test;
import org.tabletest.junit.Scenario;
import org.tabletest.junit.TableTest;
import org.tabletest.parser.Table;
import org.tabletest.parser.TableParser;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RowResultMatcherTest {

    @TableTest("""
        Actual Display Name            | Expected Pattern           | Matches?
        '[1] Simple scenario'          | 'Simple scenario'          | true
        '[1] Different name'           | 'Simple scenario'          | false
        '[2] Another scenario'         | 'Simple scenario'          | false
        '[1] Match with spaces'        | 'Match with spaces'        | true
        '[1] Match with (parenthesis)' | 'Match with (parenthesis)' | true
        '[1] "Test scenario"'          | 'Test scenario'            | true
        '[1] Test scenario'            | 'Test scenario'            | true
        """)
    void shouldMatchWithScenarioColumn(@Scenario String actualDisplayName, String expectedPattern, boolean matches) {
        boolean result = RowResultMatcher.matchesRow(actualDisplayName, Optional.of(expectedPattern));
        assertEquals(matches, result);
    }

    @TableTest("""
        Actual Display Name             | Matches?
        "[1] Scenario (param = value)"  | true
        "[1] Scenario (a = 1, b = 2)"   | true
        "[1] Different (param = value)" | false
        """)
    void shouldMatchScenarioWithExpansionParameters(@Scenario String actualDisplayName, boolean matches) {
        boolean result = RowResultMatcher.matchesRow(actualDisplayName, Optional.of("Scenario"));
        assertEquals(matches, result);
    }

    @Test
    void shouldNotMatchWithoutScenarioColumn() {
        assertFalse(RowResultMatcher.matchesRow("[1] 1, 2, 3", Optional.empty()));
        assertFalse(RowResultMatcher.matchesRow("[1] 1, 2, 4", Optional.empty()));
    }

    @Test
    void shouldFindMatchingResultsWithScenarioColumn() {
        Table table = TableParser.parse("scenario|value\nTest 1|foo\nTest 2|bar");
        List<RowResult> results =
                List.of(new RowResult(1, true, null, "[1] Test 1"), new RowResult(2, false, null, "[2] Test 2"));

        List<RowResult> matches = RowResultMatcher.findMatchingResults(0, table, OptionalInt.of(0), results);

        assertEquals(1, matches.size());
        assertEquals("[1] Test 1", matches.getFirst().displayName());
        assertTrue(matches.getFirst().passed());
    }

    @Test
    void shouldReturnEmptyListWithoutScenarioColumn() {
        // Without a scenario column, matching always fails (returns empty list)
        Table table = TableParser.parse("a|b\n1|2\n3|4");
        List<RowResult> results =
                List.of(new RowResult(1, true, null, "[1] 1, 2"), new RowResult(2, false, null, "[2] 3, 4"));

        List<RowResult> matches = RowResultMatcher.findMatchingResults(0, table, OptionalInt.empty(), results);

        assertTrue(matches.isEmpty());
    }

    @Test
    void shouldFindMultipleMatchingResultsForSetExpansion() {
        Table table = TableParser.parse("scenario|value\nTest Set|x");
        List<RowResult> results = List.of(
                new RowResult(1, true, null, "[1] Test Set (value = a)"),
                new RowResult(1, false, null, "[1] Test Set (value = b)"),
                new RowResult(1, true, null, "[1] Test Set (value = c)"));

        List<RowResult> matches = RowResultMatcher.findMatchingResults(0, table, OptionalInt.of(0), results);

        assertEquals(3, matches.size());
    }

    @Test
    void shouldReturnEmptyListWhenNoResultsMatch() {
        Table table = TableParser.parse("scenario|value\nTest 1|foo");
        List<RowResult> results = List.of(new RowResult(1, true, null, "[1] Test 2"));

        List<RowResult> matches = RowResultMatcher.findMatchingResults(0, table, OptionalInt.of(0), results);

        assertTrue(matches.isEmpty());
    }

    @TableTest("""
        Actual Display Name | Matches?
        'No brackets'       | false
        '[1]Missing space'  | false
        '1 Wrong format'    | false
        """)
    void shouldRejectInvalidDisplayNameFormat(@Scenario String actualDisplayName, boolean matches) {
        boolean result = RowResultMatcher.matchesRow(actualDisplayName, Optional.of("Test"));
        assertEquals(matches, result);
    }

    @Test
    void shouldMatchEmptyStringScenario() {
        // JUnit 6.0+ displays empty string as: [2] ""
        // After stripping quotes, we get empty string, which matches table value
        boolean result = RowResultMatcher.matchesRow("[2] \"\"", Optional.of(""));
        assertTrue(result, "Should match empty string scenario - JUnit displays \\\"\\\" for empty string parameter");
    }

    @Test
    void shouldMatchNullScenario() {
        // Based on JavaScenarioNameTest: empty cell (null) should match JUnit display "null"
        // Table: empty cell in scenario column (parsed as null)
        Table table = TableParser.parse("Scenario|value\n|foo");

        Object scenarioValue = table.rows().getFirst().value(0);

        // Empty cells are parsed as null
        // JUnit displays null parameters as: [1] null
        // But String.valueOf(null) returns "null" which should match
        String expectedPattern = scenarioValue == null ? "null" : String.valueOf(scenarioValue);

        boolean result = RowResultMatcher.matchesRow("[1] null", Optional.of(expectedPattern));
        assertTrue(result, "Should match null scenario - JUnit displays 'null' for null parameter");
    }
}
