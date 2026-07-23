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
import java.util.Map;

/**
 * The report-level settings a project declares in its {@code tabletest-reporter.yaml} sidecar file.
 * The file is read once per report run and yields every curation model it carries. An absent path,
 * or one that is not a readable regular file, yields {@link #EMPTY} so a project without the file
 * reports exactly as before. Every entry point resolves the file's location the same way — an
 * explicit configuration path, else {@link #DEFAULT_FILE_NAME} in the project directory — and hands
 * the resulting path here through {@link ReportOptions}.
 *
 * @param specMetadata the spec title, intro and feature ordering declared in the file
 * @param publishSelection the pages held back or re-admitted by the file's publish section
 */
public record ReportConfigFile(SpecMetadata specMetadata, PublishSelection publishSelection) {

    /** The conventional sidecar file name, looked for in the project directory by default. */
    public static final String DEFAULT_FILE_NAME = "tabletest-reporter.yaml";

    /** The absent case: the settings a project with no sidecar file reports under. */
    public static final ReportConfigFile EMPTY = new ReportConfigFile(SpecMetadata.EMPTY, PublishSelection.EMPTY);

    private static final ContextLoader LOADER = new ContextLoader();

    /**
     * Reads the settings declared at the given configuration file path.
     *
     * @param configFile the resolved sidecar path, or null when none applies
     * @return the parsed settings, or {@link #EMPTY} when the file is absent
     */
    public static ReportConfigFile read(Path configFile) {
        if (configFile == null || !Files.isRegularFile(configFile)) {
            return EMPTY;
        }
        return parse(LOADER.fromYaml(configFile));
    }

    /** Parses the settings from a raw YAML map, yielding {@link #EMPTY} for an empty document. */
    static ReportConfigFile parse(Map<String, Object> yaml) {
        if (yaml == null || yaml.isEmpty()) {
            return EMPTY;
        }
        return new ReportConfigFile(SpecMetadata.parse(yaml), PublishSelection.parse(yaml));
    }
}
