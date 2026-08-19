package org.tabletest.reporter.formats;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.DisplayName;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.CustomFormat;

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

    @DisplayName("Turns a custom format's name into the extension its files get")
    @Description("""
            The name arrives without the dot and the extension is the name with one, so a format
            named html writes .html files. A name a built-in format already uses is a name like
            any other here; which templates it finds is a separate rule.
            """)
    @TableTest("""
        Scenario                    | Custom format | File extension
        A name of its own           | report        | .report
        A name a built-in also uses | html          | .html
        """)
    void isNamedByTheExtensionItsFilesAreGiven(CustomFormat format, String fileExtension) {
        assertThat(format.extension()).isEqualTo(fileExtension);
    }

    @DisplayName("Refuses a format name that could not be an extension")
    @Description("""
            A name that is absent, blank, or already starts with a dot cannot be turned into an
            extension. Each is refused as the format is declared, with a message naming what is
            wrong, so no report is ever written under a name that could not name its files.
            """)
    @TableTest("""
        Scenario                      | Format name | Error message
        No name at all                |             | Format name cannot be missing
        A blank name                  | ''          | Format name cannot be blank
        A name that starts with a dot | .html       | Format name cannot start with a dot: .html
        """)
    void refusesANameThatCouldNotBeAnExtension(String formatName, String errorMessage) {
        assertThat(errorMessageFrom(() -> new CustomFormat(formatName))).isEqualTo(errorMessage);
    }

    /** The message the action fails with, or null when it does not fail. */
    private static String errorMessageFrom(ThrowableAssert.ThrowingCallable action) {
        try {
            action.call();
            return null;
        } catch (Throwable thrown) {
            return thrown.getMessage();
        }
    }
}
