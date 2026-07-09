package org.tabletest.reporter;

import org.tabletest.junit.Scenario;
import org.tabletest.junit.TableTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests relative-href computation between report pages. Every href must be relative to the
 * linking page's own directory (never root-absolute) so the tree serves from any subpath.
 */
class NavLinksTest {

    @TableTest("""
        Scenario                    | From                 | From type | Target               | Target type | Href?
        Root index to child index   | ''                   | index     | boolean-logic        | index       | boolean-logic/index.html
        Child index up to root      | boolean-logic        | index     | ''                   | index       | ../index.html
        Index down to its table     | boolean-logic        | index     | boolean-logic/and-op | table       | and-op.html
        Table up to root index      | boolean-logic/and-op | table     | ''                   | index       | ../index.html
        Table to its own index      | boolean-logic/and-op | table     | boolean-logic        | index       | index.html
        Table to sibling table      | boolean-logic/and-op | table     | boolean-logic/or-op  | table       | or-op.html
        """)
    void computes_href_relative_to_the_linking_page(
            @Scenario String scenario, String from, String fromType, String target, String targetType, String href) {
        ReportNode fromNode = node(fromType, from);
        ReportNode targetNode = node(targetType, target);

        assertThat(NavLinks.href(NavLinks.pageDirectory(fromNode), targetNode)).isEqualTo(href);
    }

    private static ReportNode node(String type, String outPath) {
        if ("index".equals(type)) {
            return new IndexNode(outPath.isEmpty() ? null : outPath, outPath, null, List.of());
        }
        return new TableNode(lastSegment(outPath), outPath, Map.of());
    }

    private static String lastSegment(String outPath) {
        int slash = outPath.lastIndexOf('/');
        return slash >= 0 ? outPath.substring(slash + 1) : outPath;
    }
}
