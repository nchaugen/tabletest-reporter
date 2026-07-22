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
import java.util.Optional;

/**
 * Curation metadata for one chapter of a spec: which report node it names, the human title to
 * show in place of the leaked package segment, and the ordered sub-chapters beneath it. A
 * chapter matches a node by {@code name} (its path segment / slug) within its parent's children,
 * so the same short name is unambiguous because it is scoped by nesting. The order chapters
 * appear in is the reading order applied to the matched siblings.
 *
 * @param name the path segment / slug of the node this chapter names
 * @param title the human title to render, or null to reorder without retitling
 * @param chapters the ordered sub-chapters, empty when this chapter has no declared children
 */
public record ChapterMetadata(String name, String title, List<ChapterMetadata> chapters) {

    public ChapterMetadata {
        chapters = List.copyOf(chapters);
    }

    /**
     * Parses one chapter entry from a raw YAML map, ignoring an entry with no {@code name} (there
     * is nothing to match a node on). Returns empty for anything that is not a well-formed entry.
     */
    static Optional<ChapterMetadata> parse(Object entry) {
        if (!(entry instanceof Map<?, ?> map)) {
            return Optional.empty();
        }
        String name = SpecMetadata.stringValue(map, "name");
        if (name == null) {
            return Optional.empty();
        }
        return Optional.of(new ChapterMetadata(
                name, SpecMetadata.stringValue(map, "title"), SpecMetadata.parseChapters(map.get("chapters"))));
    }
}
