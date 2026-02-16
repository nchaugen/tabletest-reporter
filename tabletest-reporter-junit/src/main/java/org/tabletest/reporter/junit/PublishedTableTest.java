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
 * Represents a published table test entry in a class YAML index.
 *
 * @param path       Relative path to the table test YAML file
 * @param title      Display title for the table test
 * @param methodName Underlying test method name
 * @param slug       Slugified method name used in output paths
 */
public record PublishedTableTest(String path, String title, String methodName, String slug) {

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (path != null) map.put("path", path);
        if (title != null) map.put("title", title);
        if (methodName != null) map.put("methodName", methodName);
        if (slug != null) map.put("slug", slug);
        return map;
    }
}
