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
 * Parses a JUnit platform reporting output directory value into a resolved path.
 */
public final class JunitDirParser {

    private JunitDirParser() {}

    /**
     * Parses the given directory value into a resolved path.
     * Returns empty if the value is null or blank.
     *
     * @param baseDir directory to resolve relative paths against
     * @param value directory value, or null/blank for empty result
     * @return resolved absolute or base-relative path, or empty if no value provided
     */
    public static Optional<Path> parse(Path baseDir, String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        Path path = Path.of(value);
        return Optional.of(path.isAbsolute() ? path : baseDir.resolve(path));
    }
}
