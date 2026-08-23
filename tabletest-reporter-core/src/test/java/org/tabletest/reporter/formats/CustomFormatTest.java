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
        A project can generate a report in a format of its own, beside the three built-in ones.
        A pair of templates named after the format is the whole definition.

        The name identifies the format, and every file of the report takes that name as its
        extension. The reporter therefore refuses a name that cannot be an extension when the
        project declares the format, and not when it writes the first file.
        """)
class CustomFormatTest {

    @DisplayName("Turns a custom format's name into the extension its files get")
    @Description("""
            The name arrives without the dot, and the extension is the name with one. A format named
            html therefore writes .html files. A name that a built-in format already uses is a name
            like any other here. A separate rule says which templates that name finds.
            """)
    @TableTest("""
        Scenario                    | Custom format | File extension?
        A name of its own           | report        | .report
        A name a built-in also uses | html          | .html
        """)
    void isNamedByTheExtensionItsFilesAreGiven(CustomFormat format, String fileExtension) {
        assertThat(format.extension()).isEqualTo(fileExtension);
    }

    @DisplayName("Refuses a format name that could not be an extension")
    @Description("""
            Three names cannot become an extension: an absent name, a blank name, and a name that
            already starts with a dot. The reporter refuses each one as the project declares the
            format, and the message names what is wrong. The reporter writes no report under a name
            that cannot name its files.
            """)
    @TableTest("""
        Scenario                      | Format name | Error message?
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
