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
package io.github.nchaugen.tabletest.reporter;

import java.nio.file.Path;

/**
 * Result of a TableTest report generation operation.
 *
 * @param filesGenerated number of documentation files generated
 * @param message informational message, typically used when no files were generated
 */
public record ReportResult(int filesGenerated, String message) {

    public static ReportResult empty(Path inputDir) {
        return new ReportResult(0, "No TableTest YAML files found in: " + inputDir);
    }

    public static ReportResult success(int count) {
        return new ReportResult(count, null);
    }
}
