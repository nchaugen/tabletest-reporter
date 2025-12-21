package io.github.nchaugen.tabletest.reporter;

import io.github.nchaugen.tabletest.junit.Scenario;
import io.github.nchaugen.tabletest.junit.TableTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportTreeTest {

    @TableTest("""
        Scenario         | Directories           | Files                                                                                                                | Resources?
        No yaml files    | [a.a/c, a.a/d, a.b/c] | []                                                                                                                   | []
        Only leaf files  | [a.a/c, a.a/d, a.b/c] | [a.a/c/TABLETEST-x.yaml, a.a/d/TABLETEST-y.yaml, a.b/c/TABLETEST-x.yaml]                                             | [a.a/c/TABLETEST-x.yaml, a.a/d/TABLETEST-y.yaml, a.b/c/TABLETEST-x.yaml]
        Files in all     | [a.a/c, a.a/d, a.b/c] | [a.a/TABLETEST-A.yaml, a.a/c/TABLETEST-x.yaml, a.a/d/TABLETEST-y.yaml, a.b/TABLETEST-B.yaml, a.b/c/TABLETEST-x.yaml] | [a.a/TABLETEST-A.yaml, a.a/c/TABLETEST-x.yaml, a.a/d/TABLETEST-y.yaml, a.b/TABLETEST-B.yaml, a.b/c/TABLETEST-x.yaml]
        Non-yaml ignored | [a.a/c, a.a/d, a.b/c] | [a.a/TABLETEST-z.txt, a.a/c/TABLETEST-x.xml, a.a/d/TABLETEST-y.gif, a.b/TABLETEST-w.md, a.b/c/TABLETEST-x.adoc]      | []
        """)
    void shouldFindTableTestOutputFiles(
        @Scenario String scenario,
        List<String> directories,
        List<String> files,
        List<Path> expected,
        @TempDir Path tempDir
    ) {
        directories.forEach(dir -> createSubDir(tempDir, dir));
        files.forEach(file -> createFile(tempDir, file));

        assertThat(ReportTree.findTableTestOutputFiles(tempDir)).containsExactlyElementsOf(expected);
    }

    private static void createFile(Path tempDir, String file) {
        try {
            Files.createFile(tempDir.resolve(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createSubDir(Path tempDir, String dir) {
        try {
            Files.createDirectories(tempDir.resolve(dir));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @TableTest("""
        Scenario           | Yaml files                                                    | Targets?
        Nothing there      | []                                                            | [.: ""]
        Only leaf files    | [/a/b/x.yaml, /a/c/y.yaml, /d/e/x.yaml]                       | [.: "", ./a: "", ./a/b: /a/b/x.yaml, ./a/c: /a/c/y.yaml, ./d: "", ./d/e: /d/e/x.yaml]
        Files in all       | [/a/A.yaml, /a/b/x.yaml, /a/c/y.yaml, /d/D.yaml, /d/e/x.yaml] | [.: "", ./a: /a/A.yaml, ./a/b: /a/b/x.yaml, ./a/c: /a/c/y.yaml, ./d: /d/D.yaml, ./d/e: /d/e/x.yaml]
        Remove common root | [/1/2/a/b/x.yaml, /1/2/a/c/y.yaml, /1/2/d/e/x.yaml]           | [2: "", ./a: "", ./a/b: /1/2/a/b/x.yaml, ./a/c: /1/2/a/c/y.yaml, ./d: "", ./d/e: /1/2/d/e/x.yaml]
        Split by package   | [/1.2.a/b/x.yaml, /1.2.a/c/y.yaml, /1.2.d/e/x.yaml]           | [2: "", ./a: "", ./a/b: /1.2.a/b/x.yaml, ./a/c: /1.2.a/c/y.yaml, ./d: "", ./d/e: /1.2.d/e/x.yaml]
        """)
    void shouldFindTargets(List<Path> yamlFiles, List<ReportTree.Target> expected) {
        assertThat(ReportTree.findTargets(yamlFiles)).containsExactlyElementsOf(expected);
    }

    @SuppressWarnings("unused")
    public static List<ReportTree.Target> toTargetToSources(Map<String, String> map) {
        List<ReportTree.Target> list = map.entrySet().stream()
            .map(e -> "".equals(e.getValue())
                ? ReportTree.Target.withPath(e.getKey())
                : ReportTree.Target.withPath(e.getKey()).withResource(Path.of(e.getValue()))
            ).sorted(Comparator.comparing(target -> target.path().getNameCount())).toList();
        String name = list.getFirst().name();
        return Stream.concat(
            Stream.of(ReportTree.Target.withPath(".").withName(".".equals(name) ? null : name)),
            list.stream().skip(1)
        ).sorted(Comparator.comparing(ReportTree.Target::path)).toList();
    }

    @Test
    void shouldCreateReportTree(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("pkg.T1/table1"));
        Files.createDirectories(tempDir.resolve("pkg.T1/table2"));
        Files.createDirectories(tempDir.resolve("pkg.T2/table1"));

        Files.createFile(tempDir.resolve("pkg.T1/TABLETEST-T1Test.yaml"));
        Files.createFile(tempDir.resolve("pkg.T1/table1/TABLETEST-tabletest1.yaml"));
        Files.createFile(tempDir.resolve("pkg.T1/table2/TABLETEST-tabletest2.yaml"));
        Files.createFile(tempDir.resolve("pkg.T2/TABLETEST-T2Test.yaml"));
        Files.createFile(tempDir.resolve("pkg.T2/table1/TABLETEST-tabletest1.yaml"));

        ReportNode tree = ReportTree.process(tempDir);

        ReportNode expected = new IndexNode(
            "pkg",
            "",
            null,
            List.of(
                new IndexNode(
                    "T1Test",
                    "/t1test",
                    "pkg.T1/TABLETEST-T1Test.yaml",
                    List.of(
                        new TableNode(
                            "tabletest1",
                            "/t1test/tabletest1",
                            "pkg.T1/table1/TABLETEST-tabletest1.yaml"
                        ),
                        new TableNode(
                            "tabletest2",
                            "/t1test/tabletest2",
                            "pkg.T1/table2/TABLETEST-tabletest2.yaml"
                        )
                    )
                ),
                new IndexNode(
                    "T2Test",
                    "/t2test",
                    "pkg.T2/TABLETEST-T2Test.yaml",
                    List.of(
                        new TableNode(
                            "tabletest1",
                            "/t2test/tabletest1",
                            "pkg.T2/table1/TABLETEST-tabletest1.yaml"
                        )
                    )
                )
            )
        );

        assertThat(tree)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expected);
    }

    @Test
    void shouldSupportDeeperPackageStructure(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("com.pkg.products.T1/table1"));
        Files.createDirectories(tempDir.resolve("com.pkg.products.T1/table2"));
        Files.createDirectories(tempDir.resolve("com.pkg.orders.T2/table1"));

        Files.createFile(tempDir.resolve("com.pkg.products.T1/TABLETEST-T1Test.yaml"));
        Files.createFile(tempDir.resolve("com.pkg.products.T1/table1/TABLETEST-tabletest1.yaml"));
        Files.createFile(tempDir.resolve("com.pkg.products.T1/table2/TABLETEST-tabletest2.yaml"));
        Files.createFile(tempDir.resolve("com.pkg.orders.T2/TABLETEST-T2Test.yaml"));
        Files.createFile(tempDir.resolve("com.pkg.orders.T2/table1/TABLETEST-tabletest1.yaml"));

        ReportNode tree = ReportTree.process(tempDir);

        ReportNode expected = new IndexNode(
            "pkg",
            "",
            null,
            List.of(
                new IndexNode(
                    "products",
                    "/products",
                    null,
                    List.of(
                        new IndexNode(
                            "T1Test",
                            "/products/t1test",
                            "com.pkg.products.T1/TABLETEST-T1Test.yaml",
                            List.of(
                                new TableNode(
                                    "tabletest1",
                                    "/products/t1test/tabletest1",
                                    "com.pkg.products.T1/table1/TABLETEST-tabletest1.yaml"
                                ),
                                new TableNode(
                                    "tabletest2",
                                    "/products/t1test/tabletest2",
                                    "com.pkg.products.T1/table2/TABLETEST-tabletest2.yaml"
                                )
                            )
                        )
                    )
                ),
                new IndexNode(
                    "orders",
                    "/orders",
                    null,
                    List.of(
                        new IndexNode(
                            "T2Test",
                            "/orders/t2test",
                            "com.pkg.orders.T2/TABLETEST-T2Test.yaml",
                            List.of(
                                new TableNode(
                                    "tabletest1",
                                    "/orders/t2test/tabletest1",
                                    "com.pkg.orders.T2/table1/TABLETEST-tabletest1.yaml"
                                )
                            )
                        )
                    )
                )
            )
        );

        assertThat(tree)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expected);
    }

    @Test
    void shouldSupportMultiplePackageDomains(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("no.pkg.products.T1/table1"));
        Files.createDirectories(tempDir.resolve("no.pkg.orders.T1/table2"));
        Files.createDirectories(tempDir.resolve("no.oth.orders.T2/table1"));
        Files.createDirectories(tempDir.resolve("no.pkg.packages.T1/table2"));

        Files.createFile(tempDir.resolve("no.pkg.products.T1/TABLETEST-T1Test.yaml"));
        Files.createFile(tempDir.resolve("no.pkg.products.T1/table1/TABLETEST-tabletest1.yaml"));
        Files.createFile(tempDir.resolve("no.pkg.packages.T1/table2/TABLETEST-tabletest2.yaml"));
        Files.createFile(tempDir.resolve("no.oth.orders.T2/TABLETEST-T2Test.yaml"));
        Files.createFile(tempDir.resolve("no.oth.orders.T2/table1/TABLETEST-tabletest1.yaml"));

        ReportNode tree = ReportTree.process(tempDir);

        ReportNode expected = new IndexNode(
            "no",
            "",
            null,
            List.of(
                new IndexNode(
                    "pkg",
                    "/pkg",
                    null,
                    List.of(
                        new IndexNode(
                            "products",
                            "/pkg/products",
                            null,
                            List.of(
                                new IndexNode(
                                    "T1Test",
                                    "/pkg/products/t1test",
                                    "no.pkg.products.T1/TABLETEST-T1Test.yaml",
                                    List.of(
                                        new TableNode(
                                            "tabletest1",
                                            "/pkg/products/t1test/tabletest1",
                                            "no.pkg.products.T1/table1/TABLETEST-tabletest1.yaml"
                                        )
                                    )
                                )
                            )
                        ),
                        new IndexNode(
                            "packages",
                            "/pkg/packages",
                            null,
                            List.of(
                                new IndexNode(
                                    "T1",
                                    "/pkg/packages/t1",
                                    null,
                                    List.of(
                                        new TableNode(
                                            "tabletest2",
                                            "/pkg/packages/t1/tabletest2",
                                            "no.pkg.packages.T1/table2/TABLETEST-tabletest2.yaml"
                                        )
                                    )
                                )
                            )
                        )
                    )
                ),
                new IndexNode(
                    "oth",
                    "/oth",
                    null,
                    List.of(
                        new IndexNode(
                            "orders",
                            "/oth/orders",
                            null,
                            List.of(
                                new IndexNode(
                                    "T2Test",
                                    "/oth/orders/t2test",
                                    "no.oth.orders.T2/TABLETEST-T2Test.yaml",
                                    List.of(
                                        new TableNode(
                                            "tabletest1",
                                            "/oth/orders/t2test/tabletest1",
                                            "no.oth.orders.T2/table1/TABLETEST-tabletest1.yaml"
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        );

        assertThat(tree)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expected);
    }

    @Test
    void shouldSupportMissingTestClassYaml(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("pkg.T1/table1"));
        Files.createDirectories(tempDir.resolve("pkg.T1/table2"));
        Files.createDirectories(tempDir.resolve("pkg.T2/table1"));

        Files.createFile(tempDir.resolve("pkg.T1/table1/TABLETEST-tabletest1.yaml"));
        Files.createFile(tempDir.resolve("pkg.T1/table2/TABLETEST-tabletest2.yaml"));
        Files.createFile(tempDir.resolve("pkg.T2/table1/TABLETEST-tabletest1.yaml"));

        ReportNode tree = ReportTree.process(tempDir);

        ReportNode expected = new IndexNode(
            "pkg",
            "",
            null,
            List.of(
                new IndexNode(
                    "T1",
                    "/t1",
                    null,
                    List.of(
                        new TableNode(
                            "tabletest1",
                            "/t1/tabletest1",
                            "pkg.T1/table1/TABLETEST-tabletest1.yaml"
                        ),
                        new TableNode(
                            "tabletest2",
                            "/t1/tabletest2",
                            "pkg.T1/table2/TABLETEST-tabletest2.yaml"
                        )
                    )
                ),
                new IndexNode(
                    "T2",
                    "/t2",
                    null,
                    List.of(
                        new TableNode(
                            "tabletest1",
                            "/t2/tabletest1",
                            "pkg.T2/table1/TABLETEST-tabletest1.yaml"
                        )
                    )
                )
            )
        );

        assertThat(tree)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expected);
    }

    @Test
    void shouldSlugifyOutputPaths(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("org.example.FirstTest/table_test(java.util.List, org.example.Domain)"));
        Files.createDirectories(tempDir.resolve("org.example.FirstTest/another_test(java.time.LocalDate, boolean)"));
        Files.createDirectories(tempDir.resolve("org.example.SecondTest/table test(java.lang.String, java.lang.Class)"));

        Files.createFile(tempDir.resolve("org.example.FirstTest/TABLETEST-first-test.yaml"));
        Files.createFile(tempDir.resolve("org.example.FirstTest/table_test(java.util.List, org.example.Domain)/TABLETEST-leap-year-rules.yaml"));
        Files.createFile(tempDir.resolve("org.example.FirstTest/another_test(java.time.LocalDate, boolean)/TABLETEST-another-test.yaml"));
        Files.createFile(tempDir.resolve("org.example.SecondTest/TABLETEST-a-custom-test-title.yaml"));
        Files.createFile(tempDir.resolve("org.example.SecondTest/table test(java.lang.String, java.lang.Class)/TABLETEST-table-test.yaml"));

        ReportNode tree = ReportTree.process(tempDir);

        ReportNode expected = new IndexNode(
            "example",
            "",
            null,
            List.of(
                new IndexNode(
                    "first-test",
                    "/first-test",
                    "org.example.FirstTest/TABLETEST-first-test.yaml",
                    List.of(
                        new TableNode(
                            "leap-year-rules",
                            "/first-test/leap-year-rules",
                            "org.example.FirstTest/table_test(java.util.List, org.example.Domain)/TABLETEST-leap-year-rules.yaml"
                        ),
                        new TableNode(
                            "another-test",
                            "/first-test/another-test",
                            "org.example.FirstTest/another_test(java.time.LocalDate, boolean)/TABLETEST-another-test.yaml"
                        )
                    )
                ),
                new IndexNode(
                    "a-custom-test-title",
                    "/a-custom-test-title",
                    "org.example.SecondTest/TABLETEST-a-custom-test-title.yaml",
                    List.of(
                        new TableNode(
                            "table-test",
                            "/a-custom-test-title/table-test",
                            "org.example.SecondTest/table test(java.lang.String, java.lang.Class)/TABLETEST-table-test.yaml"
                        )
                    )
                )
            )
        );

        assertThat(tree)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expected);
    }

    @Test
    void shouldSupportNestedTestClasses(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("pkg.T1/table1"));
        Files.createDirectories(tempDir.resolve("pkg.T1/Nested/table2"));
        Files.createDirectories(tempDir.resolve("pkg.T1/Nested/DeeplyNested/table1"));

        Files.createFile(tempDir.resolve("pkg.T1/TABLETEST-T1.yaml"));
        Files.createFile(tempDir.resolve("pkg.T1/Nested/TABLETEST-nested.yaml"));
        Files.createFile(tempDir.resolve("pkg.T1/Nested/DeeplyNested/TABLETEST-deeply-nested.yaml"));
        Files.createFile(tempDir.resolve("pkg.T1/table1/TABLETEST-tabletest1.yaml"));
        Files.createFile(tempDir.resolve("pkg.T1/Nested/table2/TABLETEST-tabletest2.yaml"));
        Files.createFile(tempDir.resolve("pkg.T1/Nested/DeeplyNested/table1/TABLETEST-tabletest1.yaml"));

        ReportNode tree = ReportTree.process(tempDir);

        ReportNode expected = new IndexNode(
            "pkg",
            "",
            null,
            List.of(
                new IndexNode(
                    "T1",
                    "/t1",
                    "pkg.T1/TABLETEST-T1.yaml",
                    List.of(
                        new TableNode(
                            "tabletest1",
                            "/t1/tabletest1",
                            "pkg.T1/table1/TABLETEST-tabletest1.yaml"
                        ),
                        new IndexNode(
                            "nested",
                            "/t1/nested",
                            "pkg.T1/Nested/TABLETEST-nested.yaml",
                            List.of(
                                new TableNode(
                                    "tabletest2",
                                    "/t1/nested/tabletest2",
                                    "pkg.T1/Nested/table2/TABLETEST-tabletest2.yaml"
                                ),
                                new IndexNode(
                                    "deeply-nested",
                                    "/t1/nested/deeply-nested",
                                    "pkg.T1/Nested/DeeplyNested/TABLETEST-deeply-nested.yaml",
                                    List.of(
                                        new TableNode(
                                            "tabletest1",
                                            "/t1/nested/deeply-nested/tabletest1",
                                            "pkg.T1/Nested/DeeplyNested/table1/TABLETEST-tabletest1.yaml"
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        );

        assertThat(tree)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(expected);
    }

}
