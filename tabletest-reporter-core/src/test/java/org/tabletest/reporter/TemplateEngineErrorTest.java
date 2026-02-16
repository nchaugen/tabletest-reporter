package org.tabletest.reporter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests error handling behaviour of TemplateEngine for missing or invalid templates.
 */
class TemplateEngineErrorTest {

    @Test
    void renderTable_throws_IllegalArgumentException_for_unknown_custom_format(@TempDir Path tempDir)
            throws IOException {
        Files.writeString(tempDir.resolve("table.html.peb"), "template");
        Files.writeString(tempDir.resolve("index.html.peb"), "template");
        TemplateEngine engine = new TemplateEngine(tempDir);
        Format unknownFormat = new CustomFormat("nonexistent");

        assertThatThrownBy(() -> engine.renderTable(unknownFormat, java.util.Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Custom template not found");
    }

    @Test
    void renderIndex_throws_IllegalArgumentException_for_unknown_custom_format(@TempDir Path tempDir)
            throws IOException {
        Files.writeString(tempDir.resolve("table.html.peb"), "template");
        Files.writeString(tempDir.resolve("index.html.peb"), "template");
        TemplateEngine engine = new TemplateEngine(tempDir);
        Format unknownFormat = new CustomFormat("nonexistent");

        assertThatThrownBy(() -> engine.renderIndex(unknownFormat, java.util.Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Custom template not found");
    }

    @Test
    void constructor_throws_exception_for_null_template_directory() {
        assertThatThrownBy(() -> new TemplateEngine(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("customTemplateDirectory");
    }
}
