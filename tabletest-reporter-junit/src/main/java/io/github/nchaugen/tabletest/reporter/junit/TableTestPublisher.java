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

    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(TableTestPublisher.class);

    private static final YamlRenderer YAML_RENDERER = new YamlRenderer();
    private static final String FILENAME_PREFIX = "TABLETEST-";
    private static final String YAML_EXTENSION = ".yaml";


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
                    // Mark that this class has TableTest methods
                    getClassStore(parentContext).put("hasTableTests", true);

                    // Get invocation index
                    int rowIndex = getInvocationIndex(context);

                    // Store the row result
                    storeRowResult(
                        parentContext, new RowResult(
                            rowIndex, passed, cause, context.getDisplayName()
                        )
                    );

                    // Store the table and annotation for later republishing
                    ExtensionContext.Store store = getTestMethodStore(parentContext);
                    if (store.get("tableTest") == null) {
                        store.put("tableTest", tableTest);
                        String input = InputResolver.resolveInput(parentContext, tableTest);
                        Table table = TableParser.parse(input);
                        store.put("table", table);

                        // Add this method context to the class-level list for republishing
                        addMethodContextForRepublishing(parentContext);
                    }
                }
            });
        });
    }

    private static void publishTable(ExtensionContext context, TableTest tableTest, Table table, List<RowResult> rowResults) {
        TableMetadata metadata = new JunitTableMetadata(context, table, rowResults);

        publishFile(
            context, getName(context, () -> context.getRequiredTestMethod().getName()), (Path path) -> {
                TableFileIndex.save(metadata.title(), path, context);
                return YAML_RENDERER.renderTable(table, metadata);
            }
        );
    }

    private static @NonNull String getName(ExtensionContext context, Supplier<String> defaultName) {
        return context.getElement()
            .filter(it -> it.isAnnotationPresent(DisplayName.class))
            .map(__ -> context.getDisplayName())
            .orElseGet(defaultName);
    }

    private static void publishFile(ExtensionContext context, String fileName, Function<Path, String> renderer) {
        context.publishFile(
            FILENAME_PREFIX + fileName + YAML_EXTENSION,
            MediaType.TEXT_PLAIN_UTF_8,
            path -> Files.writeString(path, renderer.apply(path))
        );
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

    private static void storeRowResult(ExtensionContext context, RowResult result) {
        ExtensionContext.Store store = getTestMethodStore(context);
        @SuppressWarnings("unchecked")
        List<RowResult> results = (List<RowResult>) store.getOrComputeIfAbsent(
            "rowResults",
            key -> new java.util.ArrayList<RowResult>()
        );
        results.add(result);
    }

    private static ExtensionContext.Store getTestMethodStore(ExtensionContext context) {
        return context.getStore(NAMESPACE);
    }

    private static ExtensionContext.Store getClassStore(ExtensionContext context) {
        return context.getRoot().getStore(ExtensionContext.Namespace.create(
            context.getRequiredTestClass()
        ));
    }

    private static void addMethodContextForRepublishing(ExtensionContext methodContext) {
        ExtensionContext.Store classStore = getClassStore(methodContext);
        @SuppressWarnings("unchecked")
        List<ExtensionContext> contexts = (List<ExtensionContext>) classStore.getOrComputeIfAbsent(
            "methodContexts",
            key -> new java.util.ArrayList<ExtensionContext>()
        );
        contexts.add(methodContext);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        publishTables(context);
        publishTestClass(context);
    }

    private void publishTables(ExtensionContext context) {
        ExtensionContext.Store classStore = getClassStore(context);
        @SuppressWarnings("unchecked")
        List<ExtensionContext> methodContexts = (List<ExtensionContext>) classStore.get("methodContexts");

        if (methodContexts != null) {
            for (ExtensionContext methodContext : methodContexts) {
                ExtensionContext.Store methodStore = getTestMethodStore(methodContext);
                TableTest tableTest = (TableTest) methodStore.get("tableTest");
                Table table = (Table) methodStore.get("table");
                @SuppressWarnings("unchecked")
                List<RowResult> rowResults = (List<RowResult>) methodStore.get("rowResults");

                if (tableTest != null && table != null && rowResults != null) {
                    publishTable(methodContext, tableTest, table, rowResults);
                }
            }
        }
    }

    public static void publishTestClass(ExtensionContext context) {
        publishFile(
            context, getName(context, () -> context.getRequiredTestClass().getSimpleName()), (Path path) ->
                YAML_RENDERER.renderClass(
                    context.getDisplayName(),
                    findDescription(context),
                    relativizeToIndex(path, TableFileIndex.allForTestClass(context))
                )
        );
    }

    private static List<TableFileEntry> relativizeToIndex(Path indexPath, List<TableFileEntry> tableFiles) {
        return tableFiles.stream()
            .map(it -> new TableFileEntry(it.title(), indexPath.getParent().relativize(it.path())))
            .toList();
    }

    private static String findDescription(ExtensionContext context) {
        return context.getTestClass()
            .map(it -> it.getAnnotation(Description.class))
            .map(Description::value)
            .orElse(null);
    }

}
