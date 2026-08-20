package org.tabletest.reporter.pebble;

import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Pebble filter ({@code text | paragraphs}) splitting text into its paragraphs. A blank line
 * separates one paragraph from the next, and the line breaks inside a paragraph are dropped.
 * <p>
 * Markdown and AsciiDoc read a blank line as a paragraph break themselves. HTML collapses it,
 * along with every other line break, so a template renders each paragraph as its own element and
 * lets it flow to the width of the page.
 */
public class FilterParagraphs implements Filter {

    public static final String NAME = "paragraphs";

    private static final String BLANK_LINE = "\\R[ \\t]*\\R";
    private static final String LINE_BREAK = "\\R[ \\t]*";

    @Override
    public Object apply(
            Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
        if (input == null) {
            return List.of();
        }
        return Arrays.stream(input.toString().strip().split(BLANK_LINE))
                .map(paragraph -> paragraph.replaceAll(LINE_BREAK, " ").strip())
                .filter(paragraph -> !paragraph.isEmpty())
                .toList();
    }

    @Override
    public List<String> getArgumentNames() {
        return null;
    }
}
