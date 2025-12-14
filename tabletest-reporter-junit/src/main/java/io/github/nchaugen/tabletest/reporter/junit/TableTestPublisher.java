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

import io.github.nchaugen.tabletest.junit.Description;
import io.github.nchaugen.tabletest.junit.InputResolver;
import io.github.nchaugen.tabletest.junit.TableTest;
import io.github.nchaugen.tabletest.parser.Table;
import io.github.nchaugen.tabletest.parser.TableParser;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.MediaType;
import org.junit.jupiter.api.extension.TestWatcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

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
        context.getParent().ifPresent(parentContext -> {
            parentContext.getTestMethod().ifPresent(method -> {
                TableTest tableTest = method.getAnnotation(TableTest.class);
                if (tableTest != null) {
                    // Ensure table metadata is stored (only happens once per test method)
                    ensureTableMetadataStored(parentContext, tableTest);

                    // Store this invocation's result
                    int rowIndex = getInvocationIndex(context);
                    store.storeRowResult(
                        parentContext,
                        new RowResult(rowIndex, passed, cause, context.getDisplayName())
                    );
                }
            });
        });
    }

    /**
     * Ensures table metadata is stored exactly once per test method.
     * On first invocation: parses table, stores metadata, marks method for publishing.
     * On subsequent invocations: does nothing (metadata already stored).
     */
    private void ensureTableMetadataStored(ExtensionContext methodContext, TableTest tableTest) {
        if (store.hasTable(methodContext)) {
            return; // Already stored
        }

        // Parse and store table
        String input = InputResolver.resolveInput(methodContext, tableTest);
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
        publishTables(context);
        publishTestClass(context);
    }

    private void publishTables(ExtensionContext context) {
        List<ExtensionContext> methodContexts = store.getMethodsToPublish(context);

        for (ExtensionContext methodContext : methodContexts) {
            Table table = store.getTable(methodContext);
            List<RowResult> rowResults = store.getRowResults(methodContext);

            if (table != null && rowResults != null) {
                publishTable(methodContext, table, rowResults);
            }
        }
    }

    private static void publishTable(ExtensionContext context, Table table, List<RowResult> rowResults) {
        TableMetadata metadata = new JunitTableMetadata(context, table, rowResults);
        TableTestData data = metadata.toTableTestData(table);

        publishFile(
            context,
            getName(context, () -> context.getRequiredTestMethod().getName()),
            (Path path) -> YAML_RENDERER.render(data)
        );
    }

    public static void publishTestClass(ExtensionContext context) {
        TestClassData data = new TestClassData(context.getDisplayName(), findDescription(context));
        
        publishFile(
            context,
            getName(context, () -> context.getRequiredTestClass().getSimpleName()),
            (Path path) -> YAML_RENDERER.render(data)
        );
    }

    private static void publishFile(ExtensionContext context, String fileName, Function<Path, String> renderer) {
        context.publishFile(
            FILENAME_PREFIX + fileName + YAML_EXTENSION,
            MediaType.TEXT_PLAIN_UTF_8,
            path -> Files.writeString(path, renderer.apply(path))
        );
    }

    private static @NonNull String getName(ExtensionContext context, Supplier<String> defaultName) {
        return context.getElement()
            .filter(it -> it.isAnnotationPresent(DisplayName.class))
            .map(__ -> context.getDisplayName())
            .orElseGet(defaultName);
    }

    private static String findDescription(ExtensionContext context) {
        return context.getTestClass()
            .map(it -> it.getAnnotation(Description.class))
            .map(Description::value)
            .orElse(null);
    }

}
