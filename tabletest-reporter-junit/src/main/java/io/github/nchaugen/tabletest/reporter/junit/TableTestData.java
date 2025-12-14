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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the complete data for a table test, ready for serialisation.
 *
 * @param title       The title of the table test (may be null)
 * @param description The description of the table test (may be null)
 * @param headers     The header cells
 * @param rows        The data rows
 * @param rowResults  The test results for each row
 */
public record TableTestData(
    String title,
    String description,
    List<CellData> headers,
    List<RowData> rows,
    List<RowResultData> rowResults
) {
    
    /**
     * Converts this table test data to a map suitable for YAML serialization.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (title != null) map.put("title", title);
        if (description != null) map.put("description", description);
        
        map.put("headers", headers.stream()
            .map(CellData::toMap)
            .toList());
        
        map.put("rows", rows.stream()
            .map(row -> row.cells().stream()
                .map(CellData::toMap)
                .toList())
            .toList());
        
        if (!rowResults.isEmpty()) {
            map.put("rowResults", rowResults.stream()
                .map(RowResultData::toMap)
                .toList());
        }
        
        return map;
    }
}
