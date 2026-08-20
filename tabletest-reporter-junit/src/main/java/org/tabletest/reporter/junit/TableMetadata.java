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

import org.tabletest.parser.Row;
import org.tabletest.parser.Table;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.IntStream.range;

public record TableMetadata(
        String methodName,
        String slug,
        String title,
        String description,
        ColumnRoles columnRoles,
        DeclaredColumnRoles declaredColumnRoles,
        ExpandingColumns expandingColumns,
        RowRoles rowRoles,
        List<RowResult> rowResults) {
    public TableMetadata {
        columnRoles = columnRoles != null ? columnRoles : ColumnRoles.NO_ROLES;
        declaredColumnRoles = declaredColumnRoles != null ? declaredColumnRoles : DeclaredColumnRoles.NONE;
        expandingColumns = expandingColumns != null ? expandingColumns : ExpandingColumns.NONE;
        rowRoles = rowRoles != null ? rowRoles : RowRoles.NO_ROLES;
        rowResults = rowResults != null ? rowResults : List.of();
    }

    public TableMetadata() {
        this(null, null, null, null, null, null, null, null, null);
    }

    public TableMetadata withMethodName(String methodName) {
        return new TableMetadata(
                methodName,
                slug,
                title,
                description,
                columnRoles,
                declaredColumnRoles,
                expandingColumns,
                rowRoles,
                rowResults);
    }

    public TableMetadata withSlug(String slug) {
        return new TableMetadata(
                methodName,
                slug,
                title,
                description,
                columnRoles,
                declaredColumnRoles,
                expandingColumns,
                rowRoles,
                rowResults);
    }

    public TableMetadata withTitle(String title) {
        return new TableMetadata(
                methodName,
                slug,
                title,
                description,
                columnRoles,
                declaredColumnRoles,
                expandingColumns,
                rowRoles,
                rowResults);
    }

    public TableMetadata withDescription(String description) {
        return new TableMetadata(
                methodName,
                slug,
                title,
                description,
                columnRoles,
                declaredColumnRoles,
                expandingColumns,
                rowRoles,
                rowResults);
    }

    public TableMetadata withExpandingColumns(ExpandingColumns expandingColumns) {
        return new TableMetadata(
                methodName,
                slug,
                title,
                description,
                columnRoles,
                declaredColumnRoles,
                expandingColumns,
                rowRoles,
                rowResults);
    }

    public TableMetadata withDeclaredColumnRoles(DeclaredColumnRoles declaredColumnRoles) {
        return new TableMetadata(
                methodName,
                slug,
                title,
                description,
                columnRoles,
                declaredColumnRoles,
                expandingColumns,
                rowRoles,
                rowResults);
    }

    public TableMetadata withColumnRoles(ColumnRoles columnRoles) {
        return new TableMetadata(
                methodName,
                slug,
                title,
                description,
                columnRoles,
                declaredColumnRoles,
                expandingColumns,
                rowRoles,
                rowResults);
    }

    public TableMetadata withRowResults(List<RowResult> rowResults) {
        return new TableMetadata(
                methodName,
                slug,
                title,
                description,
                columnRoles,
                declaredColumnRoles,
                expandingColumns,
                rowRoles,
                rowResults);
    }

    /**
     * Converts this metadata and the given table into structured data ready for serialization.
     */
    public TableTestData toTableTestData(Table table) {
        List<CellData> headers = range(0, table.columnCount())
                .mapToObj(i -> new CellData(table.header(i), columnRolesFor(i)))
                .toList();

        List<Row> rows = table.rows();
        List<RowData> rowData = range(0, rows.size())
                .mapToObj(rowIndex -> new RowData(range(0, table.columnCount())
                        .mapToObj(colIndex -> cellOf(rows.get(rowIndex).value(colIndex), colIndex, rowIndex))
                        .toList()))
                .toList();

        List<RowResultData> rowResultData =
                rowResults.stream().map(RowResultData::from).toList();

        return new TableTestData(methodName, slug, title, description, headers, rowData, rowResultData);
    }

    private Set<String> columnRolesFor(int colIndex) {
        Set<String> combined = new LinkedHashSet<>(tokensOf(columnRoles.roleFor(colIndex)));
        combined.addAll(declaredColumnRoles.rolesFor(colIndex));
        return unmodifiableSet(combined);
    }

    /**
     * Combines column roles and row roles into a single set for the given indices. Maintains order: column roles first
     * (expectation, scenario), then row roles (passed, failed).
     */
    private CellData cellOf(Object value, int colIndex, int rowIndex) {
        return new CellData(value, combineRoles(colIndex, rowIndex, value));
    }

    /**
     * @return true if this cell's set value expands the row into one run per value, rather than
     * reaching the test as a set.
     */
    private boolean isValueSet(Object value, int colIndex) {
        return value instanceof Set && expandingColumns.expands(colIndex);
    }

    private Set<String> combineRoles(int colIndex, int rowIndex, Object value) {
        Set<String> combined = new LinkedHashSet<>();
        combined.addAll(tokensOf(columnRoles.roleFor(colIndex)));
        combined.addAll(tokensOf(rowRoles.roleFor(rowIndex)));
        combined.addAll(declaredColumnRoles.rolesFor(colIndex));
        if (isValueSet(value, colIndex)) {
            combined.add(CellRole.VALUE_SET.token());
        }
        return unmodifiableSet(combined);
    }

    private static Set<String> tokensOf(Set<CellRole> roles) {
        return roles.stream().map(CellRole::token).collect(toCollection(LinkedHashSet::new));
    }
}
