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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies {@link SpecMetadata} onto an already-built report tree: writes the spec title and intro
 * onto the root index, and for each declared chapter retitles the matched node and imposes the
 * declared reading order on its siblings — declared chapters first, the rest following in their
 * existing (alphabetical) order. A declared chapter that matches no node is logged and skipped, so
 * a curation typo never fails a report. The tree is rebuilt immutably; the builder's output is left
 * untouched, which is why empty metadata returns the very same tree.
 */
final class SpecMetadataApplier {

    private static final Logger LOGGER = System.getLogger(SpecMetadataApplier.class.getName());
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";

    private SpecMetadataApplier() {}

    static ReportNode apply(ReportNode root, SpecMetadata metadata) {
        if (metadata.isEmpty()) {
            return root;
        }
        Map<String, Object> resource = enriched(root.resource(), metadata.title(), metadata.intro());
        return rebuild(root, resource, reorder(childrenOf(root), metadata.chapters()));
    }

    private static List<ReportNode> reorder(List<ReportNode> children, List<ChapterMetadata> chapters) {
        if (chapters.isEmpty()) {
            return children;
        }
        List<ReportNode> declared = new ArrayList<>();
        List<ReportNode> remaining = new ArrayList<>(children);
        for (ChapterMetadata chapter : chapters) {
            ReportNode match = removeMatch(remaining, chapter.name());
            if (match == null) {
                LOGGER.log(Level.WARNING, "No report node matches declared chapter ''{0}''", chapter.name());
                continue;
            }
            declared.add(enrichChapter(match, chapter));
        }
        declared.addAll(remaining);
        return List.copyOf(declared);
    }

    private static ReportNode removeMatch(List<ReportNode> nodes, String name) {
        for (int i = 0; i < nodes.size(); i++) {
            if (name.equals(nodes.get(i).name())) {
                return nodes.remove(i);
            }
        }
        return null;
    }

    private static ReportNode enrichChapter(ReportNode node, ChapterMetadata chapter) {
        Map<String, Object> resource = enriched(node.resource(), chapter.title(), null);
        return rebuild(node, resource, reorder(childrenOf(node), chapter.chapters()));
    }

    private static List<ReportNode> childrenOf(ReportNode node) {
        return node instanceof IndexNode index ? index.contents() : List.of();
    }

    private static ReportNode rebuild(ReportNode node, Map<String, Object> resource, List<ReportNode> contents) {
        return switch (node) {
            case IndexNode index -> new IndexNode(index.name(), index.outPath(), resource, contents);
            case TableNode table -> new TableNode(table.name(), table.outPath(), resource);
        };
    }

    private static Map<String, Object> enriched(Map<String, Object> resource, String title, String intro) {
        Map<String, Object> copy = new LinkedHashMap<>(resource != null ? resource : Map.of());
        if (title != null) {
            copy.put(TITLE, title);
        }
        if (intro != null) {
            copy.put(DESCRIPTION, intro);
        }
        return copy;
    }
}
