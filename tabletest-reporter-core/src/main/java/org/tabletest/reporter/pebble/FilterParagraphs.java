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
import java.util.regex.Pattern;

/**
 * Pebble filter ({@code text | paragraphs}) splitting text into its paragraphs. A blank line
 * separates one paragraph from the next. Inside a paragraph, a line that starts a list item
 * ({@code - }, {@code * }, {@code + }, or a number and a full stop such as {@code 12. }) keeps its line
 * break, and every other line
 * joins the line before it.
 * <p>
 * Markdown and AsciiDoc read a blank line as a paragraph break themselves. HTML collapses it,
 * along with every other line break, so a template renders each paragraph as its own element and
 * lets it flow to the width of the page; the description style keeps the line breaks of a list.
 */
public class FilterParagraphs implements Filter {

    public static final String NAME = "paragraphs";

    private static final String BLANK_LINE = "\\R[ \\t]*\\R";
    private static final String LINE_BREAK = "\\R";
    private static final Pattern LIST_ITEM = Pattern.compile("([-*+]|\\d+\\.)\\s.*");

    @Override
    public Object apply(
            Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
        if (input == null) {
            return List.of();
        }
        return Arrays.stream(input.toString().strip().split(BLANK_LINE))
                .map(FilterParagraphs::joinLines)
                .filter(paragraph -> !paragraph.isEmpty())
                .toList();
    }

    private static String joinLines(String paragraph) {
        return Arrays.stream(paragraph.split(LINE_BREAK))
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .reduce((text, line) -> text + (LIST_ITEM.matcher(line).matches() ? "\n" : " ") + line)
                .orElse("");
    }

    @Override
    public List<String> getArgumentNames() {
        return null;
    }
}
