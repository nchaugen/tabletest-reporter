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
package org.tabletest.reporter;

/**
 * Represents the depth of nesting to include in index files.
 * <p>
 * A depth of 1 shows only immediate children. A depth of 2 shows children and grandchildren.
 * {@link #INFINITE} shows all descendants regardless of nesting level.
 */
public record IndexDepth(int value) {

    public static final IndexDepth INFINITE = new IndexDepth(Integer.MAX_VALUE);
    public static final IndexDepth DEFAULT = INFINITE;

    public IndexDepth {
        if (value < 1) {
            throw new IllegalArgumentException("Index depth must be at least 1, was: " + value);
        }
    }

    public static IndexDepth of(int depth) {
        return new IndexDepth(depth);
    }

    /**
     * Parses a string value into an IndexDepth.
     * <p>
     * Accepts numeric values ("1", "2", etc.) or "infinite" for unlimited depth.
     * Null, empty, or blank values default to infinite depth.
     *
     * @param value the string to parse
     * @return the parsed IndexDepth
     * @throws IllegalArgumentException if value is not a valid depth
     */
    public static IndexDepth parse(String value) {
        if (value == null || value.isBlank()) {
            return INFINITE;
        }
        String trimmed = value.trim().toLowerCase();
        if ("infinite".equals(trimmed)) {
            return INFINITE;
        }
        try {
            return of(Integer.parseInt(trimmed));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid index depth: '" + value + "'. Expected a positive integer or 'infinite'.");
        }
    }

    public boolean isInfinite() {
        return value == Integer.MAX_VALUE;
    }
}
