package org.tabletest.reporter;

import org.junit.jupiter.api.DisplayName;
import org.tabletest.junit.Description;
import org.tabletest.junit.Scenario;
import org.tabletest.junit.TableTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests relative-href computation between report pages. Every href must be relative to the
 * linking page's own directory (never root-absolute) so the tree serves from any subpath.
 */
@DisplayName("Relative links")
@Description("""
        Every link in a report is relative, never root-absolute. A generated report therefore
        works from any directory: a local folder, a web server root, or a subpath such as GitHub
        project Pages.

        The example pages are a feature "boolean-logic" holding the tables "and-op" and "or-op".
        """)
class NavLinksTest {

    @DisplayName("Writes every link relative to the linking page's own directory")
    @TableTest("""
        Scenario                  | From                 | From type | Target               | Target type | Href?
        Root index to child index | ''                   | index     | boolean-logic        | index       | boolean-logic/index.html
        Child index up to root    | boolean-logic        | index     | ''                   | index       | ../index.html
        Index down to its table   | boolean-logic        | index     | boolean-logic/and-op | table       | and-op.html
        Table up to root index    | boolean-logic/and-op | table     | ''                   | index       | ../index.html
        Table to its own index    | boolean-logic/and-op | table     | boolean-logic        | index       | index.html
        Table to sibling table    | boolean-logic/and-op | table     | boolean-logic/or-op  | table       | or-op.html
        """)
    void computes_href_relative_to_the_linking_page(
            @Scenario String scenario, String from, String fromType, String target, String targetType, String href) {
        ReportNode fromNode = node(fromType, from);
        ReportNode targetNode = node(targetType, target);

        assertThat(NavLinks.href(NavLinks.pageDirectory(fromNode), targetNode)).isEqualTo(href);
    }

    @DisplayName("Gives every page a single root-relative path")
    @TableTest("""
        Scenario          | Node                 | Node type | Root path?
        Root index        | ''                   | index     | index.html
        Child index       | boolean-logic        | index     | boolean-logic/index.html
        Table under index | boolean-logic/and-op | table     | boolean-logic/and-op.html
        """)
    void computes_the_root_relative_path_of_a_page(
            @Scenario String scenario, String node, String nodeType, String rootPath) {
        assertThat(NavLinks.rootPath(node(nodeType, node))).isEqualTo(rootPath);
    }

    @DisplayName("Reaches shared assets by climbing to the output root")
    @Description("""
            Stylesheets and the search index live once at the output root; each page
            references them through a ../ prefix matching its own depth.
            """)
    @TableTest("""
        Scenario          | Page                 | Page type | Asset prefix?
        Root index        | ''                   | index     | ''
        Child index       | boolean-logic        | index     | ../
        Table under index | boolean-logic/and-op | table     | ../
        Deeply nested     | a/b/c                | table     | ../../
        """)
    void computes_the_depth_prefix_from_a_page_to_the_output_root(
            @Scenario String scenario, String page, String pageType, String assetPrefix) {
        ReportNode root = new IndexNode(null, "", null, List.of());
        assertThat(NavLinks.rootPrefix(node(pageType, page), root)).isEqualTo(assetPrefix);
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
