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
import java.util.Optional;

/**
 * Parses the JUnit platform reporting output directory from an override value
 * or the system property {@code junit.platform.reporting.output.dir}.
 */
public final class JunitDirParser {

    private static final String JUNIT_PLATFORM_REPORTING_OUTPUT_DIR = "junit.platform.reporting.output.dir";

    private JunitDirParser() {}

    /**
     * Resolves the JUnit output directory path from an override string or system property.
     * Returns empty if no value is available.
     *
     * @param baseDir directory to resolve relative paths against
     * @param override explicit override value, or null/blank to fall back to system property
     * @return resolved absolute or base-relative path, or empty if no value found
     */
    public static Optional<Path> parse(Path baseDir, String override) {
        String value = override == null || override.isBlank()
                ? System.getProperty(JUNIT_PLATFORM_REPORTING_OUTPUT_DIR)
                : override;
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        Path path = Path.of(value);
        return Optional.of(path.isAbsolute() ? path : baseDir.resolve(path));
    }
}
