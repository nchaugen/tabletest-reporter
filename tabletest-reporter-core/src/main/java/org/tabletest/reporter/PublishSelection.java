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
 * Which pages of a spec publish, decided at report time rather than by how the test suite was
 * tagged or run. Every page publishes unless an {@code exclude} path names it — a page named there
 * takes its whole subtree with it — and an {@code include} path re-admits a page below an excluded
 * one, so a single rule table can still publish from an otherwise internal class. Paths are matched
 * as {@link PublishPattern}s against the page path shown in the report's URLs. Sourced from the
 * {@code publish} section of {@code tabletest-reporter.yaml} and applied to the built tree, so
 * changing what publishes never means running the suite again.
 *
 * @param exclude the paths whose pages are held back, in declared order
 * @param include the paths re-admitted below an excluded page, in declared order
 */
public record PublishSelection(List<String> exclude, List<String> include) {

    /** The absent case: no selection, so every page publishes. */
    public static final PublishSelection EMPTY = new PublishSelection(List.of(), List.of());

    public PublishSelection {
        exclude = List.copyOf(exclude);
        include = List.copyOf(include);
    }

    /**
     * Parses the selection from a raw YAML map (as loaded by {@link ContextLoader}), reading its
     * {@code publish} section. A map without that section, or with nothing usable in it, yields
     * {@link #EMPTY}.
     */
    public static PublishSelection parse(Map<String, Object> yaml) {
        if (yaml == null || !(yaml.get("publish") instanceof Map<?, ?> publish)) {
            return EMPTY;
        }
        return new PublishSelection(parsePaths(publish.get("exclude")), parsePaths(publish.get("include")));
    }

    /** True when this selection holds nothing back, so applying it is a no-op. */
    public boolean isEmpty() {
        return exclude.isEmpty() && include.isEmpty();
    }

    /**
     * Returns the report tree holding only the pages this selection publishes: an excluded page and
     * its subtree are dropped, an included page below one is kept, and a feature page left with no
     * published pages under it drops with them. Returns the same tree unchanged when this selection
     * {@link #isEmpty() is empty}.
     */
    public ReportNode applyTo(ReportNode root) {
        return PublishSelectionApplier.apply(root, this);
    }

    private static List<String> parsePaths(Object value) {
        if (!(value instanceof List<?> entries)) {
            return List.of();
        }
        return entries.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(path -> !path.isBlank())
                .toList();
    }
}
