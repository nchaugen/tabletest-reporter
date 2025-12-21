package io.github.nchaugen.tabletest.reporter.junit;

import io.github.nchaugen.tabletest.junit.Scenario;
import io.github.nchaugen.tabletest.junit.TableTest;
import io.github.nchaugen.tabletest.parser.Table;
import io.github.nchaugen.tabletest.parser.TableParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RowResultMatcherTest {

    @TableTest("""
        Actual Display Name     | Expected Pattern    | Matches?
        '[1] Simple scenario'   | 'Simple scenario'   | true
        '[1] Different name'    | 'Simple scenario'   | false
        '[2] Another scenario'  | 'Simple scenario'   | false
        '[1] Match with spaces' | 'Match with spaces' | true
        """)
    void shouldMatchWithScenarioColumn(@Scenario String actualDisplayName, String expectedPattern, boolean matches) {
        Table table = TableParser.parse("a|b");
        boolean result = RowResultMatcher.matchesRow(actualDisplayName, Optional.of(expectedPattern), table, 0);
        assertEquals(matches, result);
    }

    @TableTest("""
        Actual Display Name             | Matches?
        "[1] Scenario (param = value)"  | true
        "[1] Scenario (a = 1, b = 2)"   | true
        "[1] Different (param = value)" | false
        """)
    void shouldMatchScenarioWithExpansionParameters(@Scenario String actualDisplayName, boolean matches) {
        Table table = TableParser.parse("a|b");
        boolean result = RowResultMatcher.matchesRow(actualDisplayName, Optional.of("Scenario"), table, 0);
        assertEquals(matches, result);
    }

    @Test
    void shouldMatchWithoutScenarioColumn() {
        Table table = TableParser.parse("a|b|c\n1|2|3");

        assertTrue(RowResultMatcher.matchesRow("[1] 1, 2, 3", Optional.empty(), table, 0));
        assertFalse(RowResultMatcher.matchesRow("[1] 1, 2, 4", Optional.empty(), table, 0));
        assertFalse(RowResultMatcher.matchesRow("[1] 1, 2", Optional.empty(), table, 0));
    }

    @Test
    void shouldFailToMatchWhenCellValuesContainCommas() {
        // This demonstrates the known limitation: comma-separated parsing breaks
        // when cell values themselves contain commas
        Table table = TableParser.parse("name|value\nJohn, Doe|123");

        // This SHOULD match but WON'T due to comma-splitting limitation
        // The display name "John, Doe, 123" gets split into ["John", "Doe", "123"]
        // but the table has only 2 columns: ["John, Doe", "123"]
        assertFalse(RowResultMatcher.matchesRow("[1] John, Doe, 123", Optional.empty(), table, 0));
    }

    @Test
    void shouldFindMatchingResultsWithScenarioColumn() {
        Table table = TableParser.parse("scenario|value\nTest 1|foo\nTest 2|bar");
        List<RowResult> results = List.of(
            new RowResult(1, true, null, "[1] Test 1"),
            new RowResult(2, false, null, "[2] Test 2")
        );

        List<RowResult> matches = RowResultMatcher.findMatchingResults(0, table, OptionalInt.of(0), results);

        assertEquals(1, matches.size());
        assertEquals("[1] Test 1", matches.getFirst().displayName());
        assertTrue(matches.getFirst().passed());
    }

    @Test
    void shouldFindMatchingResultsWithoutScenarioColumn() {
        Table table = TableParser.parse("a|b\n1|2\n3|4");
        List<RowResult> results = List.of(
            new RowResult(1, true, null, "[1] 1, 2"),
            new RowResult(2, false, null, "[2] 3, 4")
        );

        List<RowResult> matches = RowResultMatcher.findMatchingResults(0, table, OptionalInt.empty(), results);

        assertEquals(1, matches.size());
        assertEquals("[1] 1, 2", matches.getFirst().displayName());
    }

    @Test
    void shouldFindMultipleMatchingResultsForSetExpansion() {
        Table table = TableParser.parse("scenario|value\nTest Set|x");
        List<RowResult> results = List.of(
            new RowResult(1, true, null, "[1] Test Set (value = a)"),
            new RowResult(1, false, null, "[1] Test Set (value = b)"),
            new RowResult(1, true, null, "[1] Test Set (value = c)")
        );

        List<RowResult> matches = RowResultMatcher.findMatchingResults(0, table, OptionalInt.of(0), results);

        assertEquals(3, matches.size());
    }

    @Test
    void shouldReturnEmptyListWhenNoResultsMatch() {
        Table table = TableParser.parse("scenario|value\nTest 1|foo");
        List<RowResult> results = List.of(
            new RowResult(1, true, null, "[1] Test 2")
        );

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
        Table table = TableParser.parse("a|b");
        boolean result = RowResultMatcher.matchesRow(actualDisplayName, Optional.empty(), table, 0);
        assertEquals(matches, result);
    }

    @Test
    void shouldMatchEmptyStringScenario() {
        // Based on JavaScenarioNameTest: "" in table should match JUnit display of ""
        // Table: "" in scenario column
        Table table = TableParser.parse("Scenario|value\n\"\"|foo");

        // JUnit displays empty string ("") as: [2] ""
        // The display name contains literal quotes around empty string
        boolean result = RowResultMatcher.matchesRow("[2] \"\"", Optional.of("\"\""), table, 0);
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

        boolean result = RowResultMatcher.matchesRow("[1] null", Optional.of(expectedPattern), table, 0);
        assertTrue(result, "Should match null scenario - JUnit displays 'null' for null parameter");
    }
}
