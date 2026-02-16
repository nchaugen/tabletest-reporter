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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class TestOutputFileFinder {

    private static final String FILE_PREFIX = "TABLETEST-";
    private static final String FILE_EXTENSION = ".yaml";

    private TestOutputFileFinder() {}

    /**
     * Walks the given directory and returns all test output files as paths relative to the given directory.
     *
     * @param dir directory to traverse for .yaml files
     * @return List of relative paths to .yaml files
     */
    static List<Path> findTestOutputFiles(Path dir) {
        try (var paths = Files.walk(dir)) {
            return paths.filter(TestOutputFileFinder::isTestOutputFile)
                    .map(dir::relativize)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isTestOutputFile(Path path) {
        return path.toFile().isFile()
                && path.getFileName().toString().startsWith(FILE_PREFIX)
                && path.getFileName().toString().endsWith(FILE_EXTENSION);
    }
}
