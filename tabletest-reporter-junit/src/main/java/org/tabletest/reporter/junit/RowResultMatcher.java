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

        Optional<String> expectedDisplayNamePattern = buildExpectedDisplayName(rowIndex, table, scenarioIndex);

        return rowResults.stream()
                .filter(result -> matchesRow(result.displayName(), expectedDisplayNamePattern))
                .toList();
    }

    /**
     * Builds the expected display name for a table row.
     * - If scenario column exists: returns Optional with the scenario value
     * - Otherwise: returns empty Optional (no matching will occur)
     * <p>
     * Handles JUnit's display name formatting:
     * - null values are displayed as "null"
     * - empty strings are displayed as "" (with quotes)
     */
    static Optional<String> buildExpectedDisplayName(int rowIndex, Table table, OptionalInt scenarioIndex) {
        var rows = table.rows();
        if (rowIndex >= rows.size()) {
            return Optional.of("");
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
     * Set expansion adds parameters like "(value = a)" after the scenario name.
     * We use {@code startsWith()} to match the scenario name, which handles both:
     * - Scenario names containing parentheses (e.g., "Match (example)")
     * - Set expansion parameters appended after the scenario name
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

        // Strip surrounding quotes (JUnit 6.0+ quotes String parameters)
        displayNamePart = stripSurroundingQuotes(displayNamePart);

        // Check if display name starts with expected pattern
        // This handles both scenario names with parentheses and set expansion parameters
        return displayNamePart.startsWith(expectedPattern.get());
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
