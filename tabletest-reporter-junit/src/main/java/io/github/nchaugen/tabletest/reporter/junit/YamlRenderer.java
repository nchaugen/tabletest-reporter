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
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Renders table tests and test indices to YAML format.
 */
class YamlRenderer {

    private static final DumpSettings SETTINGS = DumpSettings.builder()
        .setDefaultFlowStyle(FlowStyle.BLOCK)
        .setIndent(2)
        .setDefaultScalarStyle(ScalarStyle.DOUBLE_QUOTED)
        .setSplitLines(false)
        .setDereferenceAliases(true)
        .setMultiLineFlow(false)
        .setUseUnicodeEncoding(true)
        .build();

    private final Dump yaml = new Dump(SETTINGS);

    /**
     * Renders a table with its metadata and row results to YAML.
     */
    String renderTable(Table table, TableMetadata metadata) {
        LinkedHashMap<String, Object> content = new LinkedHashMap<>();
        if (metadata.title() != null) content.put("title", metadata.title());
        if (metadata.description() != null) content.put("description", metadata.description());

        content.put(
            "headers", IntStream.range(0, table.columnCount())
                .mapToObj(i -> toValueMap(table.header(i), metadata.columnRoles().roleFor(i)))
                .toList()
        );

        content.put(
            "rows", table.rows().stream()
                .map(row ->
                    IntStream.range(0, table.columnCount())
                    .mapToObj(i -> toValueMap(row.value(i), metadata.columnRoles().roleFor(i)))
                    .toList()
                )
                .toList()
        );

        if (!metadata.rowResults().isEmpty()) {
            content.put(
                "rowResults", metadata.rowResults().stream()
                    .map(this::toRowResultMap)
                    .toList()
            );
        }

        return yaml.dumpToString(content);
    }

    /**
     * Renders a test class index with references to all its table test files.
     */
    String renderClass(String title, String description, List<TableFileEntry> tableFileEntries) {
        LinkedHashMap<String, Object> content = new LinkedHashMap<>();
        if (title != null) content.put("title", title);
        if (description != null) content.put("description", description);

        LinkedHashMap<String, String> tables = new LinkedHashMap<>();
        tableFileEntries.forEach(it -> tables.put(it.title(), it.path().toString()));
        content.put("tables", tables);

        return yaml.dumpToString(content);
    }

    private Map<String, Object> toRowResultMap(RowResult result) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("rowIndex", result.rowIndex());
        map.put("passed", result.passed());
        map.put("displayName", result.displayName());
        if (result.cause() != null) {
            map.put("errorMessage", result.cause().getMessage());
        }
        return map;
    }

    private static Map<String, Object> toValueMap(Object value, CellRole role) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("value", value);
        if (role != CellRole.NONE) {
            map.put("role", role.name().toLowerCase());
        }
        return map;
    }

}
