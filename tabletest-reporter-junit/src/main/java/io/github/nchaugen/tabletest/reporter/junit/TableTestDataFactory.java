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

/**
 * Builds {@link TableTestData} from table inputs, roles, and identity metadata.
 */
final class TableTestDataFactory {

    static TableTestData create(
            Table table, TableTestIdentity identity, ColumnRoles columnRoles, List<RowResult> rowResults) {
        ColumnRoles resolvedColumnRoles = columnRoles != null ? columnRoles : ColumnRoles.NO_ROLES;
        List<RowResult> resolvedResults = rowResults != null ? rowResults : List.of();
        RowRoles rowRoles = new RowRoles(table, resolvedResults, resolvedColumnRoles);

        String methodName = identity != null ? identity.methodName() : null;
        String slug = identity != null ? identity.slug() : null;
        String title = identity != null ? identity.title() : null;
        String description = identity != null ? identity.description() : null;

        return new TableMetadata(methodName, slug, title, description, resolvedColumnRoles, rowRoles, resolvedResults)
                .toTableTestData(table);
    }
}
