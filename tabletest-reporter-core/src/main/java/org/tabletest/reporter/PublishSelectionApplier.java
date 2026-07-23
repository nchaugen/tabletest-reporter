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
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Applies a {@link PublishSelection} onto an already-built report tree, keeping only the pages that
 * publish: an excluded page is dropped with everything below it, an included page below an excluded
 * one is kept, and a feature page whose pages have all gone drops with them. The root index always
 * publishes, so a selection can never leave a report without an entry page. A declared path that
 * names no page is logged and skipped, so a curation typo never fails a report. The tree is rebuilt
 * immutably; the builder's output is left untouched, which is why an empty selection returns the
 * very same tree.
 */
final class PublishSelectionApplier {

    private static final Logger LOGGER = System.getLogger(PublishSelectionApplier.class.getName());

    private PublishSelectionApplier() {}

    static ReportNode apply(ReportNode root, PublishSelection selection) {
        if (selection.isEmpty() || !(root instanceof IndexNode index)) {
            return root;
        }
        warnUnmatchedPaths(selection, pagePaths(index, List.of()).toList());
        Patterns patterns = Patterns.of(selection);
        return new IndexNode(
                index.name(), index.outPath(), index.resource(), published(index, List.of(), patterns, true));
    }

    private static List<ReportNode> published(
            IndexNode index, List<String> path, Patterns patterns, boolean publishedByDefault) {
        return index.contents().stream()
                .map(child -> publishedPage(child, append(path, child.name()), patterns, publishedByDefault))
                .flatMap(Optional::stream)
                .toList();
    }

    private static Optional<ReportNode> publishedPage(
            ReportNode page, List<String> path, Patterns patterns, boolean publishedByDefault) {
        boolean publishes = patterns.publishes(path, publishedByDefault);
        return switch (page) {
            case TableNode table -> publishes ? Optional.of(table) : Optional.empty();
            case IndexNode index -> {
                List<ReportNode> contents = published(index, path, patterns, publishes);
                yield contents.isEmpty()
                        ? Optional.empty()
                        : Optional.of(new IndexNode(index.name(), index.outPath(), index.resource(), contents));
            }
        };
    }

    private static void warnUnmatchedPaths(PublishSelection selection, List<List<String>> pagePaths) {
        Stream.concat(selection.exclude().stream(), selection.include().stream())
                .filter(declared -> pagePaths.stream()
                        .noneMatch(pagePath -> PublishPattern.parse(declared).matches(pagePath)))
                .forEach(
                        declared -> LOGGER.log(Level.WARNING, "No report page matches publish path ''{0}''", declared));
    }

    private static Stream<List<String>> pagePaths(ReportNode node, List<String> path) {
        if (!(node instanceof IndexNode index)) {
            return Stream.of();
        }
        return index.contents().stream().flatMap(child -> {
            List<String> childPath = append(path, child.name());
            return Stream.concat(Stream.of(childPath), pagePaths(child, childPath));
        });
    }

    private static List<String> append(List<String> path, String name) {
        List<String> extended = new ArrayList<>(path);
        extended.add(name);
        return List.copyOf(extended);
    }

    /** The selection's paths compiled once, and the rule deciding whether one page publishes. */
    private record Patterns(List<PublishPattern> excluded, List<PublishPattern> included) {

        static Patterns of(PublishSelection selection) {
            return new Patterns(compile(selection.exclude()), compile(selection.include()));
        }

        /** Including a page overrides excluding it; a page named by neither inherits its parent's fate. */
        boolean publishes(List<String> path, boolean publishedByDefault) {
            if (matchesAny(included, path)) {
                return true;
            }
            if (matchesAny(excluded, path)) {
                return false;
            }
            return publishedByDefault;
        }

        private static List<PublishPattern> compile(List<String> paths) {
            return paths.stream().map(PublishPattern::parse).toList();
        }

        private static boolean matchesAny(List<PublishPattern> patterns, List<String> path) {
            return patterns.stream().anyMatch(pattern -> pattern.matches(path));
        }
    }
}
