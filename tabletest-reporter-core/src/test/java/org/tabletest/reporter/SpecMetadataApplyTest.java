package org.tabletest.reporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.tabletest.junit.TableTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Unpublished: internal report-tree transform, not a user-facing rule.
@Tag("unpublished")
class SpecMetadataApplyTest {

    @TableTest("""
        Scenario                   | Existing  | Declared  | Order?
        No features keeps order    | [a, b, c] | []        | [a, b, c]
        Declared come first        | [a, b, c] | [c, a]    | [c, a, b]
        Full reorder               | [b, a]    | [a, b]    | [a, b]
        Unmatched declared ignored | [a, b, c] | [x]       | [a, b, c]
        Partial with unmatched     | [a, b, c] | [c, x, a] | [c, a, b]
        """)
    void reordersTopLevelFeaturesDeclaredFirstThenAlphabetical(
            List<String> existing, List<String> declared, List<String> order) {
        SpecMetadata metadata = new SpecMetadata(null, null, features(declared));

        ReportNode applied = metadata.applyTo(indexWithChildren("junit", existing));

        assertThat(childNames(applied)).isEqualTo(order);
    }

    @Test
    void writesTitleAndIntroOntoRootIndex() {
        ReportNode root = indexWithChildren("junit", List.of("a"));

        ReportNode applied = new SpecMetadata("Core Spec", "The intro paragraph.", List.of()).applyTo(root);

        assertThat(applied.resource())
                .containsEntry("title", "Core Spec")
                .containsEntry("description", "The intro paragraph.");
    }

    @Test
    void retitlesMatchedFeatureAndRecursesIntoItsChildren() {
        ReportNode root = new IndexNode(
                "junit",
                "",
                null,
                List.of(
                        new IndexNode(
                                "formatter", "/formatter", null, List.of(leaf("displaywidth"), leaf("extraction"))),
                        leaf("examples")));
        SpecMetadata metadata = new SpecMetadata(
                "Spec",
                null,
                List.of(
                        new FeatureMetadata(
                                "formatter",
                                "Table Formatter",
                                List.of(
                                        new FeatureMetadata("extraction", "Value Extraction", List.of()),
                                        new FeatureMetadata("displaywidth", "Display Width", List.of()))),
                        new FeatureMetadata("examples", "Worked Examples", List.of())));

        ReportNode applied = metadata.applyTo(root);

        ReportNode formatter = childNamed(applied, "formatter");
        assertThat(formatter.resource()).containsEntry("title", "Table Formatter");
        assertThat(childNames(formatter)).containsExactly("extraction", "displaywidth");
        assertThat(childNamed(applied, "examples").resource()).containsEntry("title", "Worked Examples");
    }

    @Test
    void preservesExistingResourceKeysWhenRetitling() {
        ReportNode root = new IndexNode(
                "junit",
                "",
                null,
                List.of(new IndexNode("formatter", "/formatter", Map.of("status", "passing"), List.of())));
        SpecMetadata metadata =
                new SpecMetadata("Spec", null, List.of(new FeatureMetadata("formatter", "Table Formatter", List.of())));

        ReportNode applied = metadata.applyTo(root);

        assertThat(childNamed(applied, "formatter").resource())
                .containsEntry("status", "passing")
                .containsEntry("title", "Table Formatter");
    }

    @Test
    void emptyMetadataReturnsTheSameTreeUntouched() {
        ReportNode root = indexWithChildren("junit", List.of("b", "a"));

        assertThat(SpecMetadata.EMPTY.applyTo(root)).isSameAs(root);
    }

    // --- helpers ---

    private static List<FeatureMetadata> features(List<String> names) {
        return names.stream()
                .map(name -> new FeatureMetadata(name, null, List.of()))
                .toList();
    }

    private static ReportNode indexWithChildren(String name, List<String> childNames) {
        return new IndexNode(
                name,
                "",
                null,
                childNames.stream().map(SpecMetadataApplyTest::leaf).toList());
    }

    private static ReportNode leaf(String name) {
        return new IndexNode(name, "/" + name, null, List.of());
    }

    private static List<String> childNames(ReportNode node) {
        return ((IndexNode) node).contents().stream().map(ReportNode::name).toList();
    }

    private static ReportNode childNamed(ReportNode node, String name) {
        return ((IndexNode) node)
                .contents().stream()
                        .filter(child -> name.equals(child.name()))
                        .findFirst()
                        .orElseThrow();
    }
}
