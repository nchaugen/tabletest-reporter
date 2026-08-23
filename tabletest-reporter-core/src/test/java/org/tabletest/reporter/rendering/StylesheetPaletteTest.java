package org.tabletest.reporter.rendering;

import org.junit.jupiter.api.Test;
import org.tabletest.reporter.TemplateEngine;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tabletest.reporter.BuiltInFormat.HTML;

/**
 * Guards the four palettes in the built-in stylesheet against drifting apart. CSS cannot share one
 * set of values between a media query and a plain selector, so the light palette, the two dark ones
 * and the print one each list every token by hand. A token forgotten in one of them is invisible
 * until someone views a report that way — which is how the explicit dark toggle went a release
 * without {@code --shade}, showing the light-mode scroll shadow on a dark page.
 *
 * <p>Unpublished: an internal consistency check on the stylesheet, not a rule about a report.
 */
class StylesheetPaletteTest {

    private static final Pattern DECLARATION = Pattern.compile("(--[a-z-]+):");

    /** The tokens that name a typeface rather than a colour, and so belong to no palette. */
    private static final Set<String> NOT_A_COLOUR = Set.of("--font-sans", "--font-mono");

    private static final String LIGHT = ":root {";
    private static final String DARK_BY_SCHEME = ":root:not([data-theme=\"light\"]) {";
    private static final String DARK_BY_CHOICE = ":root[data-theme=\"dark\"] {";
    private static final String PRINT = ":root[data-theme=\"light\"] {";

    @Test
    void bothDarkPalettesCarryEveryColourTheLightOneDoes() {
        Map<String, Set<String>> palettes = palettes();

        assertThat(palettes.get(DARK_BY_SCHEME)).isEqualTo(palettes.get(LIGHT));
        assertThat(palettes.get(DARK_BY_CHOICE)).isEqualTo(palettes.get(LIGHT));
    }

    @Test
    void thePrintPaletteReplacesEveryColourTheScreenPalettesSet() {
        Map<String, Set<String>> palettes = palettes();

        assertThat(palettes.get(PRINT)).isEqualTo(palettes.get(LIGHT));
    }

    @Test
    void noPaletteDeclaresTheSameTokenTwice() {
        String stylesheet = stylesheet();

        palettes().keySet().forEach(selector -> {
            String block = blockAfter(stylesheet, selector);
            long declared = DECLARATION
                    .matcher(block)
                    .results()
                    .map(result -> result.group(1))
                    .filter(token -> !NOT_A_COLOUR.contains(token))
                    .count();
            assertThat(declared)
                    .as("duplicate token declaration in %s", selector)
                    .isEqualTo(tokensIn(block).size());
        });
    }

    /** The colour tokens each palette block declares, keyed by the selector that opens it. */
    private static Map<String, Set<String>> palettes() {
        String stylesheet = stylesheet();
        Map<String, Set<String>> palettes = new HashMap<>();
        for (String selector : Set.of(LIGHT, DARK_BY_SCHEME, DARK_BY_CHOICE, PRINT)) {
            palettes.put(selector, tokensIn(blockAfter(stylesheet, selector)));
        }
        return palettes;
    }

    private static Set<String> tokensIn(String block) {
        Set<String> tokens = new LinkedHashSet<>();
        Matcher matcher = DECLARATION.matcher(block);
        while (matcher.find()) {
            if (!NOT_A_COLOUR.contains(matcher.group(1))) {
                tokens.add(matcher.group(1));
            }
        }
        return tokens;
    }

    /** The declarations between a selector's opening brace and its closing one. */
    private static String blockAfter(String stylesheet, String selector) {
        int opening = stylesheet.indexOf(selector);
        assertThat(opening).as("stylesheet has no %s block", selector).isNotNegative();
        int start = opening + selector.length();
        return stylesheet.substring(start, stylesheet.indexOf('}', start));
    }

    private static String stylesheet() {
        Map<String, Object> context = new HashMap<>(Map.of("title", "Specification"));
        String page = new TemplateEngine().renderIndex(HTML, context);
        int start = page.indexOf("<style>");
        return page.substring(start, page.indexOf("</style>", start));
    }
}
