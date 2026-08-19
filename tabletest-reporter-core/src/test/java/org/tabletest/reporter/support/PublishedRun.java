package org.tabletest.reporter.support;

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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

/**
 * Writes the run output a set of published tables would leave behind, so a rule can state its
 * input as the test classes that ran rather than as YAML. Each published table is written as
 * {@code fully.qualified.ClassName#tableMethod}.
 *
 * <p>The output is shaped by the JUnit extension's own code — {@link Slugger} names each page,
 * and {@code TestClassData}/{@code TableMetadata} render the files — so nothing about the output
 * format is restated here. {@code ReportStructureFidelityTest} pins it against a real run.
 *
 * <p>Public because the rules built on it live in more than one test package.
 */
public final class PublishedRun {

    private static final String FILENAME_PREFIX = "TABLETEST-";
    private static final String YAML_EXTENSION = ".yaml";

    private static final Dump YAML =
            new Dump(DumpSettings.builder().setDefaultFlowStyle(FlowStyle.BLOCK).build());

    private PublishedRun() {}

    /**
     * A fresh directory under {@code workingDir} holding the output the given published tables
     * would leave behind, ready to report on.
     */
    public static Path outputFor(List<String> publishedTables, Path workingDir) {
        Path runDir = createTempDirectory(workingDir);
        tablesByClassName(publishedTables).forEach((className, methodNames) -> write(runDir, className, methodNames));
        return runDir;
    }

    private static Map<String, List<String>> tablesByClassName(List<String> publishedTables) {
        return publishedTables.stream()
                .collect(groupingBy(
                        PublishedRun::classNameOf, LinkedHashMap::new, mapping(PublishedRun::methodNameOf, toList())));
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
    private static void write(Path runDir, String className, List<String> methodNames) {
        Path classDir = createDirectory(runDir, className);
        List<PublishedTableTest> tables = methodNames.stream()
                .map(methodName -> new PublishedTableTest(
                        fileNameOf(Slugger.slugify(methodName)), null, methodName, Slugger.slugify(methodName)))
                .toList();

        tables.forEach(table -> writeString(classDir.resolve(table.path()), tableYaml(table)));

        String classSlug = Slugger.slugify(simpleNameOf(className));
        writeString(
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

    private static Path createTempDirectory(Path parent) {
        try {
            return Files.createTempDirectory(parent, "run");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path createDirectory(Path parent, String name) {
        try {
            return Files.createDirectories(parent.resolve(name));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeString(Path file, String content) {
        try {
            Files.writeString(file, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
