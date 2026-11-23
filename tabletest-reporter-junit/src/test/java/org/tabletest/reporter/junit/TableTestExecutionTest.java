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
import org.tabletest.junit.Scenario;
import org.tabletest.junit.TableTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Checks result publication for concurrent methods and out-of-order invocations.
 * These integration fixtures stay out of the living spec.
 */
@Execution(ExecutionMode.SAME_THREAD)
@Timeout(30)
class TableTestExecutionTest {

    @TempDir
    Path output;

    @Test
    void publishesEveryConcurrentRowAndMethodExactlyOnce() throws IOException {
        ConcurrentTables.started = new CountDownLatch(4);
        ConcurrentTables.executed = new ConcurrentLinkedQueue<>();

        EngineExecutionResults results = execute(ConcurrentTables.class);

        results.testEvents()
                .assertStatistics(stats -> stats.started(4).succeeded(4).failed(0));
        results.containerEvents().assertStatistics(stats -> stats.failed(0));
        assertThat(ConcurrentTables.executed)
                .containsExactlyInAnyOrder("First row", "Second row", "Third row", "Fourth row");
        assertThat(documents()).hasSize(3);
        assertThat(entries(classDocument(), "tableTests"))
                .extracting(entry -> entry.get("methodName"))
                .containsExactlyInAnyOrder("firstTable", "secondTable");
        assertResults(tableDocument("firstTable"), tuple(1, "[1] First row", true), tuple(2, "[2] Second row", true));
        assertResults(tableDocument("secondTable"), tuple(1, "[1] Third row", true), tuple(2, "[2] Fourth row", true));
        assertRowRoles(tableDocument("firstTable"), "passed", "passed");
        assertRowRoles(tableDocument("secondTable"), "passed", "passed");
    }

    @Test
    void attributesExpandedResultsAfterOutOfOrderCompletion() throws IOException {
        ExpandedTable.completion = new CompletionOrder(3);

        EngineExecutionResults results = execute(ExpandedTable.class);

        results.testEvents()
                .assertStatistics(stats -> stats.started(3).succeeded(2).failed(1));
        results.containerEvents().assertStatistics(stats -> stats.failed(0));
        assertThat(ExpandedTable.completion.recorded).containsExactly(3, 2, 1);
        Map<String, Object> table = tableDocument("expandedValues");
        assertResults(
                table,
                tuple(1, "[1] Expanded (Value = 1)", true),
                tuple(2, "[2] Expanded (Value = 2)", false),
                tuple(3, "[3] Control", true));
        assertRowRoles(table, "failed", "passed");
        assertThat(documents()).hasSize(2);
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

    @SuppressWarnings("unchecked")
    private static void assertRowRoles(Map<String, Object> table, String... expected) {
        List<List<Map<String, Object>>> rows = (List<List<Map<String, Object>>>) table.get("rows");
        assertThat(rows).hasSize(expected.length);
        IntStream.range(0, expected.length)
                .forEach(index -> rows.get(index).forEach(cell -> {
                    List<String> roles = (List<String>) cell.get("roles");
                    assertThat(roles.stream().filter(role -> role.equals("passed") || role.equals("failed")))
                            .containsExactly(expected[index]);
                }));
    }

    private static void await(CountDownLatch latch, String message) throws InterruptedException {
        ForkJoinPool.managedBlock(new ForkJoinPool.ManagedBlocker() {
            @Override
            public boolean isReleasable() {
                return latch.getCount() == 0;
            }

            @Override
            public boolean block() throws InterruptedException {
                assertTrue(latch.await(10, TimeUnit.SECONDS), message);
                return true;
            }
        });
    }

    /** Releases both tables only after all four invocations have started. */
    @Execution(ExecutionMode.CONCURRENT)
    @ExtendWith(TableTestPublisher.class)
    public static class ConcurrentTables {
        static CountDownLatch started;
        static ConcurrentLinkedQueue<String> executed;

        @TableTest("""
            Scenario   | Value | Value?
            First row  | 1     | 1
            Second row | 2     | 2
            """)
        void firstTable(@Scenario String scenario, int value, int expected) throws InterruptedException {
            runRow(scenario, value, expected);
        }

        @TableTest("""
            Scenario   | Value | Value?
            Third row  | 3     | 3
            Fourth row | 4     | 4
            """)
        void secondTable(@Scenario String scenario, int value, int expected) throws InterruptedException {
            runRow(scenario, value, expected);
        }

        private static void runRow(String scenario, int value, int expected) throws InterruptedException {
            started.countDown();
            await(started, "Both tables must execute their rows concurrently");
            executed.add(scenario);
            assertEquals(expected, value);
        }
    }

    /** Forces reverse completion after each preceding result reaches the real publisher. */
    static final class CompletionOrder {
        private final List<CountDownLatch> completed;
        private final ConcurrentLinkedQueue<Integer> recorded = new ConcurrentLinkedQueue<>();

        CompletionOrder(int invocations) {
            completed = IntStream.range(0, invocations)
                    .mapToObj(index -> new CountDownLatch(1))
                    .toList();
        }

        void awaitSuccessor(int invocation) throws InterruptedException {
            if (invocation < completed.size()) {
                await(completed.get(invocation), "The later invocation did not finish");
            }
        }

        void recorded(ExtensionContext context) {
            String displayName = context.getDisplayName();
            int invocation = Integer.parseInt(displayName.substring(1, displayName.indexOf(']')));
            recorded.add(invocation);
            completed.get(invocation - 1).countDown();
        }
    }

    /** Exercises both outcomes of one expanded row before a separate passing row. */
    @Execution(ExecutionMode.CONCURRENT)
    @ExtendWith(ReverseCompletionPublisher.class)
    public static class ExpandedTable {
        static CompletionOrder completion;

        @TableTest("""
            Scenario | Value  | Value?
            Expanded | {1, 2} | 1
            Control  | 3      | 3
            """)
        void expandedValues(int value, int expected) throws InterruptedException {
            completion.awaitSuccessor(value);
            assertEquals(expected, value);
        }
    }

    /** Signals result recording after the production publisher returns. */
    public static class ReverseCompletionPublisher extends TableTestPublisher {
        @Override
        public void testSuccessful(ExtensionContext context) {
            super.testSuccessful(context);
            ExpandedTable.completion.recorded(context);
        }

        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            super.testFailed(context, cause);
            ExpandedTable.completion.recorded(context);
        }
    }
}
