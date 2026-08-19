package org.tabletest.reporter;

import org.junit.platform.engine.OutputDirectoryCreator;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Runs a sample test class through the JUnit extension and returns the output it published, so a
 * rule about what a real run records can state its input as a test class a reader could write.
 *
 * <p>{@code PublishedRun} writes that output directly, which is enough for rules about report
 * structure but not for anything the extension *decides* during a run — column roles come from
 * the package-private {@code JunitColumnRoleExtractor} and need a live extension context, and row
 * verdicts come from the rows actually passing or failing. Supplying either by hand would put
 * back invented input. The cost is that a rule reads off one sample class rather than one row per
 * sample, so its rows are the columns or rows of that class's table.
 *
 * <p>Public because the rules built on it live in more than one test package.
 */
public final class SampleRun {

    private SampleRun() {}

    /** The output a run of the given sample class published, in a fresh directory. */
    public static Path outputFor(Class<?> sampleClass, Path workingDir) {
        Path runDir = createTempDirectory(workingDir);
        EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(sampleClass))
                .enableImplicitConfigurationParameters(true)
                .outputDirectoryCreator(into(runDir))
                .execute();
        return runDir;
    }

    /** Every file the run publishes lands in the one directory; the report reads class names, not paths. */
    private static OutputDirectoryCreator into(Path runDir) {
        return new OutputDirectoryCreator() {
            @Override
            public Path getRootDirectory() {
                return runDir;
            }

            @Override
            public Path createOutputDirectory(TestDescriptor testDescriptor) {
                return runDir;
            }
        };
    }

    private static Path createTempDirectory(Path workingDir) {
        try {
            return Files.createTempDirectory(workingDir, "run");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
