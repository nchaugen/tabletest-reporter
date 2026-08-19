package org.tabletest.reporter;

import org.tabletest.reporter.support.PublishedRun;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Renders the tree of pages the reporter builds for a set of published tables as one indented
 * line per page.
 *
 * <p>Structure rules that assert a {@code ReportNode} tree by hand have to spell out the node
 * types, the resource maps and the output paths, which buries the one thing the rule states —
 * the shape. Here a rule states its input as the test classes that ran (see {@link PublishedRun})
 * and its outcome as the page tree a reader would see in the sidebar.
 */
public final class ReportStructure {

    /** The name shown for the root page when the published classes share no package. */
    public static final String UNNAMED_ROOT = "(root)";

    private ReportStructure() {}

    /**
     * The report pages for the given published tables, outermost first, indented two spaces per
     * level. Run output is written into a fresh directory under {@code workingDir}.
     */
    public static List<String> pagesFor(List<String> publishedTables, Path workingDir) {
        return pagesOf(ReportTree.process(PublishedRun.outputFor(publishedTables, workingDir)));
    }

    /** The pages of a report tree, outermost first, indented two spaces per level. */
    public static List<String> pagesOf(ReportNode tree) {
        return tree == null ? List.of() : pageLines(tree, 0).toList();
    }

    private static Stream<String> pageLines(ReportNode node, int level) {
        Stream<String> page = Stream.of("  ".repeat(level) + pageNameOf(node));
        return node instanceof IndexNode index
                ? Stream.concat(page, index.contents().stream().flatMap(child -> pageLines(child, level + 1)))
                : page;
    }

    private static String pageNameOf(ReportNode node) {
        return node.name() != null ? node.name() : UNNAMED_ROOT;
    }
}
