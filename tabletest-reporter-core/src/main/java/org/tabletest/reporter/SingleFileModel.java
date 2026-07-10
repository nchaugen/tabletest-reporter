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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assembles the render context for a single-file HTML report: the whole tree flattened into one
 * document. Every page becomes an anchored section (pre-order), the sidebar navigation and search
 * index target in-page {@code #anchor}s instead of files, and the search index is inlined so the
 * result needs no sibling assets. Pure over the report tree — building the model has no side
 * effects; the reporter renders it once and writes a single file.
 */
final class SingleFileModel {

    private static final int MAX_HEADING_LEVEL = 6;

    private SingleFileModel() {}

    static Map<String, Object> of(ReportNode root) {
        Map<String, Object> context = new HashMap<>();
        context.put("title", NavModel.label(root));
        context.put("description", description(root));
        context.put("sections", sections(root));
        context.put("nav", NavModel.build(root, null, SingleFileModel::anchorHref));
        context.put(
                "searchData", SearchIndex.of(root, SingleFileModel::anchorHref).asJavaScript());
        context.put("assetRoot", "");
        return context;
    }

    /** In-page anchor id for a node, derived from its unique output path (root anchors the top). */
    static String anchor(ReportNode node) {
        String outPath = node.outPath() == null ? "" : node.outPath().replaceAll("^/+", "");
        return outPath.isEmpty() ? "top" : outPath.replace("/", "__");
    }

    private static String anchorHref(ReportNode node) {
        return "#" + anchor(node);
    }

    private static List<Map<String, Object>> sections(ReportNode root) {
        List<Map<String, Object>> sections = new ArrayList<>();
        if (root instanceof IndexNode index) {
            index.contents().forEach(child -> collectSections(child, 1, sections));
        }
        return sections;
    }

    private static void collectSections(ReportNode node, int depth, List<Map<String, Object>> sections) {
        sections.add(section(node, depth));
        if (node instanceof IndexNode index) {
            index.contents().forEach(child -> collectSections(child, depth + 1, sections));
        }
    }

    private static Map<String, Object> section(ReportNode node, int depth) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("anchor", anchor(node));
        section.put("title", NavModel.label(node));
        section.put("type", node.type());
        section.put("status", StatusRollup.of(node).state());
        section.put("level", Math.min(depth + 1, MAX_HEADING_LEVEL));
        section.put("description", description(node));
        if (node instanceof TableNode table) {
            Map<String, Object> resource = table.resource();
            section.put("headers", resource.getOrDefault("headers", List.of()));
            section.put("rows", resource.getOrDefault("rows", List.of()));
            section.put("rowResults", resource.getOrDefault("rowResults", List.of()));
        }
        return section;
    }

    private static Object description(ReportNode node) {
        return node.resource() != null ? node.resource().get("description") : null;
    }
}
