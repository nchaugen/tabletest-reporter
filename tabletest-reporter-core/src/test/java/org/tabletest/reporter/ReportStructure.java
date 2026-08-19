package org.tabletest.reporter;

import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.tabletest.parser.TableParser;
import org.tabletest.reporter.junit.PublishedTableTest;
import org.tabletest.reporter.junit.Slugger;
import org.tabletest.reporter.junit.TableMetadata;
import org.tabletest.reporter.junit.TestClassData;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

/**
 * Turns a list of published tables, each written as {@code fully.qualified.ClassName#tableMethod},
 * into the tree of pages the reporter builds for them, rendered as one indented line per page.
 *
 * <p>Structure rules that assert a {@code ReportNode} tree by hand have to spell out the node
 * types, the resource maps and the output paths, which buries the one thing the rule states —
 * the shape. Here a rule states its input as the test classes that ran and its outcome as the
 * page tree a reader would see in the sidebar. The published tables are turned into run output
 * the way the JUnit extension turns them: the slug of a page comes from {@link Slugger}, and
 * the file contents from the extension's own {@code TestClassData}/{@code TableMetadata}, so
 * nothing about the output format is restated here. {@code ReportStructureFidelityTest} pins
 * the helper against the output of a real run.
 */
final class ReportStructure {

    private static final String FILENAME_PREFIX = "TABLETEST-";
    private static final String YAML_EXTENSION = ".yaml";

    /** The name shown for the root page when the published classes share no package. */
    static final String UNNAMED_ROOT = "(root)";

    private static final Dump YAML =
            new Dump(DumpSettings.builder().setDefaultFlowStyle(FlowStyle.BLOCK).build());

    private ReportStructure() {}

    /**
     * The report pages for the given published tables, outermost first, indented two spaces per
     * level. Run output is written into a fresh directory under {@code workingDir}.
     */
    static List<String> pagesFor(List<String> publishedTables, Path workingDir) {
        Path runDir = createDirectory(workingDir);
        tablesByClassName(publishedTables)
                .forEach((className, methodNames) -> writeRunOutput(runDir, className, methodNames));
        return pagesOf(ReportTree.process(runDir));
    }

    private static Map<String, List<String>> tablesByClassName(List<String> publishedTables) {
        return publishedTables.stream()
                .collect(groupingBy(
                        ReportStructure::classNameOf,
                        LinkedHashMap::new,
                        mapping(ReportStructure::methodNameOf, toList())));
    }

    private static String classNameOf(String publishedTable) {
        return publishedTable.substring(0, publishedTable.indexOf('#'));
    }

    private static String methodNameOf(String publishedTable) {
        return publishedTable.substring(publishedTable.indexOf('#') + 1);
    }

    /**
     * Writes what one test class publishes: a YAML file per table, and the class file listing
     * them. Each class gets its own directory, as it does in a real run.
     */
    private static void writeRunOutput(Path runDir, String className, List<String> methodNames) {
        Path classDir = createDirectory(runDir, className);
        List<PublishedTableTest> tables = methodNames.stream()
                .map(methodName -> new PublishedTableTest(
                        fileNameOf(Slugger.slugify(methodName)), null, methodName, Slugger.slugify(methodName)))
                .toList();

        tables.forEach(table -> write(classDir.resolve(table.path()), tableYaml(table)));

        String classSlug = Slugger.slugify(simpleNameOf(className));
        write(
                classDir.resolve(fileNameOf(classSlug)),
                YAML.dumpToString(new TestClassData(className, classSlug, null, null, tables).toMap()));
    }

    private static String tableYaml(PublishedTableTest table) {
        return YAML.dumpToString(new TableMetadata()
                .withMethodName(table.methodName())
                .withSlug(table.slug())
                .toTableTestData(TableParser.parse("Value\nany\n"))
                .toMap());
    }

    /** The simple name of a class, which is what a nested class is named after in the report. */
    private static String simpleNameOf(String className) {
        return className.substring(Math.max(className.lastIndexOf('.'), className.lastIndexOf('$')) + 1);
    }

    private static String fileNameOf(String slug) {
        return FILENAME_PREFIX + slug + YAML_EXTENSION;
    }

    /** The pages of a report tree, outermost first, indented two spaces per level. */
    static List<String> pagesOf(ReportNode tree) {
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

    private static Path createDirectory(Path parent, String... segments) {
        try {
            return segments.length == 0
                    ? Files.createTempDirectory(parent, "run")
                    : Files.createDirectories(Path.of(parent.toString(), segments));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(Path file, String content) {
        try {
            Files.writeString(file, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
