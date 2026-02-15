/*
 * Copyright 2025-present Nils Christian Haugen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.nchaugen.tabletest.reporter.junit;

import io.github.nchaugen.tabletest.parser.Table;
import io.github.nchaugen.tabletest.parser.TableParser;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.MediaType;
import org.junit.jupiter.api.extension.TestWatcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

public class TableTestPublisher implements TestWatcher, AfterAllCallback {

    private static final YamlRenderer YAML_RENDERER = new YamlRenderer();
    private static final String FILENAME_PREFIX = "TABLETEST-";
    private static final String YAML_EXTENSION = ".yaml";

    private final TableTestStore store = new TableTestStore();

    @Override
    public void testSuccessful(ExtensionContext context) {
        recordInvocationResult(context, true, null);
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        recordInvocationResult(context, false, cause);
    }

    private void recordInvocationResult(ExtensionContext context, boolean passed, Throwable cause) {
        // Get the parent context (test template context for parameterized tests)
        context.getParent().ifPresent(parentContext -> parentContext
                .getTestMethod()
                .flatMap(method ->
                        TableTestAnnotationResolver.resolveInput(method, parentContext.getRequiredTestClass()))
                .ifPresent(input -> {
                    // Ensure table metadata is stored (only happens once per test method)
                    ensureTableMetadataStored(parentContext, input);

                    // Store this invocation's result
                    int rowIndex = getInvocationIndex(context);
                    store.storeRowResult(
                            parentContext, new RowResult(rowIndex, passed, cause, context.getDisplayName()));
                }));
    }

    /**
     * Ensures table metadata is stored exactly once per test method.
     * On first invocation: parses table, stores metadata, marks method for publishing.
     * On subsequent invocations: does nothing (metadata already stored).
     */
    private void ensureTableMetadataStored(ExtensionContext methodContext, String input) {
        if (store.hasTable(methodContext)) {
            return; // Already stored
        }

        // Parse and store table
        Table table = TableParser.parse(input);
        store.storeTable(methodContext, table);

        // Mark this method for publishing and mark class as having table tests
        store.addMethodForPublishing(methodContext);
        store.markClassAsHavingTableTests(methodContext);
    }

    private static int getInvocationIndex(ExtensionContext context) {
        // Extract invocation index from the unique ID
        // Format: [engine:junit-jupiter]/[class:...]/[test-template:...]/[test-template-invocation:#N]
        String uniqueId = context.getUniqueId();
        int start = uniqueId.lastIndexOf("#");
        if (start >= 0 && start < uniqueId.length() - 2) {
            int end = uniqueId.indexOf("]", start);
            if (end > start) {
                try {
                    return Integer.parseInt(uniqueId.substring(start + 1, end));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    @Override
    public void afterAll(ExtensionContext context) {
        List<ExtensionContext> methodContexts = store.getMethodsToPublish(context);
        if (!methodContexts.isEmpty()) {
            publishTables(methodContexts);
            publishTestClass(context);
        }
    }

    private void publishTables(List<ExtensionContext> methodContexts) {
        for (ExtensionContext methodContext : methodContexts) {
            Table table = store.getTable(methodContext);
            List<RowResult> rowResults = store.getRowResults(methodContext);

            if (table != null && rowResults != null) {
                publishTable(methodContext, table, rowResults);
            }
        }
    }

    private void publishTable(ExtensionContext context, Table table, List<RowResult> rowResults) {
        TableTestIdentity identity = JunitTestIdentityExtractor.extract(context);
        ColumnRoles columnRoles = JunitColumnRoleExtractor.extract(context, table);
        TableTestData data = TableTestDataFactory.create(table, identity, columnRoles, rowResults);

        publishFile(context, identity.slug(), (Path path) -> {
            store.storePublishedTableTest(
                    context,
                    new PublishedTableTestInfo(path, identity.title(), identity.methodName(), identity.slug()));
            return YAML_RENDERER.render(data);
        });
    }

    public void publishTestClass(ExtensionContext context) {
        TestClassIdentity identity = JunitClassIdentityExtractor.extract(context);
        List<PublishedTableTestInfo> publishedTests = store.getPublishedTableTests(context);

        publishFile(context, identity.slug(), (Path path) -> {
            List<PublishedTableTest> tableTests = buildPublishedTableTests(path.getParent(), publishedTests);
            TestClassData data = new TestClassData(
                    identity.className(), identity.slug(), identity.title(), identity.description(), tableTests);
            return YAML_RENDERER.render(data);
        });
    }

    @SuppressWarnings("removal")
    private static void publishFile(ExtensionContext context, String fileName, Function<Path, String> renderer) {
        context.publishFile(
                FILENAME_PREFIX + fileName + YAML_EXTENSION,
                MediaType.TEXT_PLAIN_UTF_8,
                path -> Files.writeString(path, renderer.apply(path)));
    }

    private static List<PublishedTableTest> buildPublishedTableTests(
            Path classDir, List<PublishedTableTestInfo> tests) {
        if (tests == null || tests.isEmpty()) {
            return List.of();
        }
        return tests.stream()
                .map(info -> new PublishedTableTest(
                        relativizePath(classDir, info.path()), info.title(), info.methodName(), info.slug()))
                .toList();
    }

    private static String relativizePath(Path baseDir, Path target) {
        if (baseDir == null || target == null) {
            return target != null ? target.toString() : null;
        }
        try {
            return baseDir.relativize(target).toString();
        } catch (IllegalArgumentException e) {
            return target.toString();
        }
    }
}
