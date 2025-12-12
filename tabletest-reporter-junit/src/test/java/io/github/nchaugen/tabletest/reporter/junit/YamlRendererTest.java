package io.github.nchaugen.tabletest.reporter.junit;

import io.github.nchaugen.tabletest.parser.TableParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static io.github.nchaugen.tabletest.reporter.junit.ColumnRoles.NO_ROLES;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class YamlRendererTest {

    private static final TableMetadata NO_METADATA = new StubTableMetadata(List.of());
    private final YamlRenderer renderer = new YamlRenderer();

    @Test
    void shouldRenderClass() {
        assertEquals(
            """
                "title": "Title of the Test Class"
                "description": "A free-text description explaining what these tables are about."
                "tables":
                  "A Table": "path/to/a_table"
                  "B Table": "path/to/b_table"
                  "C Table": "path/to/c_table"
                """,
            renderer.renderClass(
                "Title of the Test Class",
                "A free-text description explaining what these tables are about.",
                List.of(
                    new TableFileEntry("A Table", Path.of("path/to/a_table")),
                    new TableFileEntry("B Table", Path.of("path/to/b_table")),
                    new TableFileEntry("C Table", Path.of("path/to/c_table"))
                )
            )
        );
    }

    @Test
    void shouldRenderTitleAndDescriptionIfPresent() {
        assertEquals(//language=yaml
            """
                "title": "Table Title"
                "description": "This is a description of the __table__.\\n\\nIt can span multiple lines, and include lists and formatting:\\n\\n- List item 1\\n- List item 2\\n"
                "headers":
                - "value": "a"
                - "value": "b"
                "rows":
                - - "value": "1"
                  - "value": "2"
                """,
            renderer.renderTable(
                TableParser.parse("""
                    a | b
                    1 | 2
                    """),
                new StubTableMetadata(
                    "Table Title", """
                    This is a description of the __table__.
                    
                    It can span multiple lines, and include lists and formatting:
                    
                    - List item 1
                    - List item 2
                    """
                )
            )
        );
    }

    @Test
    void shouldAddRoleForScenarioCells() {
        assertEquals(//language=yaml
            """
                "headers":
                - "value": "scenario"
                  "roles":
                  - "scenario"
                - "value": "input"
                - "value": "output?"
                  "roles":
                  - "expectation"
                "rows":
                - - "value": "add"
                    "roles":
                    - "scenario"
                  - "value": "5"
                  - "value": "5"
                    "roles":
                    - "expectation"
                - - "value": "multiply"
                    "roles":
                    - "scenario"
                  - "value": "3"
                  - "value": "15"
                    "roles":
                    - "expectation"
                """,
            renderer.renderTable(
                TableParser.parse("""
                    scenario | input | output?
                    add      | 5     | 5
                    multiply | 3     | 15
                    """),
                new StubTableMetadata(new ColumnRoles(0, Set.of(2)))
            )
        );
    }

    @Test
    void shouldAddRoleForExpectationCells() {
        assertEquals(//language=yaml
            """
                "headers":
                - "value": "a?"
                  "roles":
                  - "expectation"
                  - "scenario"
                - "value": "b?"
                  "roles":
                  - "expectation"
                - "value": "c?"
                  "roles":
                  - "expectation"
                - "value": "d?"
                  "roles":
                  - "expectation"
                - "value": "e?"
                  "roles":
                  - "expectation"
                "rows":
                - - "value": !!set {}
                    "roles":
                    - "expectation"
                    - "scenario"
                  - "value":
                    - "1"
                    - "2"
                    - "3"
                    "roles":
                    - "expectation"
                  - "value": "3"
                    "roles":
                    - "expectation"
                  - "value": !!null "null"
                    "roles":
                    - "expectation"
                  - "value":
                      "a": "1"
                      "b": "2"
                      "c": "3"
                    "roles":
                    - "expectation"
                """,
            renderer.renderTable(
                TableParser.parse("""
                    a? | b?      | c? | d? | e?
                    {} | [1,2,3] | 3  |    | [a:1,b:2,c:3]
                    """),
                new StubTableMetadata(new ColumnRoles(0, Set.of(0, 1, 2, 3, 4)))
            )
        );
    }

    @Test
    void shouldRenderNullEmptyStringAndExplicitWhitespace() {
        assertEquals(//language=yaml
            """
                "headers":
                - "value": "a"
                - "value": "b"
                - "value": "c d"
                - "value": " e "
                - "value": "f"
                - "value": "g"
                "rows":
                - - "value": !!null "null"
                  - "value": ""
                  - "value": "   "
                  - "value": "a bc  def"
                  - "value": "\\t"
                  - "value": "\\t "
                """,
            renderer.renderTable(
                TableParser.parse("""
                    a | b  | c d   | " e "     | f    | g
                      | "" | "   " | a bc  def | '\t' | '\t '
                    """),
                NO_METADATA
            )
        );
    }

    @Test
    void shouldRenderEscapedPipe() {
        assertEquals(//language=yaml
            """
                "headers":
                - "value": "++"
                - "value": "+"
                - "value": "a|b"
                "rows":
                - - "value": "|"
                  - "value": "|"
                  - "value": "Text with | character"
                """,
            renderer.renderTable(
                TableParser.parse("""
                    ++  | +   | 'a|b'
                    "|" | '|' | "Text with | character"
                    """),
                NO_METADATA
            )
        );
    }

    @Test
    void shouldRenderList() {
        assertEquals(//language=yaml
            """
                "headers":
                - "value": "a"
                - "value": "b"
                - "value": "c"
                "rows":
                - - "value": []
                  - "value":
                    - "1"
                    - "2"
                    - "3"
                  - "value":
                    - "|"
                    - "|"
                """,
            renderer.renderTable(
                TableParser.parse("""
                    a  | b         | c
                    [] | [1,2,3] | ['|', "|"]
                    """),
                NO_METADATA
            )
        );
    }

    @Test
    void shouldRenderEmptyListWhenNested() {
        assertEquals(//language=yaml
            """
                "headers":
                - "value": "a"
                - "value": "b"
                - "value": "c"
                "rows":
                - - "value": []
                  - "value":
                    - []
                  - "value":
                    - - []
                """,
            renderer.renderTable(
                TableParser.parse("""
                    a  | b    | c
                    [] | [[]] | [[[]]]
                    """),
                NO_METADATA
            )
        );
    }

    @Test
    void shouldRenderNestedLists() {
        assertEquals(//language=yaml
            """
                "headers":
                - "value": "a"
                "rows":
                - - "value":
                    - - "1"
                      - "2"
                      - "3"
                    - - "a"
                      - "b"
                      - "c"
                    - - "#"
                      - "$"
                      - "%"
                """,
            renderer.renderTable(
                TableParser.parse("""
                    a
                    [[1,2,3],[a,b,c],[#,$,%]]
                    """),
                NO_METADATA
            )
        );
    }

    @Test
    void shouldRenderSet() {
        assertEquals(//language=yaml
            """
                "headers":
                - "value": "a"
                - "value": "b"
                - "value": "c"
                "rows":
                - - "value": !!set {}
                  - "value": !!set
                      "1": !!null "null"
                      "2": !!null "null"
                      "3": !!null "null"
                  - "value": !!set
                      "||": !!null "null"
                """,
            renderer.renderTable(
                TableParser.parse("""
                    a  | b   | c
                    {} | {1,2,3} | {"||"}
                    """),
                NO_METADATA
            )
        );
    }

    @Test
    void shouldRenderNestedSets() {
        assertEquals(//language=yaml
            """
                "headers":
                - "value": "a"
                "rows":
                - - "value": !!set
                      ? !!set
                        "1": !!null "null"
                        "2": !!null "null"
                        "3": !!null "null"
                      : !!null "null"
                      ? !!set
                        "a": !!null "null"
                        "b": !!null "null"
                        "c": !!null "null"
                      : !!null "null"
                      ? !!set
                        "#": !!null "null"
                        "$": !!null "null"
                        "%": !!null "null"
                      : !!null "null"
                """,
            renderer.renderTable(
                TableParser.parse("""
                    a
                    {{1,2,3}, {a,b,c}, {#,$,%}}
                    """),
                NO_METADATA
            )
        );
    }

    @Test
    void shouldRenderMapAsDescriptionList() {
        assertEquals(//language=yaml
            """
                "headers":
                - "value": "a"
                - "value": "b"
                - "value": "c"
                "rows":
                - - "value": {}
                  - "value":
                      "a": "1"
                      "b": "2"
                      "c": "3"
                  - "value":
                      "b": "||"
                """,
            renderer.renderTable(
                TableParser.parse("""
                    a   | b             | c
                    [:] | [a:1,b:2,c:3] | [b: "||"]
                    """),
                NO_METADATA
            )
        );
    }

    @Test
    void shouldRenderNestedMaps() {
        assertEquals(//language=yaml
            """
                "headers":
                - "value": "a"
                - "value": "b"
                "rows":
                - - "value":
                      "a": {}
                      "b": {}
                  - "value":
                      "a":
                        "A": "1"
                      "b":
                        "B": "2"
                """,
            renderer.renderTable(
                TableParser.parse("""
                    a                | b
                    [a: [:], b: [:]] | [a: [A: 1],b: [B: 2]]
                    """),
                NO_METADATA
            )
        );
    }

    @Test
    void shouldRenderNestedMixedCollections() {
        assertEquals(
            """
                "headers":
                - "value": "a"
                - "value": "b"
                "rows":
                - - "value":
                      "a":
                      - "1"
                      - "2"
                      "b": !!set
                        "3": !!null "null"
                        "4": !!null "null"
                      "c": "5"
                  - "value": !!set
                      ? "A": "1"
                      : !!null "null"
                      ? "B": "2"
                      : !!null "null"
                """,
            renderer.renderTable(
                TableParser.parse("""
                    a                            | b
                    [a: [1, 2], b: {3, 4}, c: 5] | {[A: 1], [B: 2]}
                    """),
                NO_METADATA
            )
        );
    }

    @Test
    void shouldIncludeRowResultsInYaml() {

        String yaml = renderer.renderTable(
            TableParser.parse("""
                a | b
                1 | 2
                3 | 4
                """),
            new StubTableMetadata(
                List.of(
                    new RowResult(0, true, null, "test[1]"),
                    new RowResult(1, false, new AssertionError("Expected 4"), "test[2]")
                )
            )
        );

        assertEquals(
            """
                "headers":
                - "value": "a"
                - "value": "b"
                "rows":
                - - "value": "1"
                  - "value": "2"
                - - "value": "3"
                  - "value": "4"
                "rowResults":
                - "rowIndex": !!int "0"
                  "passed": !!bool "true"
                  "displayName": "test[1]"
                - "rowIndex": !!int "1"
                  "passed": !!bool "false"
                  "displayName": "test[2]"
                  "errorMessage": "Expected 4"
                """, yaml
        );
    }

    private record StubTableMetadata(
        String title,
        String description,
        ColumnRoles columnRoles,
        List<RowResult> results
    ) implements TableMetadata {

        public StubTableMetadata(String title, String description) {
            this(title, description, NO_ROLES, List.of());
        }

        public StubTableMetadata(ColumnRoles columnRoles) {
            this(null, null, columnRoles, List.of());
        }

        public StubTableMetadata(List<RowResult> results) {
            this(null, null, NO_ROLES, results);
        }

        @Override
        public String title() {
            return title;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public ColumnRoles columnRoles() {
            return columnRoles;
        }

        @Override
        public List<RowResult> rowResults() {
            return results;
        }
    }

}
