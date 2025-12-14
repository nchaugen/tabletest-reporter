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

import io.github.nchaugen.tabletest.parser.Row;
import io.github.nchaugen.tabletest.parser.Table;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static java.util.Collections.unmodifiableSet;

public record TableMetadata(
    String title,
    String description,
    ColumnRoles columnRoles,
    RowRoles rowRoles,
    List<RowResult> rowResults
) {
    public TableMetadata {
        columnRoles = columnRoles != null ? columnRoles : ColumnRoles.NO_ROLES;
        rowRoles = rowRoles != null ? rowRoles : RowRoles.NO_ROLES;
        rowResults = rowResults != null ? rowResults : List.of();
    }

    public TableMetadata() {
        this(null, null, null, null, null);
    }

    public TableMetadata withTitle(String title) {
        return new TableMetadata(title, description, columnRoles, rowRoles, rowResults);
    }

    public TableMetadata withDescription(String description) {
        return new TableMetadata(title, description, columnRoles, rowRoles, rowResults);
    }

    public TableMetadata withColumnRoles(ColumnRoles columnRoles) {
        return new TableMetadata(title, description, columnRoles, rowRoles, rowResults);
    }

    public TableMetadata withRowResults(List<RowResult> rowResults) {
        return new TableMetadata(title, description, columnRoles, rowRoles, rowResults);
    }

    /**
     * Converts this metadata and the given table into structured data ready for serialization.
     */
    public TableTestData toTableTestData(Table table) {
        List<CellData> headers = IntStream.range(0, table.columnCount())
            .mapToObj(i -> new CellData(table.header(i), columnRolesFor(i)))
            .toList();

        List<Row> rows = table.rows();
        List<RowData> rowData = IntStream.range(0, rows.size())
            .mapToObj(rowIndex -> new RowData(
                IntStream.range(0, table.columnCount())
                    .mapToObj(colIndex -> new CellData(
                        rows.get(rowIndex).value(colIndex),
                        combineRoles(colIndex, rowIndex)
                    ))
                    .toList()
            ))
            .toList();

        List<RowResultData> rowResultData = rowResults.stream()
            .map(RowResultData::from)
            .toList();

        return new TableTestData(title, description, headers, rowData, rowResultData);
    }

    private Set<CellRole> columnRolesFor(int colIndex) {
        return columnRoles.roleFor(colIndex);
    }

    /**
     * Combines column roles and row roles into a single set for the given indices.
     * Maintains order: column roles first (expectation, scenario), then row roles (passed, failed).
     */
    private Set<CellRole> combineRoles(int colIndex, int rowIndex) {
        Set<CellRole> combined = new java.util.LinkedHashSet<>();
        combined.addAll(columnRoles.roleFor(colIndex));
        combined.addAll(rowRoles.roleFor(rowIndex));
        return unmodifiableSet(combined);
    }

}
