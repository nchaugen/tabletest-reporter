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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.tabletest.parser.Table;

import java.util.ArrayList;
import java.util.List;

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
        List<RowResult> results =
                (List<RowResult>) store.getOrComputeIfAbsent("rowResults", key -> new ArrayList<RowResult>());
        results.add(result);
    }

    /**
     * Stores parsed table for a TableTest method.
     */
    void storeTable(ExtensionContext context, Table table) {
        getTestMethodStore(context).put("table", table);
    }

    /**
     * Checks if a table has already been stored for this TableTest method.
     */
    boolean hasTable(ExtensionContext context) {
        return getTestMethodStore(context).get("table") != null;
    }

    /**
     * Marks that this test class has TableTests.
     */
    void markClassAsHavingTableTests(ExtensionContext context) {
        getClassStore(context).put("hasTableTests", true);
    }

    /**
     * Adds a method context to the list of methods that need publishing.
     */
    @SuppressWarnings("unchecked")
    void addMethodForPublishing(ExtensionContext methodContext) {
        ExtensionContext.Store classStore = getClassStore(methodContext);
        List<ExtensionContext> contexts = (List<ExtensionContext>)
                classStore.getOrComputeIfAbsent("methodContexts", key -> new ArrayList<ExtensionContext>());
        contexts.add(methodContext);
    }

    /**
     * Retrieves all method contexts that need to be published.
     */
    @SuppressWarnings("unchecked")
    List<ExtensionContext> getMethodsToPublish(ExtensionContext classContext) {
        ExtensionContext.Store classStore = getClassStore(classContext);
        List<ExtensionContext> contexts = (List<ExtensionContext>) classStore.get("methodContexts");
        return contexts != null ? contexts : List.of();
    }

    /**
     * Stores published table test metadata for the current class.
     */
    @SuppressWarnings("unchecked")
    void storePublishedTableTest(ExtensionContext methodContext, PublishedTableTestInfo info) {
        ExtensionContext.Store classStore = getClassStore(methodContext);
        List<PublishedTableTestInfo> tests = (List<PublishedTableTestInfo>)
                classStore.getOrComputeIfAbsent("publishedTableTests", key -> new ArrayList<PublishedTableTestInfo>());
        tests.add(info);
    }

    /**
     * Retrieves published table test metadata for the class.
     */
    @SuppressWarnings("unchecked")
    List<PublishedTableTestInfo> getPublishedTableTests(ExtensionContext classContext) {
        ExtensionContext.Store classStore = getClassStore(classContext);
        List<PublishedTableTestInfo> tests = (List<PublishedTableTestInfo>) classStore.get("publishedTableTests");
        return tests != null ? tests : List.of();
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
        return (List<RowResult>) getTestMethodStore(methodContext).get("rowResults");
    }

    private ExtensionContext.Store getTestMethodStore(ExtensionContext context) {
        return context.getStore(NAMESPACE);
    }

    private ExtensionContext.Store getClassStore(ExtensionContext context) {
        return context.getRoot().getStore(ExtensionContext.Namespace.create(context.getRequiredTestClass()));
    }
}
