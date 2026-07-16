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
import java.util.Map;

/**
 * Represents the test execution result for a single test invocation, ready for serialisation.
 *
 * @param rowIndex     The JUnit invocation index (1-based). With value-set expansion a table
 *                     row produces several invocations, so this does not map 1:1 to table rows
 * @param passed       Whether the test passed for this invocation
 * @param displayName  The display name of the test invocation
 * @param errorMessage The error message if the test failed, null if passed
 */
public record RowResultData(int rowIndex, boolean passed, String displayName, String errorMessage) {

    /**
     * Creates a RowResultData from a RowResult.
     */
    public static RowResultData from(RowResult result) {
        return new RowResultData(
                result.rowIndex(),
                result.passed(),
                result.displayName(),
                result.cause() != null ? result.cause().getMessage() : null);
    }

    /**
     * Converts this row result to a map suitable for YAML serialisation.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("rowIndex", rowIndex);
        map.put("passed", passed);
        map.put("displayName", displayName);
        if (errorMessage != null) {
            map.put("errorMessage", errorMessage);
        }
        return map;
    }
}
