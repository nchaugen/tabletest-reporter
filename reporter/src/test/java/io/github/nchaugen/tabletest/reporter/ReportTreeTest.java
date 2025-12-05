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
                : ReportTree.Target.withPath(e.getKey()).withResource(e.getValue())
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

        Map<String, Object> tree = ReportTree.walk(tempDir);

        assertThat(tree)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(new ContextLoader().fromYaml("""
                type: index
                name: pkg
                outPath: .
                contents:
                  - type: index
                    name: T1Test
                    outPath: ./T1Test
                    resource: pkg.T1/T1Test.yaml
                    contents:
                      - type: table
                        name: tabletest1
                        outPath: ./T1Test/tabletest1
                        resource: pkg.T1/table1/tabletest1.yaml
                      - type: table
                        name: tabletest2
                        outPath: ./T1Test/tabletest2
                        resource: pkg.T1/table2/tabletest2.yaml
                  - type: index
                    name: T2Test
                    outPath: ./T2Test
                    resource: pkg.T2/T2Test.yaml
                    contents:
                      - type: table
                        name: tabletest1
                        outPath: ./T2Test/tabletest1
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

        Map<String, Object> tree = ReportTree.walk(tempDir);

        assertThat(tree)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(new ContextLoader().fromYaml("""
                type: index
                name: pkg
                outPath: .
                contents:
                  - type: index
                    name: products
                    outPath: ./products
                    contents:
                      - type: index
                        name: T1Test
                        outPath: ./products/T1Test
                        resource: com.pkg.products.T1/T1Test.yaml
                        contents:
                          - type: table
                            name: tabletest1
                            outPath: ./products/T1Test/tabletest1
                            resource: com.pkg.products.T1/table1/tabletest1.yaml
                          - type: table
                            name: tabletest2
                            outPath: ./products/T1Test/tabletest2
                            resource: com.pkg.products.T1/table2/tabletest2.yaml
                  - type: index
                    name: orders
                    outPath: ./orders
                    contents:
                      - type: index
                        name: T2Test
                        outPath: ./orders/T2Test
                        resource: com.pkg.orders.T2/T2Test.yaml
                        contents:
                          - type: table
                            name: tabletest1
                            outPath: ./orders/T2Test/tabletest1
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

        Map<String, Object> tree = ReportTree.walk(tempDir);

        assertThat(tree)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(new ContextLoader().fromYaml("""
                type: index
                name: "no"
                outPath: .
                contents:
                  - type: index
                    name: pkg
                    outPath: ./pkg
                    contents:
                      - type: index
                        name: products
                        outPath: ./pkg/products
                        contents:
                          - type: index
                            name: T1Test
                            outPath: ./pkg/products/T1Test
                            resource: no.pkg.products.T1/T1Test.yaml
                            contents:
                              - type: table
                                name: tabletest1
                                outPath: ./pkg/products/T1Test/tabletest1
                                resource: no.pkg.products.T1/table1/tabletest1.yaml
                      - type: index
                        name: packages
                        outPath: ./pkg/packages
                        contents:
                          - type: index
                            name: T1
                            outPath: ./pkg/packages/T1
                            contents:
                              - type: table
                                name: tabletest2
                                outPath: ./pkg/packages/T1/tabletest2
                                resource: no.pkg.packages.T1/table2/tabletest2.yaml
                  - type: index
                    name: oth
                    outPath: ./oth
                    contents:
                      - type: index
                        name: orders
                        outPath: ./oth/orders
                        contents:
                          - type: index
                            name: T2Test
                            outPath: ./oth/orders/T2Test
                            resource: no.oth.orders.T2/T2Test.yaml
                            contents:
                              - type: table
                                name: tabletest1
                                outPath: ./oth/orders/T2Test/tabletest1
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

        Map<String, Object> tree = ReportTree.walk(tempDir);

        assertThat(tree)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(new ContextLoader().fromYaml("""
                type: index
                name: pkg
                outPath: .
                contents:
                  - type: index
                    name: T1
                    outPath: ./T1
                    contents:
                      - type: table
                        name: tabletest1
                        outPath: ./T1/tabletest1
                        resource: pkg.T1/table1/tabletest1.yaml
                      - type: table
                        name: tabletest2
                        outPath: ./T1/tabletest2
                        resource: pkg.T1/table2/tabletest2.yaml
                  - type: index
                    name: T2
                    outPath: ./T2
                    contents:
                      - type: table
                        name: tabletest1
                        outPath: ./T2/tabletest1
                        resource: pkg.T2/table1/tabletest1.yaml
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

        Map<String, Object> tree = ReportTree.walk(tempDir);

        assertThat(tree)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(new ContextLoader().fromYaml("""
                type: index
                name: T1
                outPath: .
                contents:
                  - type: table
                    name: tabletest1
                    outPath: ./tabletest1
                    resource: pkg.T1/table1/tabletest1.yaml
                  - type: index
                    name: Nested
                    outPath: ./Nested
                    contents:
                      - type: table
                        name: tabletest2
                        outPath: ./Nested/tabletest2
                        resource: pkg.T1/Nested/table2/tabletest2.yaml
                      - type: index
                        name: DeeplyNested
                        outPath: ./Nested/DeeplyNested
                        contents:
                          - type: table
                            name: tabletest1
                            outPath: ./Nested/DeeplyNested/tabletest1
                            resource: pkg.T1/Nested/DeeplyNested/table1/tabletest1.yaml
                """)
            );
    }

}
