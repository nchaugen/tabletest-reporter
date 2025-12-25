package io.github.nchaugen.tabletest.reporter;

import io.github.nchaugen.tabletest.junit.Scenario;
import io.github.nchaugen.tabletest.junit.TableTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FormatDiscovery}.
 */
class FormatDiscoveryTest {

    @TableTest("""
        Scenario                   | Template Files                                                                                 | Discovered Formats?
        Single format              | [table.html.peb, index.html.peb]                                                               | [html]
        Multiple formats           | [table.html.peb, index.html.peb, table.xml.peb, index.xml.peb, table.json.peb, index.json.peb] | [html, xml, json]
        Only table template        | [table.html.peb]                                                                               | []
        Only index template        | [index.html.peb]                                                                               | []
        Empty directory            | []                                                                                             | []
        Non-template files present | [table.html.peb, index.html.peb, README.md, config.yml]                                        | [html]
        Subdirectories ignored     | [table.html.peb, index.html.peb, subdir/table.xml.peb, subdir/index.xml.peb]                   | [html]
        """)
    void discovers_formats(
            @Scenario String _scenario,
            List<String> templateFiles,
            List<String> discoveredFormats,
            @TempDir Path tempDir)
            throws IOException {
        for (String file : templateFiles) {
            Path filePath = tempDir.resolve(file);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, "template content");
        }

        Set<String> result = FormatDiscovery.discoverFormats(tempDir);

        assertThat(result).containsExactlyInAnyOrderElementsOf(discoveredFormats);
    }

    @Test
    void returns_empty_set_for_null_directory() {
        Set<String> formats = FormatDiscovery.discoverFormats(null);

        assertThat(formats).isEmpty();
    }

    @Test
    void returns_empty_set_for_nonexistent_directory() {
        Path nonexistent = Path.of("/nonexistent/directory");

        Set<String> formats = FormatDiscovery.discoverFormats(nonexistent);

        assertThat(formats).isEmpty();
    }
}
