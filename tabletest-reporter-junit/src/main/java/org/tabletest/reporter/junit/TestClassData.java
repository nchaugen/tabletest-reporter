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
 * Represents the data for a test class, ready for serialisation.
 *
 * @param className   Fully qualified class name (may be null)
 * @param slug        Slugified class name used in output paths (may be null)
 * @param title       The title of the test class (may be null)
 * @param description The description of the test class (may be null)
 * @param tableTests  Published table test entries (may be empty)
 */
public record TestClassData(
        String className,
        String slug,
        String title,
        String description,
        java.util.List<PublishedTableTest> tableTests) {

    public TestClassData(String title, String description) {
        this(null, null, title, description, java.util.List.of());
    }

    /**
     * Converts this test class data to a map suitable for YAML serialisation.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (className != null) map.put("className", className);
        if (slug != null) map.put("slug", slug);
        if (title != null) map.put("title", title);
        if (description != null) map.put("description", description);
        if (tableTests != null && !tableTests.isEmpty()) {
            map.put(
                    "tableTests",
                    tableTests.stream().map(PublishedTableTest::toMap).toList());
        }
        return map;
    }
}
