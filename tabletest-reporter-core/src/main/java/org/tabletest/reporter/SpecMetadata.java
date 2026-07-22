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

import java.util.List;
import java.util.Map;

/**
 * Report-time curation for a whole spec: the spec title and intro paragraph shown on the root
 * index, plus an ordered chapter tree that retitles intermediate index pages and imposes an
 * explicit reading order on them. Sourced from a {@code tabletest-reporter.yaml} sidecar file and
 * applied on top of the deterministic tree the builder produces, so no metadata leaves the report
 * output unchanged from today's behaviour. Every field is optional: {@link #EMPTY} is the absent
 * case.
 *
 * @param title the spec title for the root index, or null to keep the derived name
 * @param intro the intro paragraph for the root index, or null for none
 * @param chapters the ordered top-level chapters, empty when none are declared
 */
public record SpecMetadata(String title, String intro, List<ChapterMetadata> chapters) {

    /** The absent case: no title, no intro, no chapters. Applying it leaves a tree unchanged. */
    public static final SpecMetadata EMPTY = new SpecMetadata(null, null, List.of());

    public SpecMetadata {
        chapters = List.copyOf(chapters);
    }

    /**
     * Parses spec metadata from a raw YAML map (as loaded by {@link ContextLoader}). A null map or
     * one with none of the recognised keys yields {@link #EMPTY}.
     */
    public static SpecMetadata parse(Map<String, Object> yaml) {
        if (yaml == null || yaml.isEmpty()) {
            return EMPTY;
        }
        return new SpecMetadata(
                stringValue(yaml, "title"), stringValue(yaml, "intro"), parseChapters(yaml.get("chapters")));
    }

    /** True when this metadata carries nothing, so applying it is a no-op. */
    public boolean isEmpty() {
        return title == null && intro == null && chapters.isEmpty();
    }

    /**
     * Returns the report tree with this metadata applied: the spec title and intro on the root
     * index, declared chapters retitled and reordered ahead of their alphabetical siblings.
     * Returns the same tree unchanged when this metadata {@link #isEmpty() is empty}.
     */
    public ReportNode applyTo(ReportNode root) {
        return SpecMetadataApplier.apply(root, this);
    }

    static List<ChapterMetadata> parseChapters(Object value) {
        if (!(value instanceof List<?> entries)) {
            return List.of();
        }
        return entries.stream()
                .map(ChapterMetadata::parse)
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    static String stringValue(Map<?, ?> map, String key) {
        return map.get(key) instanceof String s && !s.isBlank() ? s : null;
    }
}
