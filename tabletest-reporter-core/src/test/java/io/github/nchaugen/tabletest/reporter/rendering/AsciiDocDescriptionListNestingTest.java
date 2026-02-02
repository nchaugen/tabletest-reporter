package io.github.nchaugen.tabletest.reporter.rendering;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduces GitHub issue #11: AsciiDoctor renders sibling description list
 * entries as nested when using 5+ colons as delimiter.
 */
class AsciiDocDescriptionListNestingTest {

    private static final Asciidoctor ASCIIDOCTOR = Asciidoctor.Factory.create();

    @Test
    void sibling_entries_with_two_colons_are_siblings_in_html() {
        String html = convertToHtml("""
                a:: 1
                b:: 2
                c:: 3
                """);

        assertThat(dlistCount(html)).as("2 colons: all siblings in one dlist").isEqualTo(1);
    }

    @Test
    void sibling_entries_with_three_colons_are_siblings_in_html() {
        String html = convertToHtml("""
                a::: 1
                b::: 2
                c::: 3
                """);

        assertThat(dlistCount(html)).as("3 colons: all siblings in one dlist").isEqualTo(1);
    }

    @Test
    void sibling_entries_with_four_colons_are_siblings_in_html() {
        String html = convertToHtml("""
                a:::: 1
                b:::: 2
                c:::: 3
                """);

        assertThat(dlistCount(html)).as("4 colons: all siblings in one dlist").isEqualTo(1);
    }

    @Test
    void sibling_entries_with_five_colons_are_nested_in_html() {
        String html = convertToHtml("""
                a::::: 1
                b::::: 2
                c::::: 3
                """);

        assertThat(dlistCount(html))
                .as("5 colons: should be 1 dlist but AsciiDoctor nests them — issue #11")
                .isGreaterThan(1);
    }

    private String convertToHtml(String asciidoc) {
        return ASCIIDOCTOR.convert(asciidoc, Options.builder().build());
    }

    private long dlistCount(String html) {
        return (html.length() - html.replace("<div class=\"dlist\">", "").length()) / "<div class=\"dlist\">".length();
    }
}
