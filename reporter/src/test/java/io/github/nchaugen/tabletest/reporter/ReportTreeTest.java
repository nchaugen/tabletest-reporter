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
        Scenario         | Directories           | Files                                                              | Resources?
        No yaml files    | [a.a/c, a.a/d, a.b/c] | []                                                                 | []
        Only leaf files  | [a.a/c, a.a/d, a.b/c] | [a.a/c/x.yaml, a.a/d/y.yaml, a.b/c/x.yaml]                         | [a.a/c/x.yaml, a.a/d/y.yaml, a.b/c/x.yaml]
        Files in all     | [a.a/c, a.a/d, a.b/c] | [a.a/A.yaml, a.a/c/x.yaml, a.a/d/y.yaml, a.b/B.yaml, a.b/c/x.yaml] | [a.a/A.yaml, a.a/c/x.yaml, a.a/d/y.yaml, a.b/B.yaml, a.b/c/x.yaml]
        Non-yaml ignored | [a.a/c, a.a/d, a.b/c] | [a.a/z.txt, a.a/c/x.xml, a.a/d/y.gif, a.b/w.md, a.b/c/x.adoc]      | []
        """)
    void shouldFindTableTestOutputFiles(@Scenario String scenario, List<String> directories, List<String> files, List<Path> expected, @TempDir Path tempDir) {
        directories.forEach(dir -> {
            try {
                Files.createDirectories(tempDir.resolve(dir));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        files.forEach(file -> {
            try {
                Files.createFile(tempDir.resolve(file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(ReportTree.findTableTestOutputFiles(tempDir)).containsExactlyElementsOf(expected);
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

        Files.createFile(tempDir.resolve("pkg.T1/T1Test.yaml"));
        Files.createFile(tempDir.resolve("pkg.T1/table1/tabletest1.yaml"));
        Files.createFile(tempDir.resolve("pkg.T1/table2/tabletest2.yaml"));
        Files.createFile(tempDir.resolve("pkg.T2/T2Test.yaml"));
        Files.createFile(tempDir.resolve("pkg.T2/table1/tabletest1.yaml"));

        Map<String, Object> tree = ReportTree.process(tempDir);

        assertThat(tree)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(new ContextLoader().fromYaml("""
                type: index
                name: pkg
                outPath: ""
                contents:
                  - type: index
                    name: T1Test
                    outPath: /t1test
                    resource: pkg.T1/T1Test.yaml
                    contents:
                      - type: table
                        name: tabletest1
                        outPath: /t1test/tabletest1
                        resource: pkg.T1/table1/tabletest1.yaml
                      - type: table
                        name: tabletest2
                        outPath: /t1test/tabletest2
                        resource: pkg.T1/table2/tabletest2.yaml
                  - type: index
                    name: T2Test
                    outPath: /t2test
                    resource: pkg.T2/T2Test.yaml
                    contents:
                      - type: table
                        name: tabletest1
                        outPath: /t2test/tabletest1
                        resource: pkg.T2/table1/tabletest1.yaml
                """)
            );
    }

    @Test
    void shouldSupportDeeperPackageStructure(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("com.pkg.products.T1/table1"));
        Files.createDirectories(tempDir.resolve("com.pkg.products.T1/table2"));
        Files.createDirectories(tempDir.resolve("com.pkg.orders.T2/table1"));

        Files.createFile(tempDir.resolve("com.pkg.products.T1/T1Test.yaml"));
        Files.createFile(tempDir.resolve("com.pkg.products.T1/table1/tabletest1.yaml"));
        Files.createFile(tempDir.resolve("com.pkg.products.T1/table2/tabletest2.yaml"));
        Files.createFile(tempDir.resolve("com.pkg.orders.T2/T2Test.yaml"));
        Files.createFile(tempDir.resolve("com.pkg.orders.T2/table1/tabletest1.yaml"));

        Map<String, Object> tree = ReportTree.process(tempDir);

        assertThat(tree)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(new ContextLoader().fromYaml("""
                type: index
                name: pkg
                outPath: ""
                contents:
                  - type: index
                    name: products
                    outPath: /products
                    contents:
                      - type: index
                        name: T1Test
                        outPath: /products/t1test
                        resource: com.pkg.products.T1/T1Test.yaml
                        contents:
                          - type: table
                            name: tabletest1
                            outPath: /products/t1test/tabletest1
                            resource: com.pkg.products.T1/table1/tabletest1.yaml
                          - type: table
                            name: tabletest2
                            outPath: /products/t1test/tabletest2
                            resource: com.pkg.products.T1/table2/tabletest2.yaml
                  - type: index
                    name: orders
                    outPath: /orders
                    contents:
                      - type: index
                        name: T2Test
                        outPath: /orders/t2test
                        resource: com.pkg.orders.T2/T2Test.yaml
                        contents:
                          - type: table
                            name: tabletest1
                            outPath: /orders/t2test/tabletest1
                            resource: com.pkg.orders.T2/table1/tabletest1.yaml
                """)
            );
    }

    @Test
    void shouldSupportMultiplePackageDomains(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("no.pkg.products.T1/table1"));
        Files.createDirectories(tempDir.resolve("no.pkg.orders.T1/table2"));
        Files.createDirectories(tempDir.resolve("no.oth.orders.T2/table1"));
        Files.createDirectories(tempDir.resolve("no.pkg.packages.T1/table2"));

        Files.createFile(tempDir.resolve("no.pkg.products.T1/T1Test.yaml"));
        Files.createFile(tempDir.resolve("no.pkg.products.T1/table1/tabletest1.yaml"));
        Files.createFile(tempDir.resolve("no.pkg.packages.T1/table2/tabletest2.yaml"));
        Files.createFile(tempDir.resolve("no.oth.orders.T2/T2Test.yaml"));
        Files.createFile(tempDir.resolve("no.oth.orders.T2/table1/tabletest1.yaml"));

        Map<String, Object> tree = ReportTree.process(tempDir);

        assertThat(tree)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(new ContextLoader().fromYaml("""
                type: index
                name: "no"
                outPath: ""
                contents:
                  - type: index
                    name: pkg
                    outPath: /pkg
                    contents:
                      - type: index
                        name: products
                        outPath: /pkg/products
                        contents:
                          - type: index
                            name: T1Test
                            outPath: /pkg/products/t1test
                            resource: no.pkg.products.T1/T1Test.yaml
                            contents:
                              - type: table
                                name: tabletest1
                                outPath: /pkg/products/t1test/tabletest1
                                resource: no.pkg.products.T1/table1/tabletest1.yaml
                      - type: index
                        name: packages
                        outPath: /pkg/packages
                        contents:
                          - type: index
                            name: T1
                            outPath: /pkg/packages/t1
                            contents:
                              - type: table
                                name: tabletest2
                                outPath: /pkg/packages/t1/tabletest2
                                resource: no.pkg.packages.T1/table2/tabletest2.yaml
                  - type: index
                    name: oth
                    outPath: /oth
                    contents:
                      - type: index
                        name: orders
                        outPath: /oth/orders
                        contents:
                          - type: index
                            name: T2Test
                            outPath: /oth/orders/t2test
                            resource: no.oth.orders.T2/T2Test.yaml
                            contents:
                              - type: table
                                name: tabletest1
                                outPath: /oth/orders/t2test/tabletest1
                                resource: no.oth.orders.T2/table1/tabletest1.yaml
                """)
            );
    }

    @Test
    void shouldSupportMissingTestClassYaml(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("pkg.T1/table1"));
        Files.createDirectories(tempDir.resolve("pkg.T1/table2"));
        Files.createDirectories(tempDir.resolve("pkg.T2/table1"));

        Files.createFile(tempDir.resolve("pkg.T1/table1/tabletest1.yaml"));
        Files.createFile(tempDir.resolve("pkg.T1/table2/tabletest2.yaml"));
        Files.createFile(tempDir.resolve("pkg.T2/table1/tabletest1.yaml"));

        Map<String, Object> tree = ReportTree.process(tempDir);

        assertThat(tree)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(new ContextLoader().fromYaml("""
                type: index
                name: pkg
                outPath: ""
                contents:
                  - type: index
                    name: T1
                    outPath: /t1
                    contents:
                      - type: table
                        name: tabletest1
                        outPath: /t1/tabletest1
                        resource: pkg.T1/table1/tabletest1.yaml
                      - type: table
                        name: tabletest2
                        outPath: /t1/tabletest2
                        resource: pkg.T1/table2/tabletest2.yaml
                  - type: index
                    name: T2
                    outPath: /t2
                    contents:
                      - type: table
                        name: tabletest1
                        outPath: /t2/tabletest1
                        resource: pkg.T2/table1/tabletest1.yaml
                """)
            );
    }

    @Test
    void shouldSlugifyOutputPaths(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("org.example.FirstTest/table_test(java.util.List, org.example.Domain)"));
        Files.createDirectories(tempDir.resolve("org.example.FirstTest/another_test(java.time.LocalDate, boolean)"));
        Files.createDirectories(tempDir.resolve("org.example.SecondTest/table test(java.lang.String, java.lang.Class)"));

        Files.createFile(tempDir.resolve("org.example.FirstTest/table_test(java.util.List, org.example.Domain)/Leap Year Rules.yaml"));
        Files.createFile(tempDir.resolve("org.example.FirstTest/another_test(java.time.LocalDate, boolean)/another_test (LocalDate, boolean).yaml"));
        Files.createFile(tempDir.resolve("org.example.SecondTest/A Custom Test Title!.yaml"));
        Files.createFile(tempDir.resolve("org.example.SecondTest/table test(java.lang.String, java.lang.Class)/table test (String, Class).yaml"));

        Map<String, Object> tree = ReportTree.process(tempDir);

        assertThat(tree)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(new ContextLoader().fromYaml("""
                type: index
                name: example
                outPath: ""
                contents:
                  - type: index
                    name: FirstTest
                    outPath: /firsttest
                    contents:
                      - type: table
                        name: Leap Year Rules
                        outPath: /firsttest/leap-year-rules
                        resource: org.example.FirstTest/table_test(java.util.List, org.example.Domain)/Leap Year Rules.yaml
                      - type: table
                        name: another_test (LocalDate, boolean)
                        outPath: /firsttest/another_test-localdate-boolean
                        resource: org.example.FirstTest/another_test(java.time.LocalDate, boolean)/another_test (LocalDate, boolean).yaml
                  - type: index
                    name: A Custom Test Title!
                    outPath: /a-custom-test-title
                    resource: org.example.SecondTest/A Custom Test Title!.yaml
                    contents:
                      - type: table
                        name: table test (String, Class)
                        outPath: /a-custom-test-title/table-test-string-class
                        resource: org.example.SecondTest/table test(java.lang.String, java.lang.Class)/table test (String, Class).yaml
                """)
            );
    }

    @Test
    void shouldSupportNestedTestClasses(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("pkg.T1/table1"));
        Files.createDirectories(tempDir.resolve("pkg.T1/Nested/table2"));
        Files.createDirectories(tempDir.resolve("pkg.T1/Nested/DeeplyNested/table1"));

        Files.createFile(tempDir.resolve("pkg.T1/table1/tabletest1.yaml"));
        Files.createFile(tempDir.resolve("pkg.T1/Nested/table2/tabletest2.yaml"));
        Files.createFile(tempDir.resolve("pkg.T1/Nested/DeeplyNested/table1/tabletest1.yaml"));

        Map<String, Object> tree = ReportTree.process(tempDir);

        assertThat(tree)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(new ContextLoader().fromYaml("""
                type: index
                name: T1
                outPath: ""
                contents:
                  - type: table
                    name: tabletest1
                    outPath: /tabletest1
                    resource: pkg.T1/table1/tabletest1.yaml
                  - type: index
                    name: Nested
                    outPath: /nested
                    contents:
                      - type: table
                        name: tabletest2
                        outPath: /nested/tabletest2
                        resource: pkg.T1/Nested/table2/tabletest2.yaml
                      - type: index
                        name: DeeplyNested
                        outPath: /nested/deeplynested
                        contents:
                          - type: table
                            name: tabletest1
                            outPath: /nested/deeplynested/tabletest1
                            resource: pkg.T1/Nested/DeeplyNested/table1/tabletest1.yaml
                """)
            );
    }

}
