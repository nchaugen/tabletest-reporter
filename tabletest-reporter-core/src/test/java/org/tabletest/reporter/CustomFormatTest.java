package org.tabletest.reporter;

import org.junit.jupiter.api.DisplayName;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CustomFormat}.
 */
@DisplayName("Custom formats")
@Description("""
        Beyond the three built-in formats, a report can be generated in a format of the project's
        own, defined by nothing more than a pair of templates named after it. The name is the whole
        definition: it identifies the format and it is the extension every file of the report gets,
        so a name that could not be an extension is refused when the format is declared rather than
        when the first file is written.
        """)
class CustomFormatTest {

    @DisplayName("A custom format is named by the extension its files are given")
    @Description("""
            The name arrives without the dot and the extension is the name with one, so a format
            named html writes .html files. A name that is absent, blank, or already starts with a
            dot cannot be turned into an extension, and each is refused with a message naming what
            is wrong.
            """)
    @TableTest("""
        Scenario                      | Format name | File extension? | Error message?
        A name of its own             | report      | .report         |
        A name a built-in also uses   | html        | .html           |
        No name at all                |             |                 | name
        A blank name                  | ''          |                 | Format name cannot be blank
        A name that starts with a dot | .html       |                 | Format name cannot start with a dot: .html
        """)
    void isNamedByTheExtensionItsFilesAreGiven(String formatName, String fileExtension, String errorMessage) {
        assertThat(extensionOf(formatName)).isEqualTo(fileExtension);
        assertThat(errorFromNaming(formatName)).isEqualTo(errorMessage);
    }

    /** The extension a format of this name gives its files, or null where the name is refused. */
    private static String extensionOf(String formatName) {
        try {
            return new CustomFormat(formatName).extension();
        } catch (RuntimeException refused) {
            return null;
        }
    }

    /** The message the name is refused with, or null where it is accepted. */
    private static String errorFromNaming(String formatName) {
        try {
            new CustomFormat(formatName);
            return null;
        } catch (RuntimeException refused) {
            return refused.getMessage();
        }
    }
}
