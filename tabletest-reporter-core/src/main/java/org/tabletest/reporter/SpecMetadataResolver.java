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

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads {@link SpecMetadata} from the {@code tabletest-reporter.yaml} sidecar file. An absent path,
 * or one that is not a readable regular file, yields {@link SpecMetadata#EMPTY} so a project without
 * the file reports exactly as before. Every entry point resolves the file's location the same way —
 * an explicit configuration path, else {@link #DEFAULT_FILE_NAME} in the project directory — and
 * hands the resulting path here through {@link ReportOptions}.
 */
public final class SpecMetadataResolver {

    /** The conventional sidecar file name, looked for in the project directory by default. */
    public static final String DEFAULT_FILE_NAME = "tabletest-reporter.yaml";

    private static final ContextLoader LOADER = new ContextLoader();

    private SpecMetadataResolver() {}

    /**
     * Resolves the spec metadata at the given configuration file path.
     *
     * @param configFile the resolved sidecar path, or null when none applies
     * @return the parsed metadata, or {@link SpecMetadata#EMPTY} when the file is absent
     */
    public static SpecMetadata resolve(Path configFile) {
        if (configFile == null || !Files.isRegularFile(configFile)) {
            return SpecMetadata.EMPTY;
        }
        return SpecMetadata.parse(LOADER.fromYaml(configFile));
    }
}
