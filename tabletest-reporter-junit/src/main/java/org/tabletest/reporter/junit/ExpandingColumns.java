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

import java.util.Set;

/**
 * The columns whose set value expands a row into one run per value, rather than reaching the test
 * as a set. A column expands when the parameter bound to it is not itself a set, which is the same
 * rule the runtime applies when it decides what to do with a set value.
 */
public final class ExpandingColumns {

    public static final ExpandingColumns NONE = new ExpandingColumns(Set.of());

    private final Set<Integer> columnIndices;

    public ExpandingColumns(Set<Integer> columnIndices) {
        this.columnIndices = Set.copyOf(columnIndices);
    }

    /**
     * @return true if a set value in this column expands the row instead of being passed as a set.
     */
    public boolean expands(int columnIndex) {
        return columnIndices.contains(columnIndex);
    }
}
