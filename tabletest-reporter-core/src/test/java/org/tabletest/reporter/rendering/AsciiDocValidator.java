package org.tabletest.reporter.rendering;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;

/**
 * Validates AsciiDoc syntax by attempting to parse it.
 * <p>
 * Uses AsciidoctorJ parser to verify that generated AsciiDoc is syntactically valid.
 * Invalid AsciiDoc will cause parsing exceptions.
 */
class AsciiDocValidator {

    private static final Asciidoctor ASCIIDOCTOR = Asciidoctor.Factory.create();

    /**
     * Validates that the given AsciiDoc content can be successfully parsed.
     *
     * @param asciidoc the AsciiDoc content to validate
     * @throws RuntimeException if the AsciiDoc is invalid
     */
    static void assertValidAsciiDoc(String asciidoc) {
        try {
            ASCIIDOCTOR.load(asciidoc, Options.builder().build());
        } catch (Exception e) {
            throw new AssertionError("Invalid AsciiDoc syntax: " + e.getMessage(), e);
        }
    }
}
