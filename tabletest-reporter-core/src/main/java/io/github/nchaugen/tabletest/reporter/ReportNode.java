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

import java.util.List;

/**
 * Represents a node in the report structure tree.
 * Nodes can be either index pages (with children) or table leaf pages.
 */
public sealed interface ReportNode permits IndexNode, TableNode {
    String name();
    String outPath();
    String resource();
    String type();
}

/**
 * An index node representing a package or test class directory.
 * Contains links to child nodes (other indexes or tables).
 */
record IndexNode(
    String name,
    String outPath,
    String resource,
    List<ReportNode> contents
) implements ReportNode {

    public IndexNode {
        contents = List.copyOf(contents);
    }

    /**
     * Template compatibility: Pebble templates check node type.
     */
    public String type() {
        return "index";
    }
}

/**
 * A table leaf node representing a single TableTest method's report.
 * Always has an associated YAML resource file.
 */
record TableNode(
    String name,
    String outPath,
    String resource
) implements ReportNode {

    public TableNode {
        if (resource == null || resource.isBlank()) {
            throw new IllegalArgumentException("TableNode must have a resource");
        }
    }

    /**
     * Template compatibility: Pebble templates check node type.
     */
    public String type() {
        return "table";
    }
}
