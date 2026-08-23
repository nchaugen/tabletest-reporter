package org.tabletest.reporter.formats;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.junit.Description;
import org.tabletest.junit.Scenario;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.FormatLister;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FormatLister}.
 */
@DisplayName("Available output formats")
class FormatListerTest {

    @DisplayName("Lists the built-in formats plus any of your own, sorted")
    @Description("""
            Three formats are always available: asciidoc, html and markdown. The reporter finds any
            custom format in the template directory and adds it to those three. It then sorts the
            whole list alphabetically.
            """)
    @TableTest("""
        Scenario                      | Template Files                                                             | Available Formats?
        Empty template directory      | []                                                                         | [asciidoc, html, markdown]
        Custom XML format             | [table.xml.peb, index.xml.peb]                                             | [asciidoc, html, markdown, xml]
        Formats sorted alphabetically | [table.zebra.peb, index.zebra.peb, table.aardvark.peb, index.aardvark.peb] | [aardvark, asciidoc, html, markdown, zebra]
        Single custom format          | [table.custom.peb, index.custom.peb]                                       | [asciidoc, custom, html, markdown]
        """)
    void lists_formats(
            @Scenario String scenario, List<String> templateFiles, List<String> availableFormats, @TempDir Path tempDir)
            throws IOException {
        for (String file : templateFiles) {
            Path filePath = tempDir.resolve(file);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, "template content");
        }

        String result = FormatLister.listFormats(tempDir);

        assertThat(result).isEqualTo(String.join(System.lineSeparator(), availableFormats));
    }

    @Test
    void lists_built_in_formats_when_template_directory_is_null() {
        String result = FormatLister.listFormats(null);

        assertThat(result).isEqualTo(String.join(System.lineSeparator(), "asciidoc", "html", "markdown"));
    }

    @Test
    void handles_nonexistent_template_directory() {
        Path nonexistent = Path.of("/nonexistent/directory");

        String result = FormatLister.listFormats(nonexistent);

        assertThat(result).isEqualTo(String.join(System.lineSeparator(), "asciidoc", "html", "markdown"));
    }
}
