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
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Determines pass/fail roles for table rows based on test execution results.
 * Handles both regular rows and rows with set expansion, where multiple test results
 * correspond to a single table row.
 */
public class RowRoles {
    public static final RowRoles NO_ROLES = new RowRoles();

    private final Optional<Table> table;
    private final List<RowResult> rowResults;
    private final ColumnRoles columnRoles;

    private RowRoles() {
        this.table = Optional.empty();
        this.rowResults = List.of();
        this.columnRoles = ColumnRoles.NO_ROLES;
    }

    public RowRoles(Table table, List<RowResult> rowResults, ColumnRoles columnRoles) {
        this.table = Optional.of(table);
        this.rowResults = rowResults;
        this.columnRoles = columnRoles;
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
        if (table.isEmpty() || rowResults.isEmpty()) {
            return Collections.emptySet();
        }

        // Find all test results that match this row
        OptionalInt scenarioIndex = columnRoles.scenarioIndex();
        List<RowResult> matchingResults = RowResultMatcher.findMatchingResults(
            rowIndex,
            table.get(),
            scenarioIndex,
            rowResults
        );

        if (matchingResults.isEmpty()) {
            return Collections.emptySet();
        }

        // If ANY result failed, mark the row as FAILED
        boolean anyFailed = matchingResults.stream().anyMatch(result -> !result.passed());
        return Set.of(anyFailed ? CellRole.FAILED : CellRole.PASSED);
    }
}
