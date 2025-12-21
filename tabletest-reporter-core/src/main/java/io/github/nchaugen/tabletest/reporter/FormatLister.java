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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lists available output formats.
 *
 * <p>Provides functionality to list all available output formats,
 * including both built-in formats and custom formats discovered from
 * template directories.
 */
public final class FormatLister {

    private FormatLister() {
    }

    /**
     * Lists all available output formats.
     *
     * <p>Returns a sorted list of format names, one per line.
     * Includes built-in formats (asciidoc, markdown) and custom formats
     * if a template directory is provided.
     *
     * @param templateDirectory optional template directory for discovering custom formats (may be null)
     * @return sorted list of format names, one per line
     */
    public static String listFormats(Path templateDirectory) {
        Set<String> formats = FormatResolver.getAvailableFormats(templateDirectory);
        return formats.stream()
            .sorted()
            .collect(Collectors.joining(System.lineSeparator()));
    }
}
