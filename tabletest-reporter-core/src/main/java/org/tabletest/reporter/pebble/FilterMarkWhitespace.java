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
package org.tabletest.reporter.pebble;

import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Pebble filter ({@code value | markWhitespace}) producing the HTML for a
 * whitespace-significant literal: the text HTML-escaped, with each significant whitespace
 * run wrapped in marker spans ({@code sp} around space runs, {@code tab} around each tab)
 * for the stylesheet to draw per-character markers on. A run is significant when it
 * contains a tab, spans two or more characters, or touches a line boundary; single spaces
 * between words stay unmarked so ordinary prose renders clean. The real characters remain
 * in the DOM for copy and search.
 */
public class FilterMarkWhitespace implements Filter {

    public static final String NAME = "markWhitespace";

    private static final String WHITESPACE_RUN = "[ \\t]+";
    private static final String SPACE_RUN_MARKUP = "<span class=\"sp\">%s</span>";
    private static final String TAB_MARKUP = "<span class=\"tab\">\t</span>";

    @Override
    public Object apply(
            Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
        if (input == null) return null;
        return Arrays.stream(input.toString().split("\n", -1))
                .map(FilterMarkWhitespace::markedLine)
                .collect(Collectors.joining("\n"));
    }

    private static String markedLine(String line) {
        String[] segments = line.splitWithDelimiters(WHITESPACE_RUN, -1);
        return IntStream.range(0, segments.length)
                .mapToObj(index -> isWhitespaceRun(index)
                        ? (isSignificant(segments, index) ? markedRun(segments[index]) : segments[index])
                        : escapeHtml(segments[index]))
                .collect(Collectors.joining());
    }

    private static boolean isWhitespaceRun(int index) {
        return index % 2 == 1;
    }

    private static boolean isSignificant(String[] segments, int index) {
        String run = segments[index];
        boolean touchesLineStart = index == 1 && segments[0].isEmpty();
        boolean touchesLineEnd = index == segments.length - 2 && segments[segments.length - 1].isEmpty();
        return run.contains("\t") || run.length() >= 2 || touchesLineStart || touchesLineEnd;
    }

    private static String markedRun(String run) {
        return Arrays.stream(run.splitWithDelimiters("\\t", -1))
                .filter(part -> !part.isEmpty())
                .map(part -> part.startsWith("\t") ? TAB_MARKUP : SPACE_RUN_MARKUP.formatted(part))
                .collect(Collectors.joining());
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    @Override
    public List<String> getArgumentNames() {
        return List.of();
    }
}
