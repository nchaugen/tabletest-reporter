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

import java.util.List;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;

public interface TableMetadata {
    String title();
    String description();
    ColumnRoles columnRoles();
    default RowRoles rowRoles() {
        return RowRoles.NO_ROLES;
    }
    default List<RowResult> rowResults() {
        return List.of();
    }

    default Set<CellRole> columnRolesFor(int colIndex) {
        return columnRoles().roleFor(colIndex);
    }

    /**
     * Combines column roles and row roles into a single set for the given indices.
     * Maintains order: column roles first (expectation, scenario), then row roles (passed, failed).
     */
    default Set<CellRole> combineRoles(int colIndex, int rowIndex) {
        Set<CellRole> combined = new java.util.LinkedHashSet<>();
        combined.addAll(columnRoles().roleFor(colIndex));
        combined.addAll(rowRoles().roleFor(rowIndex));
        return unmodifiableSet(combined);
    }
}
