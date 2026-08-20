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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a table cell with its value and associated roles.
 *
 * @param value The cell value
 * @param roles The role tokens associated with this cell, in publishing order
 */
public record CellData(Object value, Set<String> roles) {

    /**
     * Converts this cell to a map suitable for YAML serialisation.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("value", value);
        if (!roles.isEmpty()) {
            map.put("roles", List.copyOf(roles));
        }
        return map;
    }
}
