package org.tabletest.reporter;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.OutputDirectoryCreator;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.junit.TableTestPublisher;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Pins {@link ReportStructure} against production: the pages the helper builds for a named test
 * class must be the pages the reporter builds from the YAML the JUnit extension writes when that
 * same class really runs. Without it the helper could drift from the extension — in how a page is
 * named, where its file lands, or what the class file says about its tables — and every published
 * structure rule would document the helper rather than the reporter.
 */
// Unpublished: a fidelity check on a test helper, not a rule about the reporter.
class ReportStructureFidelityTest {

    /** The same class and tables as {@link StructureSampleTest}, as the helper's input. */
    private static final List<String> SAME_TABLES = List.of(
            StructureSampleTest.class.getName() + "#orderItems", StructureSampleTest.class.getName() + "#orderTotals");

    @TempDir
    Path realRunDir;

    @TempDir
    Path helperDir;

    @Test
    void helperBuildsTheSamePagesARealRunDoes() {
        List<String> fromRealRun = ReportStructure.pagesOf(ReportTree.process(runSampleTest()));

        List<String> fromHelper = ReportStructure.pagesFor(SAME_TABLES, helperDir);

        assertThat(fromHelper).isEqualTo(fromRealRun);
    }

    /** Runs the sample test through the JUnit extension and returns the directory it published to. */
    private Path runSampleTest() {
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(StructureSampleTest.class))
                .enableImplicitConfigurationParameters(true)
                .outputDirectoryCreator(intoRealRunDir())
                .execute();
        return realRunDir;
    }

    private @NonNull OutputDirectoryCreator intoRealRunDir() {
        return new OutputDirectoryCreator() {
            @Override
            public Path getRootDirectory() {
                return realRunDir;
            }

            @Override
            public Path createOutputDirectory(TestDescriptor testDescriptor) {
                return realRunDir;
            }
        };
    }

    /**
     * Run only by the fidelity test above, through EngineTestKit — a static nested class is not
     * picked up by the surrounding test run, so it publishes no page of its own.
     */
    @ExtendWith(TableTestPublisher.class)
    static class StructureSampleTest {

        @TableTest("""
            Item  | Quantity
            Chair | 2
            """)
        void orderItems(String item, int quantity) {
            assertThat(item).isNotBlank();
            assertThat(quantity).isPositive();
        }

        @TableTest("""
            Order | Total
            A-1   | 99
            """)
        void orderTotals(String order, int total) {
            assertThat(order).isNotBlank();
            assertThat(total).isPositive();
        }
    }
}
