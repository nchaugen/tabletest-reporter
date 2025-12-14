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

import java.util.LinkedHashSet;
import java.util.OptionalInt;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;

public class ColumnRoles {
    public static final ColumnRoles NO_ROLES = new ColumnRoles(OptionalInt.empty(), Set.of());

    private final OptionalInt scenarioIndex;
    private final Set<Integer> expectationIndices;

    public ColumnRoles(int scenarioIndex, Set<Integer> expectationIndices) {
        this(scenarioIndex >= 0 ? OptionalInt.of(scenarioIndex) : OptionalInt.empty(), expectationIndices);
    }

    public ColumnRoles(OptionalInt scenarioIndex, Set<Integer> expectationIndices) {
        this.scenarioIndex = scenarioIndex;
        this.expectationIndices = expectationIndices;
    }

    public OptionalInt scenarioIndex() {
        return scenarioIndex;
    }

    public Set<CellRole> roleFor(int columnIndex) {
        Set<CellRole> roles = new LinkedHashSet<>();
        if (expectationIndices.contains(columnIndex)) {
            roles.add(CellRole.EXPECTATION);
        }
        if (scenarioIndex.isPresent() && columnIndex == scenarioIndex.getAsInt()) {
            roles.add(CellRole.SCENARIO);
        }
        return unmodifiableSet(roles);
    }
}
