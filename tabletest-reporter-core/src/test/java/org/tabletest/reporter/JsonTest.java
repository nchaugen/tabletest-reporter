package org.tabletest.reporter;

import org.junit.jupiter.api.Test;
import org.tabletest.junit.Scenario;
import org.tabletest.junit.TableTest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the minimal JSON encoder used to emit the shared search-index asset. The output
 * doubles as a JavaScript literal, so string escaping must also neutralise the two Unicode
 * line separators (U+2028/U+2029) that are legal in JSON but break a JS string literal.
 */
// Unpublished: internal mechanism (JSON value serialization), not a user-facing reporter rule.
class JsonTest {

    @TableTest("""
        Scenario      | Value | Encoded?
        Plain string  | hello | '"hello"'
        Empty string  | ''    | '""'
        Integer       | 42    | 42
        Boolean true  | true  | true
        Boolean false | false | false
        Null          |       | null
        """)
    void encodes_scalars(@Scenario String scenario, String value, String encoded) {
        assertThat(Json.encode(coerce(value))).isEqualTo(encoded);
    }

    @Test
    void escapes_quotes_and_backslashes() {
        assertThat(Json.encode("say \"hi\" \\ bye")).isEqualTo("\"say \\\"hi\\\" \\\\ bye\"");
    }

    @Test
    void escapes_the_c_style_control_characters() {
        assertThat(Json.encode("a\tb\nc\rd")).isEqualTo("\"a\\tb\\nc\\rd\"");
    }

    @Test
    void escapes_other_control_characters_as_unicode() {
        assertThat(Json.encode("a" + (char) 0x01 + "b")).isEqualTo("\"a\\u0001b\"");
    }

    @Test
    void escapes_the_javascript_line_separators() {
        String withSeparators = "a" + (char) 0x2028 + "b" + (char) 0x2029 + "c";
        assertThat(Json.encode(withSeparators)).isEqualTo("\"a\\u2028b\\u2029c\"");
    }

    @Test
    void encodes_a_list_as_a_json_array() {
        assertThat(Json.encode(List.of("a", "b", "c"))).isEqualTo("[\"a\",\"b\",\"c\"]");
    }

    @Test
    void encodes_an_empty_list_as_empty_brackets() {
        assertThat(Json.encode(List.of())).isEqualTo("[]");
    }

    @Test
    void encodes_a_map_as_a_json_object_preserving_order() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", "Leap Year");
        map.put("type", "table");
        assertThat(Json.encode(map)).isEqualTo("{\"title\":\"Leap Year\",\"type\":\"table\"}");
    }

    @Test
    void encodes_nested_lists_of_maps() {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("path", "a/b.html");
        entry.put("text", "content");
        assertThat(Json.encode(List.of(entry))).isEqualTo("[{\"path\":\"a/b.html\",\"text\":\"content\"}]");
    }

    private static Object coerce(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "42" -> 42;
            case "true" -> true;
            case "false" -> false;
            default -> value;
        };
    }
}
