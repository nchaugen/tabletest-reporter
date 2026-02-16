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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class TargetPathResolver {

    private static final Path ROOT_PATH = Path.of("." + File.separator);

    private TargetPathResolver() {}

    static Path classPathFromClassName(String className) {
        int lastDot = className.lastIndexOf('.');
        String packagePart = lastDot >= 0 ? className.substring(0, lastDot) : "";
        String classPart = lastDot >= 0 ? className.substring(lastDot + 1) : className;
        List<String> segments = new ArrayList<>();
        if (!packagePart.isBlank()) {
            segments.addAll(Arrays.asList(packagePart.split("\\.")));
        }
        if (!classPart.isBlank()) {
            segments.addAll(Arrays.asList(classPart.split("\\$")));
        }
        if (segments.isEmpty()) {
            return ROOT_PATH;
        }
        return Path.of(segments.getFirst(), segments.subList(1, segments.size()).toArray(String[]::new));
    }

    static Path resolveTableResource(Path classResource, String tablePath) {
        if (tablePath == null || tablePath.isBlank()) {
            return null;
        }
        Path baseDir = classResource != null ? classResource.getParent() : null;
        Path relative = Path.of(tablePath);
        Path resolved = baseDir != null ? baseDir.resolve(relative) : relative;
        return resolved.normalize();
    }
}
