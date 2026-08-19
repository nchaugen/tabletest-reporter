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
 * Curation metadata for one feature of a spec: which report node it names, the human title to
 * show in place of the leaked package segment, the paragraph introducing the feature on its own
 * index page, and the ordered sub-features beneath it. A
 * feature matches a node by {@code name} (its path segment / slug) within its parent's children,
 * so the same short name is unambiguous because it is scoped by nesting. The order features
 * appear in is the reading order applied to the matched siblings.
 *
 * @param name the path segment / slug of the node this feature names
 * @param title the human title to render, or null to reorder without retitling
 * @param description the paragraph to introduce the feature with, or null to leave it bare
 * @param features the ordered sub-features, empty when this feature has no declared children
 */
public record FeatureMetadata(String name, String title, String description, List<FeatureMetadata> features) {

    public FeatureMetadata {
        features = List.copyOf(features);
    }

    /**
     * Parses one feature entry from a raw YAML map, ignoring an entry with no {@code name} (there
     * is nothing to match a node on). Returns empty for anything that is not a well-formed entry.
     */
    static Optional<FeatureMetadata> parse(Object entry) {
        if (!(entry instanceof Map<?, ?> map)) {
            return Optional.empty();
        }
        String name = SpecMetadata.stringValue(map, "name");
        if (name == null) {
            return Optional.empty();
        }
        return Optional.of(new FeatureMetadata(
                name,
                SpecMetadata.stringValue(map, "title"),
                SpecMetadata.stringValue(map, "description"),
                SpecMetadata.parseFeatures(map.get("features"))));
    }
}
