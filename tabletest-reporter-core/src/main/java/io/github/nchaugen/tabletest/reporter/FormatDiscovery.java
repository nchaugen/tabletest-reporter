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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Discovers custom formats from template directories.
 *
 * <p>A valid custom format must have both {@code table.{format}.peb}
 * and {@code index.{format}.peb} template files present.
 */
public final class FormatDiscovery {

    private static final Pattern TABLE_TEMPLATE_PATTERN = Pattern.compile("table\\.([^.]+)\\.peb");
    private static final Pattern INDEX_TEMPLATE_PATTERN = Pattern.compile("index\\.([^.]+)\\.peb");

    private FormatDiscovery() {
    }

    /**
     * Discovers all valid custom formats in the given template directory.
     *
     * @param templateDirectory the directory to scan for templates
     * @return set of format names that have both table and index templates
     */
    public static Set<String> discoverFormats(Path templateDirectory) {
        if (templateDirectory == null || !Files.isDirectory(templateDirectory)) {
            return Set.of();
        }

        Set<String> tableFormats = findFormats(templateDirectory, TABLE_TEMPLATE_PATTERN);
        Set<String> indexFormats = findFormats(templateDirectory, INDEX_TEMPLATE_PATTERN);

        tableFormats.retainAll(indexFormats);
        return tableFormats;
    }

    private static Set<String> findFormats(Path directory, Pattern pattern) {
        Set<String> formats = new HashSet<>();

        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                .map(Path::getFileName)
                .map(Path::toString)
                .forEach(filename -> {
                    Matcher matcher = pattern.matcher(filename);
                    if (matcher.matches()) {
                        formats.add(matcher.group(1));
                    }
                });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to scan template directory: " + directory, e);
        }

        return formats;
    }
}
