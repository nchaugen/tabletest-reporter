package org.tabletest.reporter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tabletest.reporter.BuiltInFormat.MARKDOWN;

// Unpublished: end-to-end wiring of the publish selection into the report, not a user-facing rule.
class PublishSelectionReportTest {

    @TempDir
    Path tempDir;

    @Test
    void anExcludedTableIsNeitherWrittenNorLinked() throws IOException {
        Path inDir = setupTwoClassInput();
        Path outDir = tempDir.resolve("out");

        report(inDir, outDir, new PublishSelection(List.of("order-test"), List.of()));

        assertThat(outDir.resolve("order-test")).doesNotExist();
        assertThat(outDir.resolve("auth-test/login-validation.md")).exists();
        assertThat(Files.readString(outDir.resolve("index.md"))).doesNotContain("Order Test");
    }

    @Test
    void anIncludedTableSurvivesItsExcludedClass() throws IOException {
        Path inDir = setupTwoClassInput();
        Path outDir = tempDir.resolve("out-included");

        report(inDir, outDir, new PublishSelection(List.of("**"), List.of("order-test/place-order")));

        assertThat(outDir.resolve("order-test/place-order.md")).exists();
        assertThat(outDir.resolve("auth-test")).doesNotExist();
    }

    @Test
    void withoutASelectionEveryTablePublishes() throws IOException {
        Path inDir = setupTwoClassInput();
        Path outDir = tempDir.resolve("out-plain");

        report(inDir, outDir, PublishSelection.EMPTY);

        assertThat(outDir.resolve("auth-test/login-validation.md")).exists();
        assertThat(outDir.resolve("order-test/place-order.md")).exists();
    }

    private void report(Path inDir, Path outDir, PublishSelection selection) {
        new TableTestReporter(new ReportConfiguration(
                        MARKDOWN,
                        null,
                        IndexDepth.DEFAULT,
                        false,
                        SpecMetadata.EMPTY,
                        selection,
                        SiteLink.NONE,
                        FrontMatter.NONE))
                .report(inDir, outDir);
    }

    private Path setupTwoClassInput() throws IOException {
        Path inDir = Files.createDirectory(tempDir.resolve("in"));
        writeClass(inDir, "AuthTest", "auth-test", "Auth Test", "login", "login-validation");
        writeClass(inDir, "OrderTest", "order-test", "Order Test", "placeOrder", "place-order");
        return inDir;
    }

    private void writeClass(Path inDir, String simpleName, String slug, String title, String method, String tableSlug)
            throws IOException {
        Path classDir = Files.createDirectory(inDir.resolve("org.example." + simpleName));
        Files.writeString(classDir.resolve("TABLETEST-" + slug + ".yaml"), """
                "className": "org.example.%s"
                "slug": "%s"
                "title": "%s"
                "tableTests":
                  - "path": "%s(String)/TABLETEST-%s.yaml"
                    "methodName": "%s"
                    "slug": "%s"
                """.formatted(
                        simpleName, slug, title, method, tableSlug, method, tableSlug));
        Path tableDir = Files.createDirectory(classDir.resolve(method + "(String)"));
        Files.writeString(tableDir.resolve("TABLETEST-" + tableSlug + ".yaml"), """
                "title": "%s"
                "headers":
                  - "value": "Input"
                  - "value": "Valid?"
                "rows":
                    - - "value": "x"
                      - "value": "true"
                """.formatted(title));
    }
}
