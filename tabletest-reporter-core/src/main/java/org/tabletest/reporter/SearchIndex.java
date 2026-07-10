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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The whole-report search index: one entry per page, each with the page's root-relative path,
 * display title, roll-up status, and a searchable text blob flattened from the page's own
 * title, description, headers, and cell values. Pure over the report tree — building it has no
 * side effects; the reporter writes {@link #asJavaScript()} once to the output root and every
 * page loads it to search across the whole report.
 */
final class SearchIndex {

    /** File name of the emitted shared asset, relative to the output root. */
    static final String ASSET_NAME = "tabletest-search-index.js";

    private static final String GLOBAL = "window.TableTestSearchIndex";

    private final List<Map<String, Object>> entries;

    private SearchIndex(List<Map<String, Object>> entries) {
        this.entries = entries;
    }

    static SearchIndex of(ReportNode root) {
        List<Map<String, Object>> entries = new ArrayList<>();
        collectEntries(root, entries);
        return new SearchIndex(List.copyOf(entries));
    }

    List<Map<String, Object>> entries() {
        return entries;
    }

    /**
     * The pages matching a query: a case-insensitive substring match of the trimmed query against
     * each entry's title and searchable text. A blank query matches nothing (the drawer shows the
     * navigation tree instead of results). This is the search contract; the inline
     * {@code searchScript()} client mirrors it in the browser (adding only a display cap).
     */
    List<Map<String, Object>> search(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return List.of();
        }
        return entries.stream()
                .filter(entry -> haystack(entry).contains(needle))
                .toList();
    }

    private static String haystack(Map<String, Object> entry) {
        return (entry.get("title") + " " + entry.get("text")).toLowerCase(Locale.ROOT);
    }

    String asJavaScript() {
        return GLOBAL + " = " + Json.encode(entries) + ";\n";
    }

    private static void collectEntries(ReportNode node, List<Map<String, Object>> entries) {
        entries.add(entryFor(node));
        if (node instanceof IndexNode index) {
            index.contents().forEach(child -> collectEntries(child, entries));
        }
    }

    private static Map<String, Object> entryFor(ReportNode node) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("path", NavLinks.rootPath(node));
        entry.put("title", title(node));
        entry.put("type", node.type());
        entry.put("status", StatusRollup.of(node).state());
        entry.put("text", searchableText(node));
        return entry;
    }

    private static String title(ReportNode node) {
        Object title = node.resource() != null ? node.resource().get("title") : null;
        if (title != null) {
            return title.toString();
        }
        return node.name() != null ? node.name() : "Home";
    }

    private static String searchableText(ReportNode node) {
        List<String> parts = new ArrayList<>();
        addIfPresent(parts, node.name());
        Map<String, Object> resource = node.resource();
        if (resource != null) {
            addIfPresent(parts, asString(resource.get("title")));
            addIfPresent(parts, asString(resource.get("description")));
            collectCells(resource.get("headers"), parts);
            collectRows(resource.get("rows"), parts);
        }
        return String.join(" ", parts);
    }

    private static void collectRows(Object rows, List<String> parts) {
        if (rows instanceof List<?> list) {
            list.forEach(row -> collectCells(row, parts));
        }
    }

    private static void collectCells(Object cells, List<String> parts) {
        if (cells instanceof List<?> list) {
            list.forEach(cell -> {
                if (cell instanceof Map<?, ?> map) {
                    collectValue(map.get("value"), parts);
                }
            });
        }
    }

    private static void collectValue(Object value, List<String> parts) {
        switch (value) {
            case null -> {}
            case Map<?, ?> map ->
                map.forEach((key, nested) -> {
                    addIfPresent(parts, asString(key));
                    collectValue(nested, parts);
                });
            case Iterable<?> iterable -> iterable.forEach(element -> collectValue(element, parts));
            default -> addIfPresent(parts, value.toString());
        }
    }

    private static void addIfPresent(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value);
        }
    }

    private static String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}
