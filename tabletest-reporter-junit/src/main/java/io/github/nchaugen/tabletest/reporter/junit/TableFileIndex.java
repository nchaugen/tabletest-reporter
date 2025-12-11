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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks published table files for a test class to enable creating an index.
 */
class TableFileIndex {
    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(TableFileIndex.class);

    /**
     * Saves a table file entry for later indexing.
     */
    static void save(String title, Path path, ExtensionContext context) {
        ExtensionContext.Store store = getClassStore(context);
        List<TableFileEntry> entries = getOrCreateEntries(store);
        entries.add(new TableFileEntry(title, path));
    }

    /**
     * Retrieves all table file entries for the test class.
     */
    static List<TableFileEntry> allForTestClass(ExtensionContext context) {
        ExtensionContext.Store store = getClassStore(context);
        List<TableFileEntry> entries = getOrCreateEntries(store);
        return List.copyOf(entries);
    }

    private static ExtensionContext.Store getClassStore(ExtensionContext context) {
        return context.getRoot().getStore(
            ExtensionContext.Namespace.create(context.getRequiredTestClass())
        );
    }

    @SuppressWarnings("unchecked")
    private static List<TableFileEntry> getOrCreateEntries(ExtensionContext.Store store) {
        return (List<TableFileEntry>) store.getOrComputeIfAbsent(
            "tableFileEntries",
            key -> new ArrayList<TableFileEntry>()
        );
    }
}
