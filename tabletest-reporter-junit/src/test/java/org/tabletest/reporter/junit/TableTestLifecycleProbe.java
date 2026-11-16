package org.tabletest.reporter.junit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.platform.engine.OutputDirectoryCreator;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.tabletest.junit.TableTest;
import org.tabletest.junit.TypeConverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Checks publication when an arguments source fails after submitting an invocation.
 * Invoked explicitly with -Dtest=TableTestLifecycleProbe: JUnit 6.1.3 has a known
 * lifecycle failure, while JUnit 6.2.0-M1 passes. The Probe suffix keeps this check
 * outside the ordinary test suite without disabling its assertions.
 */
@Execution(ExecutionMode.SAME_THREAD)
@Timeout(30)
class TableTestLifecycleProbe {

    @TempDir
    Path output;

    @Test
    void waitsForSubmittedRowsWhenTheArgumentsSourceFails() throws Exception {
        FailingSource.state = new SourceFailure();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<EngineExecutionResults> execution = executor.submit(() -> execute(FailingSource.class));
        boolean publishedBeforeCompletion;
        EngineExecutionResults results;
        try {
            await(FailingSource.state.started, "The submitted invocation did not start");
            await(FailingSource.state.failed, "The arguments source did not fail");
            // A bounded absence check while the submitted invocation is held at a latch.
            publishedBeforeCompletion = FailingSource.state.published.await(200, TimeUnit.MILLISECONDS);
        } finally {
            FailingSource.state.release.countDown();
            try {
                results = execution.get(10, TimeUnit.SECONDS);
                await(FailingSource.state.completed, "The submitted invocation did not finish its watcher");
            } finally {
                executor.shutdownNow();
            }
        }

        assertThat(publishedBeforeCompletion)
                .as("Publishing must wait for the invocation submitted before the source failed")
                .isFalse();
        results.testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
        results.containerEvents().assertStatistics(stats -> stats.failed(1));
        assertResults(tableDocument("partiallyProvidedValues"), tuple(1, "[1] Submitted", true));
        assertThat(entries(classDocument(), "tableTests"))
                .extracting(entry -> entry.get("methodName"))
                .containsExactly("partiallyProvidedValues");
    }

    private EngineExecutionResults execute(Class<?> fixture) {
        return EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(fixture))
                .enableImplicitConfigurationParameters(false)
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "true")
                .configurationParameter("junit.jupiter.execution.parallel.config.strategy", "fixed")
                .configurationParameter("junit.jupiter.execution.parallel.config.fixed.parallelism", "4")
                .configurationParameter("junit.jupiter.execution.parallel.config.fixed.max-pool-size", "16")
                .outputDirectoryCreator(new OutputDirectoryCreator() {
                    @Override
                    public Path getRootDirectory() {
                        return output;
                    }

                    @Override
                    public Path createOutputDirectory(TestDescriptor descriptor) {
                        return output;
                    }
                })
                .execute();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> documents() throws IOException {
        List<Map<String, Object>> documents = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(output)) {
            for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(".yaml"))
                    .toList()) {
                Load loader = new Load(LoadSettings.builder().build());
                documents.add((Map<String, Object>) loader.loadFromString(Files.readString(path)));
            }
        }
        return documents;
    }

    private Map<String, Object> tableDocument(String method) throws IOException {
        List<Map<String, Object>> tables = documents().stream()
                .filter(document -> method.equals(document.get("methodName")))
                .toList();
        assertThat(tables).as("Published table for %s", method).hasSize(1);
        return tables.get(0);
    }

    private Map<String, Object> classDocument() throws IOException {
        List<Map<String, Object>> classes = documents().stream()
                .filter(document -> document.containsKey("className"))
                .toList();
        assertThat(classes).as("Published test class").hasSize(1);
        return classes.get(0);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> entries(Map<String, Object> document, String key) {
        return (List<Map<String, Object>>) document.get(key);
    }

    private static void assertResults(Map<String, Object> table, org.assertj.core.groups.Tuple... expected) {
        assertThat(entries(table, "rowResults"))
                .extracting(
                        result -> result.get("rowIndex"),
                        result -> result.get("displayName"),
                        result -> result.get("passed"))
                .containsExactlyInAnyOrder(expected);
    }

    private static void await(CountDownLatch latch, String message) throws InterruptedException {
        assertTrue(latch.await(10, TimeUnit.SECONDS), message);
    }

    /** Holds one submitted invocation open while the next table cell fails conversion. */
    static final class SourceFailure {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch failed = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final CountDownLatch published = new CountDownLatch(1);
        private final CountDownLatch completed = new CountDownLatch(1);
    }

    /** A converted cell whose second spelling fails inside the real arguments stream. */
    public record WorkItem(String name) {}

    /** Submits one invocation and then fails while converting the following row. */
    @Execution(ExecutionMode.CONCURRENT)
    @ExtendWith(SourceFailurePublisher.class)
    public static class FailingSource {
        static SourceFailure state;

        @TableTest("""
            Scenario  | Work
            Submitted | ready
            Rejected  | broken
            """)
        void partiallyProvidedValues(WorkItem work) throws InterruptedException {
            state.started.countDown();
            await(state.release, "The submitted invocation was not released");
            assertEquals("ready", work.name());
        }

        @TypeConverter
        public static WorkItem parseWorkItem(String value) throws InterruptedException {
            if (value.equals("broken")) {
                await(state.started, "The first invocation must start before the source fails");
                state.failed.countDown();
                throw new IllegalArgumentException("The arguments source failed");
            }
            return new WorkItem(value);
        }
    }

    /** Observes class publication without changing the production callback order. */
    public static class SourceFailurePublisher extends TableTestPublisher {
        @Override
        public void testSuccessful(ExtensionContext context) {
            try {
                super.testSuccessful(context);
            } finally {
                FailingSource.state.completed.countDown();
            }
        }

        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            try {
                super.testFailed(context, cause);
            } finally {
                FailingSource.state.completed.countDown();
            }
        }

        @Override
        public void afterAll(ExtensionContext context) {
            try {
                super.afterAll(context);
            } finally {
                FailingSource.state.published.countDown();
            }
        }
    }
}
