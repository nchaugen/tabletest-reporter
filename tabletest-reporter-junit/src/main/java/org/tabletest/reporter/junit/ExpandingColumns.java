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
