package org.tabletest.reporter.rendering;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.reporter.BuiltInFormat;
import org.tabletest.reporter.IndexDepth;
import org.tabletest.reporter.TableTestReporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for configurable index depth rendering.
 */
class IndexDepthRenderingTest {

    @TempDir
    Path tempDir;

    @Test
    void depth_1_renders_immediate_children_only_asciidoc() throws IOException {
        Path inDir = setupNestedTestInput();
        Path outDir = Files.createDirectory(tempDir.resolve("out"));

        new TableTestReporter(null, IndexDepth.of(1)).report(BuiltInFormat.ASCIIDOC, inDir, outDir);

        List<String> lines = Files.readAllLines(outDir.resolve("index.adoc"));
        assertThat(lines).contains("* xref:./level1/index.adoc[++Level 1++]").doesNotContain("** xref:");
    }

    @Test
    void depth_2_renders_children_and_grandchildren_asciidoc() throws IOException {
        Path inDir = setupNestedTestInput();
        Path outDir = Files.createDirectory(tempDir.resolve("out"));

        new TableTestReporter(null, IndexDepth.of(2)).report(BuiltInFormat.ASCIIDOC, inDir, outDir);

        List<String> lines = Files.readAllLines(outDir.resolve("index.adoc"));
        assertThat(lines)
                .contains("* xref:./level1/index.adoc[++Level 1++]")
                .contains("** xref:./level1/level2.adoc[++Level 2++]")
                .doesNotContain("*** xref:");
    }

    @Test
    void infinite_depth_renders_all_levels_asciidoc() throws IOException {
        Path inDir = setupNestedTestInput();
        Path outDir = Files.createDirectory(tempDir.resolve("out"));

        new TableTestReporter(null, IndexDepth.INFINITE).report(BuiltInFormat.ASCIIDOC, inDir, outDir);

        List<String> lines = Files.readAllLines(outDir.resolve("index.adoc"));
        assertThat(lines)
                .contains("* xref:./level1/index.adoc[++Level 1++]")
                .contains("** xref:./level1/level2.adoc[++Level 2++]");
    }

    @Test
    void depth_1_renders_immediate_children_only_markdown() throws IOException {
        Path inDir = setupNestedTestInput();
        Path outDir = Files.createDirectory(tempDir.resolve("out"));

        new TableTestReporter(null, IndexDepth.of(1)).report(BuiltInFormat.MARKDOWN, inDir, outDir);

        List<String> lines = Files.readAllLines(outDir.resolve("index.md"));
        assertThat(lines).contains("* [Level 1](./level1/index.md)").noneMatch(line -> line.startsWith("  *"));
    }

    @Test
    void depth_2_renders_children_and_grandchildren_markdown() throws IOException {
        Path inDir = setupNestedTestInput();
        Path outDir = Files.createDirectory(tempDir.resolve("out"));

        new TableTestReporter(null, IndexDepth.of(2)).report(BuiltInFormat.MARKDOWN, inDir, outDir);

        List<String> lines = Files.readAllLines(outDir.resolve("index.md"));
        assertThat(lines)
                .contains("* [Level 1](./level1/index.md)")
                .contains("  * [Level 2](./level1/level2.md)")
                .noneMatch(line -> line.startsWith("    *"));
    }

    @Test
    void infinite_depth_renders_all_levels_markdown() throws IOException {
        Path inDir = setupNestedTestInput();
        Path outDir = Files.createDirectory(tempDir.resolve("out"));

        new TableTestReporter(null, IndexDepth.INFINITE).report(BuiltInFormat.MARKDOWN, inDir, outDir);

        List<String> lines = Files.readAllLines(outDir.resolve("index.md"));
        assertThat(lines).contains("* [Level 1](./level1/index.md)").contains("  * [Level 2](./level1/level2.md)");
    }

    private Path setupNestedTestInput() throws IOException {
        Path inDir = Files.createDirectory(tempDir.resolve("in"));

        Path level1Dir = Files.createDirectory(inDir.resolve("org.example.Level1"));
        Files.writeString(level1Dir.resolve("TABLETEST-level1.yaml"), """
                "className": "org.example.Level1"
                "slug": "level1"
                "title": "Level 1"
                "tableTests":
                  - "path": "nested()/TABLETEST-level2.yaml"
                    "methodName": "nested"
                    "slug": "level2"
                """);

        Path level2Dir = Files.createDirectory(level1Dir.resolve("nested()"));
        Files.writeString(level2Dir.resolve("TABLETEST-level2.yaml"), """
                "className": "org.example.Level2"
                "slug": "level2"
                "title": "Level 2"
                "tableTests":
                  - "path": "deep()/TABLETEST-deep-table.yaml"
                    "methodName": "deep"
                    "slug": "deep-table"
                """);

        Path deepDir = Files.createDirectory(level2Dir.resolve("deep()"));
        Files.writeString(deepDir.resolve("TABLETEST-deep-table.yaml"), """
                "title": "Deep Table"
                "headers":
                  - "value": "Column"
                "rows":
                  - - "value": "data"
                """);

        return inDir;
    }
}
