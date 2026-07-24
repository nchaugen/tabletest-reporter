package org.tabletest.reporter;

import org.junit.jupiter.api.Test;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Unpublished: internal report-tree transform, not a user-facing rule.
class PublishSelectionApplyTest {

    @Description("""
            Every scenario starts from the same report: two features, each with its table pages —
            features/null-values, parsing/lists and parsing/maps. Published pages? lists the pages
            that survive, feature pages included; the root index always publishes and is not listed.
            """)
    @TableTest("""
        Scenario                                 | Exclude                       | Include         | Published pages?
        Nothing selected publishes everything    | []                            | []              | [features, features/null-values, parsing, parsing/lists, parsing/maps]
        Excluded table drops its page            | [parsing/maps]                | []              | [features, features/null-values, parsing, parsing/lists]
        Excluded feature drops its whole subtree | [parsing]                     | []              | [features, features/null-values]
        Feature emptied table by table goes too  | [parsing/lists, parsing/maps] | []              | [features, features/null-values]
        Include re-admits one excluded table     | [parsing]                     | [parsing/lists] | [features, features/null-values, parsing, parsing/lists]
        Include wins on the same page            | [parsing/maps]                | [parsing/maps]  | [features, features/null-values, parsing, parsing/lists, parsing/maps]
        Wildcard matches part of a page name     | [parsing/ma*]                 | []              | [features, features/null-values, parsing, parsing/lists]
        Deep wildcard reaches a nested page      | [**/null-values]              | []              | [parsing, parsing/lists, parsing/maps]
        Pattern matching nothing changes nothing | [nosuch]                      | []              | [features, features/null-values, parsing, parsing/lists, parsing/maps]
        """)
    void publishesEveryPageNotExcluded(List<String> exclude, List<String> include, List<String> publishedPages) {
        ReportNode published = new PublishSelection(exclude, include).applyTo(sampleReport());

        assertThat(pagePaths(published)).isEqualTo(publishedPages);
    }

    @Test
    void emptySelectionReturnsTheSameTreeUntouched() {
        ReportNode report = sampleReport();

        assertThat(PublishSelection.EMPTY.applyTo(report)).isSameAs(report);
    }

    @Test
    void excludingEveryFeatureLeavesTheRootIndexAlone() {
        ReportNode published = new PublishSelection(List.of("**"), List.of()).applyTo(sampleReport());

        assertThat(published.name()).isEqualTo("junit");
        assertThat(pagePaths(published)).isEmpty();
    }

    // --- helpers ---

    /** The report every scenario starts from: features/null-values, parsing/lists, parsing/maps. */
    private static ReportNode sampleReport() {
        return new IndexNode(
                "junit", "", null, List.of(feature("features", "null-values"), feature("parsing", "lists", "maps")));
    }

    private static ReportNode feature(String name, String... tableNames) {
        List<ReportNode> tables = new ArrayList<>();
        for (String tableName : tableNames) {
            tables.add(new TableNode(tableName, "/" + name + "/" + tableName, tableResource(tableName)));
        }
        return new IndexNode(name, "/" + name, null, List.copyOf(tables));
    }

    private static Map<String, Object> tableResource(String tableName) {
        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("title", tableName);
        return resource;
    }

    /** Every page path below the root, parent before child, as slash-separated page names. */
    private static List<String> pagePaths(ReportNode root) {
        return childPaths(root, "");
    }

    private static List<String> childPaths(ReportNode node, String prefix) {
        if (!(node instanceof IndexNode index)) {
            return List.of();
        }
        return index.contents().stream()
                .flatMap(child -> {
                    String path = prefix + child.name();
                    return java.util.stream.Stream.concat(
                            java.util.stream.Stream.of(path), childPaths(child, path + "/").stream());
                })
                .toList();
    }
}
