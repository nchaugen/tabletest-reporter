package io.github.nchaugen.tabletest.reporter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.snakeyaml.engine.v2.exceptions.ScannerException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests error handling behaviour of ContextLoader for malformed YAML and IO errors.
 */
class ContextLoaderErrorTest {

    private final ContextLoader contextLoader = new ContextLoader();

    @Test
    void fromYaml_throws_UncheckedIOException_when_file_does_not_exist(@TempDir Path tempDir) {
        Path nonExistent = tempDir.resolve("does-not-exist.yaml");

        assertThatThrownBy(() -> contextLoader.fromYaml(nonExistent))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("Failed to read YAML from")
                .hasMessageContaining(nonExistent.toString());
    }

    @Test
    void fromYaml_throws_ScannerException_for_malformed_yaml(@TempDir Path tempDir) throws IOException {
        Path malformedYaml = tempDir.resolve("malformed.yaml");
        Files.writeString(malformedYaml, """
                title: "Unclosed quote
                headers:
                  - invalid: yaml: syntax
                """);

        assertThatThrownBy(() -> contextLoader.fromYaml(malformedYaml)).isInstanceOf(ScannerException.class);
    }

    @Test
    void fromYaml_throws_ScannerException_for_invalid_indentation(@TempDir Path tempDir) throws IOException {
        Path badIndentation = tempDir.resolve("bad-indent.yaml");
        Files.writeString(badIndentation, """
                title: Test
                  headers:
                - value: Wrong
                """);

        assertThatThrownBy(() -> contextLoader.fromYaml(badIndentation)).isInstanceOf(ScannerException.class);
    }

    @Test
    void fromYaml_string_throws_ScannerException_for_malformed_yaml() {
        String malformedYaml = "key: \"unclosed";

        assertThatThrownBy(() -> contextLoader.fromYaml(malformedYaml)).isInstanceOf(ScannerException.class);
    }
}
