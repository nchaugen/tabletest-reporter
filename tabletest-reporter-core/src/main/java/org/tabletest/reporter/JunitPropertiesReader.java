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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Reads the JUnit platform reporting output directory from junit-platform.properties.
 */
public final class JunitPropertiesReader {

    private static final String PROPERTIES_PATH = "src/test/resources/junit-platform.properties";
    private static final String OUTPUT_DIR_KEY = "junit.platform.reporting.output.dir";

    private JunitPropertiesReader() {}

    /**
     * Resolves the JUnit platform reporting output directory from the properties file.
     * Placeholders like {uniqueNumber} are stripped, resolving to the parent path segment.
     * A bare {uniqueNumber} resolves to the base directory itself.
     *
     * @param baseDir project base directory containing src/test/resources
     * @return resolved path, or empty if the property is absent or blank
     */
    public static Optional<Path> resolve(Path baseDir) {
        Properties properties = loadProperties(baseDir.resolve(PROPERTIES_PATH));
        String value = properties.getProperty(OUTPUT_DIR_KEY);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String resolved = stripPlaceholders(value);
        return JunitDirParser.parse(baseDir, resolved);
    }

    private static String stripPlaceholders(String value) {
        if (!value.contains("{uniqueNumber}")) {
            return value;
        }
        String prefix = Arrays.stream(value.split("/"))
                .takeWhile(segment -> !segment.contains("{uniqueNumber}"))
                .collect(Collectors.joining("/"));
        return prefix.isEmpty() ? "." : prefix;
    }

    private static Properties loadProperties(Path path) {
        Properties properties = new Properties();
        if (!Files.exists(path)) {
            return properties;
        }
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        } catch (IOException e) {
            return properties;
        }
        return properties;
    }
}
