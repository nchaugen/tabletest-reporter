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

/**
 * Computes relative hyperlinks between report pages. Every href is relative to the linking
 * page's own directory, never root-absolute, so the generated tree can be served unchanged
 * from any subpath (e.g. GitHub project Pages under {@code /<repo>/}).
 */
final class NavLinks {

    private NavLinks() {}

    /**
     * The output directory a node's page is written into, relative to the output root: an
     * index sits in its own directory as {@code index.html}, a table as {@code <name>.html}
     * in its parent directory.
     */
    static Path pageDirectory(ReportNode node) {
        Path self = Path.of("./" + node.outPath());
        if (node instanceof TableNode) {
            Path parent = self.getParent();
            return parent != null ? parent : Path.of(".");
        }
        return self;
    }

    /**
     * The relative href from a page directory to the target node's page, including the
     * {@code .html} / {@code index.html} filename and using forward slashes throughout.
     */
    static String href(Path fromDirectory, ReportNode target) {
        String relative = fromDirectory
                .relativize(Path.of("./" + target.outPath()))
                .toString()
                .replace('\\', '/');
        if (target instanceof IndexNode) {
            return relative.isEmpty() ? "index.html" : relative + "/index.html";
        }
        return relative + ".html";
    }
}
