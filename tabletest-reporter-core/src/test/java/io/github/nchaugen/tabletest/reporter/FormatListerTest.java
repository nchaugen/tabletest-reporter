package io.github.nchaugen.tabletest.reporter;

import io.github.nchaugen.tabletest.junit.Scenario;
import io.github.nchaugen.tabletest.junit.TableTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FormatLister}.
 */
class FormatListerTest {

    @TableTest("""
        Scenario                      | Template Files                                                             | Expected Output
        Empty template directory      | []                                                                         | [asciidoc, markdown]
        Custom HTML and XML formats   | [table.html.peb, index.html.peb, table.xml.peb, index.xml.peb]             | [asciidoc, html, markdown, xml]
        Formats sorted alphabetically | [table.zebra.peb, index.zebra.peb, table.aardvark.peb, index.aardvark.peb] | [aardvark, asciidoc, markdown, zebra]
        Single custom format          | [table.custom.peb, index.custom.peb]                                       | [asciidoc, custom, markdown]
        """)
    void lists_formats(@Scenario String scenario, List<String> templateFiles, List<String> expectedOutput,
                      @TempDir Path tempDir) throws IOException {
        for (String file : templateFiles) {
            Path filePath = tempDir.resolve(file);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, "template content");
        }

        String result = FormatLister.listFormats(tempDir);

        assertThat(result).isEqualTo(String.join(System.lineSeparator(), expectedOutput));
    }

    @Test
    void lists_built_in_formats_when_template_directory_is_null() {
        String result = FormatLister.listFormats(null);

        assertThat(result).isEqualTo("asciidoc" + System.lineSeparator() + "markdown");
    }

    @Test
    void handles_nonexistent_template_directory() {
        Path nonexistent = Path.of("/nonexistent/directory");

        String result = FormatLister.listFormats(nonexistent);

        assertThat(result).isEqualTo("asciidoc" + System.lineSeparator() + "markdown");
    }
}
