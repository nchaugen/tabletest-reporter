package org.tabletest.reporter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tabletest.junit.Description;
import org.tabletest.junit.Scenario;
import org.tabletest.junit.TableTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the shared, whole-report search index: a flat list of one entry per page, each
 * carrying the page's root-relative path, display title, roll-up status, and a searchable text
 * blob flattened from the page's own title, description, headers, and cell values. The index is
 * emitted once to the output root and searched client-side from every page.
 */
@DisplayName("Whole-report search")
@Description("""
        Every report carries a search index with one entry per page. A table page is
        searchable by its title, description, column headers, and cell values; an index
        page only by its title. The index is built once at the output root and searched
        client-side from every page.
        """)
class SearchIndexTest {

    private final TableNode leapYear = new TableNode(
            "leap-year-rules",
            "calendar/leap-year-rules",
            Map.of(
                    "title", "Leap Year Rules",
                    "description", "Gregorian leap year determination",
                    "headers", List.of(Map.of("value", "Year"), Map.of("value", "Is Leap Year?")),
                    "rows", List.of(List.of(Map.of("value", "2004"), Map.of("value", "Yes"))),
                    "rowResults", List.of(Map.of("passed", true))));
    private final IndexNode calendar =
            new IndexNode("calendar", "calendar", Map.of("title", "Calendar"), List.of(leapYear));
    private final IndexNode root = new IndexNode(null, "", null, List.of(calendar));

    @Test
    void has_one_entry_per_page_in_the_tree() {
        assertThat(paths()).containsExactly("index.html", "calendar/index.html", "calendar/leap-year-rules.html");
    }

    @Test
    void a_table_entry_carries_its_path_title_type_and_status() {
        Map<String, Object> entry = entryFor("calendar/leap-year-rules.html");

        assertThat(entry.get("title")).isEqualTo("Leap Year Rules");
        assertThat(entry.get("type")).isEqualTo("table");
        assertThat(entry.get("status")).isEqualTo("passed");
    }

    @Test
    void a_table_entry_text_includes_its_description_headers_and_cell_values() {
        String text = (String) entryFor("calendar/leap-year-rules.html").get("text");

        assertThat(text)
                .contains("Leap Year Rules")
                .contains("Gregorian leap year determination")
                .contains("Year")
                .contains("Is Leap Year?")
                .contains("2004")
                .contains("Yes");
    }

    @Test
    void an_index_entry_uses_its_title_and_does_not_absorb_child_text() {
        Map<String, Object> entry = entryFor("calendar/index.html");

        assertThat(entry.get("title")).isEqualTo("Calendar");
        assertThat(entry.get("type")).isEqualTo("index");
        assertThat((String) entry.get("text")).doesNotContain("2004");
    }

    @Test
    void renders_a_self_contained_javascript_asset_assigning_the_global_index() {
        String javascript = SearchIndex.of(root).asJavaScript();

        assertThat(javascript).startsWith("window.TableTestSearchIndex = [");
        assertThat(javascript).endsWith("];\n");
        assertThat(javascript).contains("\"calendar/leap-year-rules.html\"");
    }

    @DisplayName("Finds pages whose title or text contains the query")
    @Description("""
            The searched report holds one table, "Leap Year Rules" (description "Gregorian
            leap year determination", column headers Year and Is Leap Year?, one row:
            2004 / Yes), inside a feature titled "Calendar". Matching is case-insensitive
            substring matching.
            """)
    @TableTest("""
        Scenario                         | Query     | Matching pages?
        Matches a cell value             | 2004      | [calendar/leap-year-rules.html]
        Matches a description word       | gregorian | [calendar/leap-year-rules.html]
        Matches title regardless of case | YEAR      | [calendar/leap-year-rules.html]
        Matches an index by title        | calendar  | [calendar/index.html]
        No page matches                  | zzz       | []
        Blank query matches nothing      | ''        | []
        """)
    void finds_pages_whose_title_or_text_contains_the_query(
            @Scenario String scenario, String query, List<String> matchingPages) {
        List<String> paths = SearchIndex.of(root).search(query).stream()
                .map(entry -> (String) entry.get("path"))
                .toList();

        assertThat(paths).containsExactlyInAnyOrderElementsOf(matchingPages);
    }

    private List<String> paths() {
        return SearchIndex.of(root).entries().stream()
                .map(entry -> (String) entry.get("path"))
                .toList();
    }

    private Map<String, Object> entryFor(String path) {
        return SearchIndex.of(root).entries().stream()
                .filter(entry -> path.equals(entry.get("path")))
                .findFirst()
                .orElseThrow();
    }
}
