package org.tabletest.reporter;

import org.tabletest.reporter.support.PublishedRun;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the tree of pages the reporter makes for a set of published tables.
 *
 * <p>Structure rules that assert a {@code ReportNode} tree by hand have to spell out the node
 * types, the resource maps and the output paths. That buries the one thing the rule states, which
 * is the shape. Here a rule states its input as the test classes that ran (see {@link
 * PublishedRun}) and its outcome as the page tree a reader sees in the sidebar.
 *
 * <p>The tree is a map of one entry: the root page, and the pages it holds. A page that holds
 * other pages is an entry of its own. A page that holds none is a plain name.
 */
public final class ReportStructure {

    /** The name shown for the root page when the published classes share no package. */
    public static final String UNNAMED_ROOT = "(root)";

    private ReportStructure() {}

    /**
     * The page tree for the given published tables. Run output is written into a fresh directory
     * under {@code workingDir}.
     */
    public static Map<String, Object> pageTreeFor(List<String> publishedTables, Path workingDir) {
        return pageTreeOf(ReportTree.process(PublishedRun.outputFor(publishedTables, workingDir)));
    }

    /** The page tree of a report tree. */
    public static Map<String, Object> pageTreeOf(ReportNode tree) {
        return tree == null ? Map.of() : entryFor(tree);
    }

    private static Map<String, Object> entryFor(ReportNode node) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put(pageNameOf(node), contentsOf(node));
        return entry;
    }

    private static List<Object> contentsOf(ReportNode node) {
        return node instanceof IndexNode index
                ? index.contents().stream().map(ReportStructure::pageOf).toList()
                : List.of();
    }

    private static Object pageOf(ReportNode node) {
        return node instanceof IndexNode ? entryFor(node) : pageNameOf(node);
    }

    private static String pageNameOf(ReportNode node) {
        return node.name() != null ? node.name() : UNNAMED_ROOT;
    }
}
