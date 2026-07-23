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
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.joining;

/**
 * Several explicitly configured input directories, split into the ones that exist and the ones that
 * do not — the shape a multi-module report needs, where a module that has not been built yet should
 * be reported as absent rather than fail the whole spec. Relative paths resolve against the project
 * directory, order is as declared, and repeats collapse. This is the resolution the Maven mojo, CLI
 * and Gradle task share for a configured list; a single unconfigured directory is auto-detected by
 * {@link InputDirectoryResolver} instead.
 *
 * @param present the configured directories that exist, in declared order
 * @param missing the configured directories that do not exist, in declared order
 */
public record InputDirectories(List<Path> present, List<Path> missing) {

    public InputDirectories {
        present = List.copyOf(present);
        missing = List.copyOf(missing);
    }

    /**
     * Resolves configured directories against a project directory, sorting them into present and
     * missing.
     *
     * @param configured the directories as configured, absolute or relative to {@code baseDir}
     * @param baseDir the project directory relative paths resolve against, or null for the working
     *     directory
     */
    public static InputDirectories resolve(List<Path> configured, Path baseDir) {
        Path base = baseDir != null ? baseDir : Path.of(".");
        List<Path> normalized = (configured != null ? configured : List.<Path>of())
                .stream()
                        .filter(Objects::nonNull)
                        .map(dir -> dir.isAbsolute()
                                ? dir.normalize()
                                : base.resolve(dir).normalize())
                        .distinct()
                        .toList();
        return new InputDirectories(
                normalized.stream().filter(Files::isDirectory).toList(),
                normalized.stream().filter(dir -> !Files.isDirectory(dir)).toList());
    }

    /** True when none of the configured directories exists, so there is nothing to report from. */
    public boolean isEmpty() {
        return present.isEmpty();
    }

    /** Names every configured directory, marking the ones that are not there. */
    public String formatMissingInputMessage() {
        return "No input directory exists among the configured locations:" + System.lineSeparator()
                + missing.stream().map(dir -> "  - " + dir.toAbsolutePath()).collect(joining(System.lineSeparator()));
    }

    /** Names the configured directories that are not there, for a warning that does not stop the report. */
    public String formatSkippedInputMessage() {
        return "Skipping input directories that do not exist: "
                + missing.stream().map(dir -> dir.toAbsolutePath().toString()).collect(joining(", "));
    }
}
