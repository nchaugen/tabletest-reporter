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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;

/**
 * One entry of a {@link PublishSelection}: a slash-separated path of report page names naming the
 * pages it selects. A segment matches one page name, where {@code *} stands for any part of a name
 * and a whole segment of {@code **} stands for any number of nested levels (including none). The
 * path matched against is the page's path below the root index — the same path its URL shows.
 *
 * @param segments the pattern's path segments, separator noise already stripped
 */
record PublishPattern(List<String> segments) {

    private static final String ANY_DEPTH = "**";

    PublishPattern {
        segments = List.copyOf(segments);
    }

    /** Reads a pattern as written in the config file, ignoring surrounding and repeated separators. */
    static PublishPattern parse(String text) {
        return new PublishPattern(Arrays.stream(text.trim().split("/"))
                .filter(segment -> !segment.isBlank())
                .toList());
    }

    /** True when this pattern names the page at the given path of page names. */
    boolean matches(List<String> pagePath) {
        return matchesFrom(0, pagePath, 0);
    }

    private boolean matchesFrom(int segmentIndex, List<String> pagePath, int pathIndex) {
        if (segmentIndex == segments.size()) {
            return pathIndex == pagePath.size();
        }
        String segment = segments.get(segmentIndex);
        if (ANY_DEPTH.equals(segment)) {
            return matchesAtAnyDepth(segmentIndex, pagePath, pathIndex);
        }
        return pathIndex < pagePath.size()
                && nameMatches(segment, pagePath.get(pathIndex))
                && matchesFrom(segmentIndex + 1, pagePath, pathIndex + 1);
    }

    private boolean matchesAtAnyDepth(int segmentIndex, List<String> pagePath, int pathIndex) {
        return java.util.stream.IntStream.rangeClosed(pathIndex, pagePath.size())
                .anyMatch(skipTo -> matchesFrom(segmentIndex + 1, pagePath, skipTo));
    }

    private static boolean nameMatches(String segment, String pageName) {
        return segment.indexOf('*') < 0 ? segment.equals(pageName) : Pattern.matches(asRegex(segment), pageName);
    }

    private static String asRegex(String segment) {
        return Arrays.stream(segment.split("\\*", -1)).map(Pattern::quote).collect(joining(".*"));
    }
}
