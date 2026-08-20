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

import java.util.List;
import java.util.Map;

/**
 * The role tokens each column declares, through the annotations on the test method parameter that
 * column binds to. Kept apart from the roles the reporter derives, so a declared role reaches the
 * published cells without reaching anything that decides how the run is reported.
 */
public final class DeclaredColumnRoles {

    public static final DeclaredColumnRoles NONE = new DeclaredColumnRoles(Map.of());

    private final Map<Integer, List<String>> rolesByColumn;

    public DeclaredColumnRoles(Map<Integer, List<String>> rolesByColumn) {
        this.rolesByColumn = Map.copyOf(rolesByColumn);
    }

    public List<String> rolesFor(int columnIndex) {
        return rolesByColumn.getOrDefault(columnIndex, List.of());
    }
}
