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
