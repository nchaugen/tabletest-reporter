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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks pass/fail status for table rows based on test execution results.
 * Handles both regular rows and rows with set expansion, where multiple test results
 * correspond to a single table row.
 */
public class RowRoles {
    public static final RowRoles NO_ROLES = new RowRoles(null, List.of());

    private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("^\\[(\\d+)]\\s+(.*)$");

    private final Table table;
    private final List<RowResult> rowResults;
    private final int scenarioIndex;

    public RowRoles(Table table, List<RowResult> rowResults) {
        this(table, rowResults, -1);
    }

    public RowRoles(Table table, List<RowResult> rowResults, int scenarioIndex) {
        this.table = table;
        this.rowResults = rowResults;
        this.scenarioIndex = scenarioIndex;
    }

    /**
     * Returns the role for a given row index based on test execution results.
     * Handles set expansion where multiple test results may correspond to one table row.
     * If any result for the row fails, the entire row is marked as FAILED.
     *
     * @param rowIndex 0-based table row index
     * @return Set containing PASSED or FAILED role, or empty set if no results found
     */
    public Set<CellRole> roleFor(int rowIndex) {
        if (table == null || rowResults.isEmpty()) {
            return Collections.emptySet();
        }

        // Get the expected row display name pattern from the table
        String expectedDisplayNamePattern = buildExpectedDisplayName(rowIndex);

        // Find all results that match this row (handles set expansion)
        List<RowResult> matchingResults = rowResults.stream()
            .filter(result -> matchesRow(result.displayName(), expectedDisplayNamePattern, rowIndex))
            .toList();

        if (matchingResults.isEmpty()) {
            return Collections.emptySet();
        }

        // If ANY result failed, mark the row as FAILED
        boolean anyFailed = matchingResults.stream().anyMatch(result -> !result.passed());
        return Set.of(anyFailed ? CellRole.FAILED : CellRole.PASSED);
    }

    /**
     * Builds the expected display name for a table row.
     * - If scenario column exists: returns the scenario value
     * - Otherwise: returns null (will use fuzzy matching for non-scenario rows)
     */
    private String buildExpectedDisplayName(int rowIndex) {
        var rows = table.rows();
        if (rowIndex >= rows.size()) {
            return "";
        }

        var row = rows.get(rowIndex);

        // If there's a scenario column, use its value
        if (scenarioIndex >= 0 && scenarioIndex < table.columnCount()) {
            return String.valueOf(row.value(scenarioIndex));
        }

        // For rows without scenario column, return null to indicate fuzzy matching
        return null;
    }

    /**
     * Checks if a test result display name matches the expected row pattern.
     * Display name format: "[index] displayName" or "[index] displayName (params)"
     * We match on the displayName part (before any parentheses for expansion params).
     */
    private boolean matchesRow(String actualDisplayName, String expectedPattern, int rowIndex) {
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
        if (expectedPattern != null) {
            return displayNamePart.equals(expectedPattern);
        }

        // For rows without scenario column, use fuzzy matching
        // This handles set expansion where display names vary
        return matchesRowWithoutScenario(displayNamePart, rowIndex);
    }

    /**
     * Matches a display name against a row when there's no scenario column.
     * Handles set expansion by checking if non-Set values match their positions.
     */
    private boolean matchesRowWithoutScenario(String displayNamePart, int rowIndex) {
        var rows = table.rows();
        if (rowIndex >= rows.size()) {
            return false;
        }

        var row = rows.get(rowIndex);
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
            if (!String.valueOf(rowValue).equals(displayValues[i])) {
                return false;
            }
        }

        return true;
    }
}
