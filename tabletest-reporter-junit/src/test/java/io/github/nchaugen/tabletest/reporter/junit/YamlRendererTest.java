package io.github.nchaugen.tabletest.reporter.junit;

import io.github.nchaugen.tabletest.parser.TableParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class YamlRendererTest {

    private final YamlRenderer renderer = new YamlRenderer();

    @Test
    void shouldRenderClass() {
        assertEquals( // language=yaml
                """
                        "title": "Title of the Test Class"
                        "description": "A free-text description explaining what these tables are about."
                        """,
                renderer.render(new TestClassData(
                        "Title of the Test Class", "A free-text description explaining what these tables are about.")));
    }

    @Test
    void shouldRenderClassNameAndSlug() {
        assertEquals( // language=yaml
                """
                        "className": "com.example.SampleTest"
                        "slug": "sample-test"
                        "title": "Sample Test Class"
                        "description": "Sample description."
                        "tableTests":
                        - "path": "TABLETEST-sample.yaml"
                          "title": "Sample Table"
                          "methodName": "sampleMethod"
                          "slug": "sample-method"
                        - "path": "nested/TABLETEST-other.yaml"
                          "title": "Other Table"
                          "methodName": "otherMethod"
                          "slug": "other-method"
                        """,
                renderer.render(new TestClassData(
                        "com.example.SampleTest",
                        "sample-test",
                        "Sample Test Class",
                        "Sample description.",
                        List.of(
                                new PublishedTableTest(
                                        "TABLETEST-sample.yaml", "Sample Table", "sampleMethod", "sample-method"),
                                new PublishedTableTest(
                                        "nested/TABLETEST-other.yaml",
                                        "Other Table",
                                        "otherMethod",
                                        "other-method")))));
    }

    @Test
    void shouldRenderMethodNameAndSlug() {
        assertEquals( // language=yaml
                """
                        "methodName": "sampleMethod"
                        "slug": "sample-method"
                        "headers":
                        - "value": "a"
                        "rows":
                        - - "value": "1"
                        """,
                renderer.render(new TableMetadata()
                        .withMethodName("sampleMethod")
                        .withSlug("sample-method")
                        .toTableTestData(TableParser.parse("""
                                a
                                1
                                """))));
    }

    @Test
    void shouldRenderTitleAndDescriptionIfPresent() {
        assertEquals( // language=yaml
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
                renderer.render(new TableMetadata()
                        .withTitle("Table Title")
                        .withDescription("""
                                This is a description of the __table__.

                                It can span multiple lines, and include lists and formatting:

                                - List item 1
                                - List item 2
                                """)
                        .toTableTestData(TableParser.parse("""
                                a | b
                                1 | 2
                                """))));
    }

    @Test
    void shouldAddRoleForScenarioCells() {
        assertEquals( // language=yaml
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
                renderer.render(new TableMetadata()
                        .withColumnRoles(new ColumnRoles(0, Set.of(2)))
                        .toTableTestData(TableParser.parse("""
                                scenario | input | output?
                                add      | 5     | 5
                                multiply | 3     | 15
                                """))));
    }

    @Test
    void shouldAddRoleForExpectationCells() {
        assertEquals( // language=yaml
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
                renderer.render(new TableMetadata()
                        .withColumnRoles(new ColumnRoles(0, Set.of(0, 1, 2, 3, 4)))
                        .toTableTestData(TableParser.parse("""
                                a? | b?      | c? | d? | e?
                                {} | [1,2,3] | 3  |    | [a:1,b:2,c:3]
                                """))));
    }

    @Test
    void shouldRenderNullEmptyStringAndExplicitWhitespace() {
        assertEquals( // language=yaml
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
                        """, renderer.render(new TableMetadata().toTableTestData(TableParser.parse("""
                        a | b  | c d   | " e "     | f    | g
                          | "" | "   " | a bc  def | '\t' | '\t '
                        """))));
    }

    @Test
    void shouldRenderEscapedPipe() {
        assertEquals( // language=yaml
                """
                        "headers":
                        - "value": "++"
                        - "value": "+"
                        - "value": "a|b"
                        "rows":
                        - - "value": "|"
                          - "value": "|"
                          - "value": "Text with | character"
                        """, renderer.render(new TableMetadata().toTableTestData(TableParser.parse("""
                        ++  | +   | 'a|b'
                        "|" | '|' | "Text with | character"
                        """))));
    }

    @Test
    void shouldRenderList() {
        assertEquals( // language=yaml
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
                        """, renderer.render(new TableMetadata().toTableTestData(TableParser.parse("""
                        a  | b         | c
                        [] | [1,2,3] | ['|', "|"]
                        """))));
    }

    @Test
    void shouldRenderEmptyListWhenNested() {
        assertEquals( // language=yaml
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
                        """, renderer.render(new TableMetadata().toTableTestData(TableParser.parse("""
                        a  | b    | c
                        [] | [[]] | [[[]]]
                        """))));
    }

    @Test
    void shouldRenderNestedLists() {
        assertEquals( // language=yaml
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
                        """, renderer.render(new TableMetadata().toTableTestData(TableParser.parse("""
                        a
                        [[1,2,3],[a,b,c],[#,$,%]]
                        """))));
    }

    @Test
    void shouldRenderSet() {
        assertEquals( // language=yaml
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
                        """, renderer.render(new TableMetadata().toTableTestData(TableParser.parse("""
                        a  | b   | c
                        {} | {1,2,3} | {"||"}
                        """))));
    }

    @Test
    void shouldRenderNestedSets() {
        assertEquals( // language=yaml
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
                        """, renderer.render(new TableMetadata().toTableTestData(TableParser.parse("""
                        a
                        {{1,2,3}, {a,b,c}, {#,$,%}}
                        """))));
    }

    @Test
    void shouldRenderMapAsDescriptionList() {
        assertEquals( // language=yaml
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
                        """, renderer.render(new TableMetadata().toTableTestData(TableParser.parse("""
                        a   | b             | c
                        [:] | [a:1,b:2,c:3] | [b: "||"]
                        """))));
    }

    @Test
    void shouldRenderNestedMaps() {
        assertEquals( // language=yaml
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
                        """, renderer.render(new TableMetadata().toTableTestData(TableParser.parse("""
                        a                | b
                        [a: [:], b: [:]] | [a: [A: 1],b: [B: 2]]
                        """))));
    }

    @Test
    void shouldRenderNestedMixedCollections() {
        assertEquals("""
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
                """, renderer.render(new TableMetadata().toTableTestData(TableParser.parse("""
                a                            | b
                [a: [1, 2], b: {3, 4}, c: 5] | {[A: 1], [B: 2]}
                """))));
    }

    @Test
    void shouldIncludeRowResultsInYaml() {
        String yaml = renderer.render(new TableMetadata()
                .withRowResults(List.of(
                        new RowResult(0, true, null, "test[1]"),
                        new RowResult(1, false, new AssertionError("Expected 4"), "test[2]")))
                .toTableTestData(TableParser.parse("""
                        a | b
                        1 | 2
                        3 | 4
                        """)));

        assertEquals("""
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
                """, yaml);
    }
}
