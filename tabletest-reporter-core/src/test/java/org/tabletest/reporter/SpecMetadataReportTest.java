package org.tabletest.reporter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tabletest.reporter.BuiltInFormat.MARKDOWN;

// Unpublished: end-to-end wiring of spec metadata into the report, not a user-facing rule.
class SpecMetadataReportTest {

    @TempDir
    Path tempDir;

    @Test
    void appliesTitleIntroAndFeatureOrderToRootIndex() throws IOException {
        Path inDir = setupTwoClassInput();
        Path outDir = Files.createDirectory(tempDir.resolve("out"));
        SpecMetadata metadata = new SpecMetadata(
                "Example Spec",
                "How the example behaves.",
                List.of(new FeatureMetadata("order-test", "Ordered Placement", null, List.of())));

        new TableTestReporter().report(MARKDOWN, inDir, outDir, false, metadata);

        String rootIndex = Files.readString(outDir.resolve("index.md"));
        assertThat(rootIndex)
                .startsWith("# Example Spec")
                .contains("How the example behaves.")
                .doesNotContain("# example");
        // Declared feature (retitled) leads; the undeclared sibling follows alphabetically.
        assertThat(rootIndex).containsSubsequence("Ordered Placement", "Auth Test");
    }

    @Test
    void withoutMetadataRootIndexKeepsThePackageName() throws IOException {
        Path inDir = setupTwoClassInput();
        Path outDir = Files.createDirectory(tempDir.resolve("out-plain"));

        new TableTestReporter().report(MARKDOWN, inDir, outDir);

        assertThat(Files.readString(outDir.resolve("index.md"))).startsWith("# example");
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
