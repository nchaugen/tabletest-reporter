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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * Reads the JUnit platform reporting output directory from junit-platform.properties.
 */
public final class JunitPropertiesReader {

    private static final String PROPERTIES_PATH = "src/test/resources/junit-platform.properties";
    private static final String OUTPUT_DIR_KEY = "junit.platform.reporting.output.dir";

    private JunitPropertiesReader() {}

    /**
     * Result of reading the output directory property.
     *
     * @param value the raw property value
     * @param deterministic true if the value contains no placeholders like {uniqueNumber}
     */
    public record Result(String value, boolean deterministic) {}

    /**
     * Reads the JUnit platform reporting output directory from the properties file.
     *
     * @param baseDir project base directory containing src/test/resources
     * @return the result if the property is present and non-blank, or empty otherwise
     */
    public static Optional<Result> read(Path baseDir) {
        Properties properties = loadProperties(baseDir.resolve(PROPERTIES_PATH));
        String value = properties.getProperty(OUTPUT_DIR_KEY);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        boolean deterministic = !value.contains("{uniqueNumber}");
        return Optional.of(new Result(value, deterministic));
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
