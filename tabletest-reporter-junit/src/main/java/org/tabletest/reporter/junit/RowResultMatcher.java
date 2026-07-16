/*
 * Copyright 2025-present Nils Christian Haugen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tabletest.reporter.junit;

import org.tabletest.parser.Table;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Matches JUnit test results against table rows using scenario column values.
 * <p>
 * <strong>IMPORTANT:</strong> Reliable matching requires a scenario column.
 * Tables without a scenario column will not have {@code .passed}/{@code .failed}
 * roles applied because parameter type conversion makes matching unreliable.
 * <p>
 * When matching fails (e.g., duplicate scenario names, no scenario column),
 * no roles are applied to the row. Users can fix duplicate scenario names
 * by ensuring each row has a unique scenario value.
 * <p>
 * This is a utility class with static methods only.
 */
class RowResultMatcher {
    private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("^\\[(\\d+)]\\s+(.*)$");

    private RowResultMatcher() {
        // Utility class - no instantiation
    }

    /**
     * Finds all test results that match the given table row.
     * This is the main entry point for matching test results to table rows.
     *
     * @param rowIndex the 0-based table row index
     * @param table the table containing row data
     * @param scenarioIndex optional scenario column index for matching
     * @param rowResults all test results to search through
     * @return list of matching results (may be empty, may contain multiple for set expansion)
     */
    static List<RowResult> findMatchingResults(
            int rowIndex, Table table, OptionalInt scenarioIndex, List<RowResult> rowResults) {

        Optional<String> expectedDisplayNamePattern = buildExpectedDisplayName(rowIndex, table, scenarioIndex)
                .filter(scenario -> !isDuplicatedScenario(scenario, table, scenarioIndex));

        return rowResults.stream()
                .filter(result -> matchesRow(result.displayName(), expectedDisplayNamePattern))
                .toList();
    }

    /**
     * Checks whether the given scenario value occurs in more than one table row. Duplicated
     * scenario values make result attribution unreliable, so such rows match no results.
     */
    private static boolean isDuplicatedScenario(String scenario, Table table, OptionalInt scenarioIndex) {
        if (scenarioIndex.isEmpty() || scenarioIndex.getAsInt() >= table.columnCount()) {
            return false;
        }
        long occurrences = table.rows().stream()
                .map(row -> formatForJUnitDisplay(row.value(scenarioIndex.getAsInt())))
                .filter(scenario::equals)
                .count();
        return occurrences > 1;
    }

    /**
     * Builds the expected display name for a table row.
     * - If scenario column exists: returns Optional with the scenario value
     * - Otherwise, or when the row index is out of range: returns empty Optional (no matching)
     * <p>
     * Handles JUnit's display name formatting:
     * - null values are displayed as "null"
     * - empty strings are displayed as "" (with quotes)
     */
    static Optional<String> buildExpectedDisplayName(int rowIndex, Table table, OptionalInt scenarioIndex) {
        var rows = table.rows();
        if (rowIndex >= rows.size()) {
            return Optional.empty();
        }

        var row = rows.get(rowIndex);

        // If there's a scenario column, use its value
        if (scenarioIndex.isPresent()) {
            int index = scenarioIndex.getAsInt();
            if (index < table.columnCount()) {
                Object value = row.value(index);
                return Optional.of(formatForJUnitDisplay(value));
            }
        }

        // For rows without scenario column, return empty to indicate no matching
        return Optional.empty();
    }

    /**
     * Formats a value for comparison with display names.
     * <p>
     * Note: Quote-stripping is handled separately by {@link #stripSurroundingQuotes},
     * so this method returns raw string values without adding quotes.
     * <p>
     * - null → "null" (JUnit displays null as the string "null")
     * - other values → String.valueOf(value)
     */
    private static String formatForJUnitDisplay(Object value) {
        if (value == null) {
            return "null";
        }
        return String.valueOf(value);
    }

    /**
     * Checks if a test result display name matches the expected row pattern.
     * Display name format: "[index] displayName" or "[index] displayName (expansion params)"
     * <p>
     * A display name matches when it equals the scenario value exactly, or when it is the
     * scenario value followed by set-expansion parameters like " (value = a)". A bare prefix
     * is not enough: scenario "Add" must not claim the results of scenario "Add negative".
     * <p>
     * Returns {@code false} if no scenario column exists, as matching without a scenario
     * column is unreliable due to parameter type conversion.
     */
    static boolean matchesRow(String actualDisplayName, Optional<String> expectedPattern) {
        // No scenario column means no reliable matching possible
        if (expectedPattern.isEmpty()) {
            return false;
        }

        Matcher matcher = DISPLAY_NAME_PATTERN.matcher(actualDisplayName);
        if (!matcher.matches()) {
            return false;
        }

        String displayNamePart = matcher.group(2);
        String expected = expectedPattern.get();

        // Strip surrounding quotes (JUnit 6.0+ quotes String parameters)
        return stripSurroundingQuotes(displayNamePart).equals(expected)
                || hasExpansionParameters(displayNamePart, expected);
    }

    /**
     * Checks for the scenario value followed by set-expansion parameters, with or without
     * JUnit 6.0+ quoting of the scenario value itself.
     */
    private static boolean hasExpansionParameters(String displayNamePart, String expected) {
        return displayNamePart.startsWith(expected + " (") || displayNamePart.startsWith("\"" + expected + "\" (");
    }

    /**
     * Strips surrounding double quotes from a string if present.
     * <p>
     * JUnit 6.0+ quotes String parameters in display names: {@code "value"}
     * This method strips those quotes for comparison against table scenario values.
     * For JUnit 5.x (no quotes), this is a no-op.
     *
     * @param value the display name value (potentially quoted)
     * @return the value without surrounding quotes if they were present
     */
    private static String stripSurroundingQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
