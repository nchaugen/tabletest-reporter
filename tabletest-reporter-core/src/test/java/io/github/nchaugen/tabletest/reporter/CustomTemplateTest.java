package io.github.nchaugen.tabletest.reporter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomTemplateTest {

    @TempDir
    Path tempDir;

    @Test
    void custom_template_overrides_builtin() throws IOException {
        // Create custom template directory
        Path customTemplateDir = tempDir.resolve("templates");
        Files.createDirectories(customTemplateDir);

        // Create custom table template that completely replaces built-in
        String customTemplate = """
            = CUSTOM HEADER

            == {{ title }}

            Custom table content here

            = CUSTOM FOOTER
            """;
        Files.writeString(customTemplateDir.resolve("table.adoc.peb"), customTemplate);

        // Create template engine with custom directory
        TemplateEngine engine = new TemplateEngine(customTemplateDir);

        // Render with custom template
        Map<String, Object> context = Map.of(
            "title", "Test Table",
            "headers", java.util.List.of(Map.of("value", "a")),
            "rows", java.util.List.of()
        );

        String rendered = engine.renderTable(BuiltInFormat.ASCIIDOC, context);

        // Verify custom template is used
        assertThat(rendered).contains("CUSTOM HEADER");
        assertThat(rendered).contains("CUSTOM FOOTER");
        assertThat(rendered).contains("Test Table");
        assertThat(rendered).contains("Custom table content here");
    }

    @Test
    void fallback_to_builtin_when_custom_not_found() {
        // Create empty custom template directory
        Path customTemplateDir = tempDir.resolve("templates");

        // Create template engine with custom directory (no custom templates)
        TemplateEngine engine = new TemplateEngine(customTemplateDir);

        // Render with built-in template
        Map<String, Object> context = Map.of(
            "title", "Test Table",
            "headers", java.util.List.of(Map.of("value", "a")),
            "rows", java.util.List.of()
        );

        String rendered = engine.renderTable(BuiltInFormat.ASCIIDOC, context);

        // Verify built-in template is used
        assertThat(rendered).startsWith("== ++Test Table++");
        assertThat(rendered).contains("[%header,cols=");
    }
}
