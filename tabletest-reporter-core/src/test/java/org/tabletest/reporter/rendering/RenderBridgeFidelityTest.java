package org.tabletest.reporter.rendering;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.OutputDirectoryCreator;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.ContextLoader;
import org.tabletest.reporter.TemplateEngine;
import org.tabletest.reporter.junit.TableTestPublisher;
import org.tabletest.reporter.support.RenderBridge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.tabletest.reporter.BuiltInFormat.MARKDOWN;

/**
 * Pins {@link RenderBridge} against production: the bridge must render a table exactly as the
 * report does when the JUnit extension has written the YAML for that same table during a real
 * run. This is what lets the published cell-rendering rules state their input as table source
 * text and still be believed — without it the bridge could drift from the extension and every
 * rule built on it would document the bridge rather than the reporter.
 */
// Unpublished: a fidelity check on a test helper, not a rule about the reporter.
class RenderBridgeFidelityTest {

    /** The same table as {@link CollectionCellSampleTest}, as source text. */
    private static final String SAME_TABLE = """
            Values          | Collection
            Empty list      | []
            List of numbers | [1, 2, 3]
            Set of numbers  | {1, 2, 3}
            Map of numbers  | [a: 1, b: 2]
            Pipe in a value | ["|", "|"]
            Nested list     | [[1, 2], [a, b]]
            """;

    @TempDir
    Path tempDir;

    private final TemplateEngine templateEngine = new TemplateEngine();

    @Test
    void bridgeRendersATableExactlyAsARealRunDoes() throws IOException {
        String fromRealRun = renderFromYamlWrittenByARealRun();

        String fromBridge =
                templateEngine.renderTable(MARKDOWN, RenderBridge.contextFor(SAME_TABLE, "Collection cell rendering"));

        assertThat(fromBridge).isEqualTo(fromRealRun);
    }

    /** Runs the sample test through the JUnit extension and renders the YAML it writes. */
    private String renderFromYamlWrittenByARealRun() throws IOException {
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(CollectionCellSampleTest.class))
                .enableImplicitConfigurationParameters(true)
                .outputDirectoryCreator(intoTempDir())
                .execute();

        return templateEngine.renderTable(MARKDOWN, new ContextLoader().fromYaml(tableYamlIn(tempDir)));
    }

    private Path tableYamlIn(Path directory) throws IOException {
        try (var paths = Files.walk(directory)) {
            return paths.filter(p -> p.getFileName().toString().equals("TABLETEST-collection-cell-rendering.yaml"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("The extension wrote no YAML for the sample table"));
        }
    }

    private @NonNull OutputDirectoryCreator intoTempDir() {
        return new OutputDirectoryCreator() {
            @Override
            public Path getRootDirectory() {
                return tempDir;
            }

            @Override
            public Path createOutputDirectory(TestDescriptor testDescriptor) {
                return tempDir;
            }
        };
    }

    /**
     * Run only by the fidelity test above, through EngineTestKit — a static nested class is not
     * picked up by the surrounding test run, so it publishes no page of its own.
     */
    @ExtendWith(TableTestPublisher.class)
    static class CollectionCellSampleTest {

        @org.junit.jupiter.api.DisplayName("Collection cell rendering")
        @TableTest("""
            Values          | Collection
            Empty list      | []
            List of numbers | [1, 2, 3]
            Set of numbers  | {1, 2, 3}
            Map of numbers  | [a: 1, b: 2]
            Pipe in a value | ["|", "|"]
            Nested list     | [[1, 2], [a, b]]
            """)
        void rendersEveryCollectionKind(String values, Object collection) {
            assertThat(values).isNotBlank();
            assertThat(collection).isNotNull();
        }
    }
}
