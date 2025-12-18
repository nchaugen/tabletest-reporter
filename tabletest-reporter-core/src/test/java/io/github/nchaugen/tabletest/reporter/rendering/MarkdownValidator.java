package io.github.nchaugen.tabletest.reporter.rendering;

import org.commonmark.parser.Parser;

/**
 * Validates Markdown syntax by attempting to parse it.
 * <p>
 * Uses CommonMark parser to verify that generated Markdown is syntactically valid.
 * Invalid Markdown will cause parsing exceptions.
 */
class MarkdownValidator {

    private static final Parser PARSER = Parser.builder().build();

    /**
     * Validates that the given Markdown content can be successfully parsed.
     *
     * @param markdown the Markdown content to validate
     * @throws RuntimeException if the Markdown is invalid
     */
    static void assertValidMarkdown(String markdown) {
        try {
            PARSER.parse(markdown);
        } catch (Exception e) {
            throw new AssertionError("Invalid Markdown syntax: " + e.getMessage(), e);
        }
    }
}
