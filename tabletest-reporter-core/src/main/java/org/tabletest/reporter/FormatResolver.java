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
package org.tabletest.reporter;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Resolves format names to Format instances.
 *
 * <p>This class handles both built-in formats (asciidoc, markdown) and
 * custom formats discovered from template directories.
 */
public final class FormatResolver {

    private FormatResolver() {}

    /**
     * Resolves a format name to a Format instance.
     *
     * <p>First checks for built-in format aliases (adoc, asciidoc, md, markdown).
     * If not found, checks for custom formats in the template directory.
     *
     * @param formatName the format name to resolve
     * @param templateDirectory the template directory to search for custom formats (may be null)
     * @return the resolved Format
     * @throws IllegalArgumentException if the format is not found
     */
    public static Format resolve(String formatName, Path templateDirectory) {
        if (formatName == null || formatName.isBlank()) {
            return BuiltInFormat.ASCIIDOC;
        }

        String normalizedName = formatName.trim().toLowerCase();

        // Check built-in formats first
        Format builtInFormat =
                switch (normalizedName) {
                    case "adoc", "asciidoc", "asciidoctor" -> BuiltInFormat.ASCIIDOC;
                    case "md", "markdown" -> BuiltInFormat.MARKDOWN;
                    default -> null;
                };

        if (builtInFormat != null) {
            return builtInFormat;
        }

        // Get all available formats (built-in + custom) and check if requested format exists
        Set<String> availableFormats = getAvailableFormats(templateDirectory);

        if (availableFormats.contains(normalizedName)) {
            return new CustomFormat(normalizedName);
        }

        // Format not found - provide helpful error message
        String formatsStr = availableFormats.stream().sorted().collect(Collectors.joining(", "));

        throw new IllegalArgumentException("Unknown format: " + formatName + ". Available formats: " + formatsStr);
    }

    /**
     * Returns all available formats (built-in and custom).
     *
     * @param templateDirectory the template directory to search for custom formats (may be null)
     * @return set of available format names
     */
    public static Set<String> getAvailableFormats(Path templateDirectory) {
        Set<String> builtInFormats =
                Stream.of(BuiltInFormat.values()).map(Format::formatName).collect(Collectors.toSet());

        if (templateDirectory != null) {
            Set<String> customFormats = FormatDiscovery.discoverFormats(templateDirectory);
            return Stream.concat(builtInFormats.stream(), customFormats.stream())
                    .collect(Collectors.toSet());
        }

        return builtInFormats;
    }
}
