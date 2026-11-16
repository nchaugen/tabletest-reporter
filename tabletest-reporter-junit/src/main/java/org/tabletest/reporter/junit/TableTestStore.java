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
package org.tabletest.reporter.junit;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.tabletest.parser.Table;
import org.tabletest.parser.TableParser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages storage of TableTest data and results using JUnit's ExtensionContext stores.
 *
 * Uses two storage scopes:
 * - Test method store: Per-method data (table, row results)
 * - Class store: Per-class data (list of methods to publish)
 */
class TableTestStore {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(TableTestPublisher.class);

    /**
     * Stores a row result for a test invocation.
     */
    void storeRowResult(ExtensionContext context, RowResult result) {
        ExtensionContext.Store store = getTestMethodStore(context);
        @SuppressWarnings("unchecked")
        Queue<RowResult> results = (Queue<RowResult>)
                store.getOrComputeIfAbsent("rowResults", key -> new ConcurrentLinkedQueue<RowResult>());
        results.add(result);
    }

    /**
     * Initialises the table atomically and registers its method once, preserving registration order.
     */
    @SuppressWarnings("unchecked")
    void registerTable(ExtensionContext methodContext, String input) {
        getTestMethodStore(methodContext).getOrComputeIfAbsent("table", key -> TableParser.parse(input));
        ExtensionContext.Store classStore = getClassStore(methodContext);
        Map<String, ExtensionContext> contexts = (Map<String, ExtensionContext>)
                classStore.getOrComputeIfAbsent("methodContexts", key -> new LinkedHashMap<String, ExtensionContext>());
        synchronized (contexts) {
            contexts.putIfAbsent(methodContext.getUniqueId(), methodContext);
        }
    }

    /**
     * Retrieves all method contexts that need to be published.
     */
    @SuppressWarnings("unchecked")
    List<ExtensionContext> getMethodsToPublish(ExtensionContext classContext) {
        ExtensionContext.Store classStore = getClassStore(classContext);
        Map<String, ExtensionContext> contexts = (Map<String, ExtensionContext>) classStore.get("methodContexts");
        if (contexts == null) {
            return List.of();
        }
        synchronized (contexts) {
            return List.copyOf(contexts.values());
        }
    }

    /**
     * Stores published table test metadata for the current class.
     */
    @SuppressWarnings("unchecked")
    void storePublishedTableTest(ExtensionContext methodContext, PublishedTableTestInfo info) {
        ExtensionContext.Store classStore = getClassStore(methodContext);
        Queue<PublishedTableTestInfo> tests = (Queue<PublishedTableTestInfo>) classStore.getOrComputeIfAbsent(
                "publishedTableTests", key -> new ConcurrentLinkedQueue<PublishedTableTestInfo>());
        tests.add(info);
    }

    /**
     * Retrieves published table test metadata for the class.
     */
    @SuppressWarnings("unchecked")
    List<PublishedTableTestInfo> getPublishedTableTests(ExtensionContext classContext) {
        ExtensionContext.Store classStore = getClassStore(classContext);
        Queue<PublishedTableTestInfo> tests = (Queue<PublishedTableTestInfo>) classStore.get("publishedTableTests");
        return tests != null ? List.copyOf(tests) : List.of();
    }

    /**
     * Retrieves stored parsed table for a method context.
     */
    Table getTable(ExtensionContext methodContext) {
        return (Table) getTestMethodStore(methodContext).get("table");
    }

    /**
     * Retrieves stored row results for a method context.
     */
    @SuppressWarnings("unchecked")
    List<RowResult> getRowResults(ExtensionContext methodContext) {
        Queue<RowResult> results =
                (Queue<RowResult>) getTestMethodStore(methodContext).get("rowResults");
        return results != null ? List.copyOf(results) : null;
    }

    private ExtensionContext.Store getTestMethodStore(ExtensionContext context) {
        return context.getStore(NAMESPACE);
    }

    private ExtensionContext.Store getClassStore(ExtensionContext context) {
        return context.getRoot().getStore(ExtensionContext.Namespace.create(context.getRequiredTestClass()));
    }
}
