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
package io.github.nchaugen.tabletest.reporter.junit;

import io.github.nchaugen.tabletest.parser.Table;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and matches JUnit display names against table rows.
 * Handles both scenario-based and fuzzy matching for set expansion.
 *
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
                .filter(result -> matchesRow(result.displayName(), expectedDisplayNamePattern, table, rowIndex))
                .toList();
    }

    /**
     * Builds the expected display name for a table row.
     * - If scenario column exists: returns Optional with the scenario value
     * - Otherwise: returns empty Optional (will use fuzzy matching for non-scenario rows)
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

        // For rows without scenario column, return empty to indicate fuzzy matching
        return Optional.empty();
    }

    /**
     * Formats a value as JUnit would display it in test names.
     * - null → "null"
     * - empty string → "\"\"" (with quotes)
     * - other values → String.valueOf(value)
     */
    private static String formatForJUnitDisplay(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String str && str.isEmpty()) {
            return "\"\"";
        }
        return String.valueOf(value);
    }

    /**
     * Checks if a test result display name matches the expected row pattern.
     * Display name format: "[index] displayName" or "[index] displayName (params)"
     * We match on the displayName part (before any parentheses for expansion params).
     */
    static boolean matchesRow(String actualDisplayName, Optional<String> expectedPattern, Table table, int rowIndex) {
        Matcher matcher = DISPLAY_NAME_PATTERN.matcher(actualDisplayName);
        if (!matcher.matches()) {
            return false;
        }

        String displayNamePart = matcher.group(2);

        // Remove expansion parameters (everything from first '(' onwards)
        int parenIndex = displayNamePart.indexOf('(');
        if (parenIndex >= 0) {
            displayNamePart = displayNamePart.substring(0, parenIndex).trim();
        }

        // If there's a scenario column, do exact matching
        if (expectedPattern.isPresent()) {
            return displayNamePart.equals(expectedPattern.get());
        }

        // For rows without scenario column, use fuzzy matching
        // This handles set expansion where display names vary
        return matchesRowWithoutScenario(displayNamePart, table, rowIndex);
    }

    /**
     * Matches a display name against a row when there's no scenario column.
     * Handles set expansion by checking if non-Set values match their positions.
     *
     * Note: This implementation assumes display names use comma-separated values.
     * It will fail if table cell values themselves contain commas, as JUnit's
     * display name generation doesn't escape commas in parameter values.
     */
    private static boolean matchesRowWithoutScenario(String displayNamePart, Table table, int rowIndex) {
        var rows = table.rows();
        if (rowIndex >= rows.size()) {
            return false;
        }

        var row = rows.get(rowIndex);

        // Split by comma - NOTE: This breaks if cell values contain commas
        // This is a limitation of JUnit's display name format
        String[] displayValues = displayNamePart.split(",\\s*");

        // If the number of display values doesn't match column count, no match
        if (displayValues.length != table.columnCount()) {
            return false;
        }

        // Check if non-Set values in the row match the corresponding display values
        for (int i = 0; i < table.columnCount(); i++) {
            Object rowValue = row.value(i);

            // Skip Set values (these will vary in display names due to expansion)
            if (rowValue instanceof java.util.Set) {
                continue;
            }

            // Check if the non-Set value matches the display name at this position
            String expectedValue = String.valueOf(rowValue);
            if (!expectedValue.equals(displayValues[i])) {
                return false;
            }
        }

        return true;
    }
}
