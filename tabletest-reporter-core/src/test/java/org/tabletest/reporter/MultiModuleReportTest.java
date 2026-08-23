package org.tabletest.reporter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.tabletest.reporter.BuiltInFormat.MARKDOWN;

// Unpublished: end-to-end wiring of multi-directory input, not a user-facing rule.
class MultiModuleReportTest {

    @TempDir
    Path tempDir;

    @Test
    void modulesMergeIntoOneSpecUnderTheirCommonPackage() throws IOException {
        Path coreOutput = moduleOutput("core", "org.example.core.ParserTest", "parser-test", "Parser Test", "grammar");
        Path junitOutput =
                moduleOutput("junit", "org.example.junit.SlugifyTest", "slugify-test", "Slugify Test", "slugify");
        Path outDir = tempDir.resolve("out");

        ReportResult result = markdownReporter().report(List.of(coreOutput, junitOutput), outDir);

        assertThat(result.filesGenerated()).isPositive();
        assertThat(outDir.resolve("core/parser-test/grammar.md")).exists();
        assertThat(outDir.resolve("junit/slugify-test/slugify.md")).exists();
        assertThat(Files.readString(outDir.resolve("index.md"))).contains("Parser Test", "Slugify Test");
    }

    @Test
    void aClassPublishedByTwoModulesReportsItsMostRecentRun() throws IOException {
        Path stale = moduleOutput("stale", "org.example.SharedTest", "shared-test", "Stale Title", "shared-rule");
        Path fresh = moduleOutput("fresh", "org.example.SharedTest", "shared-test", "Fresh Title", "shared-rule");
        setLastModified(stale, Instant.now().minusSeconds(600));
        Path outDir = tempDir.resolve("out-duplicate");

        markdownReporter().report(List.of(stale, fresh), outDir);

        assertThat(Files.readString(outDir.resolve("shared-test/index.md"))).contains("Fresh Title");
    }

    @Test
    void aDirectoryWithoutOutputContributesNothing() throws IOException {
        Path withOutput = moduleOutput("core", "org.example.core.ParserTest", "parser-test", "Parser Test", "grammar");
        Path empty = Files.createDirectories(tempDir.resolve("empty"));
        Path outDir = tempDir.resolve("out-partial");

        ReportResult result = markdownReporter().report(List.of(withOutput, empty), outDir);

        assertThat(result.filesGenerated()).isPositive();
        assertThat(outDir.resolve("parser-test/grammar.md")).exists();
    }

    @Test
    void noOutputAnywhereNamesEveryDirectoryRead() throws IOException {
        Path first = Files.createDirectories(tempDir.resolve("first"));
        Path second = Files.createDirectories(tempDir.resolve("second"));

        ReportResult result = markdownReporter().report(List.of(first, second), tempDir.resolve("out-none"));

        assertThat(result.filesGenerated()).isZero();
        assertThat(result.message()).contains("first", "second");
    }

    @Test
    void reportingFromNoDirectoryAtAllIsRejected() {
        assertThatThrownBy(() -> markdownReporter().report(List.of(), tempDir.resolve("out-empty")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    // --- helpers ---

    private static TableTestReporter markdownReporter() {
        return new TableTestReporter(new ReportConfiguration(
                MARKDOWN,
                null,
                IndexDepth.DEFAULT,
                false,
                SpecMetadata.EMPTY,
                PublishSelection.EMPTY,
                SiteLink.NONE,
                FrontMatter.NONE));
    }

    /** One module's test output directory, holding a single test class with a single table. */
    private Path moduleOutput(String module, String className, String slug, String title, String tableSlug)
            throws IOException {
        Path inDir = Files.createDirectories(tempDir.resolve(module).resolve("junit-jupiter"));
        Path classDir = Files.createDirectories(inDir.resolve(className));
        Files.writeString(classDir.resolve("TABLETEST-" + slug + ".yaml"), """
                "className": "%s"
                "slug": "%s"
                "title": "%s"
                "tableTests":
                  - "path": "rule(String)/TABLETEST-%s.yaml"
                    "methodName": "rule"
                    "slug": "%s"
                """.formatted(
                        className, slug, title, tableSlug, tableSlug));
        Path tableDir = Files.createDirectories(classDir.resolve("rule(String)"));
        Files.writeString(tableDir.resolve("TABLETEST-" + tableSlug + ".yaml"), """
                "title": "%s"
                "headers":
                  - "value": "Input"
                  - "value": "Valid?"
                "rows":
                    - - "value": "x"
                      - "value": "true"
                """.formatted(title));
        return inDir;
    }

    private void setLastModified(Path directory, Instant when) throws IOException {
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                Files.setLastModifiedTime(path, FileTime.from(when));
            }
        }
    }
}
