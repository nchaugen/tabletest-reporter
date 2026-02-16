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
import java.util.List;

public class ReportTree {

    /**
     * Processes the top-level directory where TableTest .yaml files have been created during
     * test run and builds a typed node hierarchy describing the desired report structure.
     *
     * @param dir directory to traverse for .yaml files
     * @return typed node hierarchy describing the desired report structure
     */
    public static ReportNode process(Path dir) {
        if (dir == null) {
            throw new IllegalArgumentException("argument `dir` cannot be null");
        }
        List<Path> files = TestOutputFileFinder.findTestOutputFiles(dir);
        List<Source> sources = SourceLoader.loadSources(dir, files);
        return TreeBuilder.buildTree(sources);
    }
}
