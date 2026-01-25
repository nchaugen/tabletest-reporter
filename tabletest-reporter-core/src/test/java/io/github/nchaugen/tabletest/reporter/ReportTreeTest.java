package io.github.nchaugen.tabletest.reporter;

import io.github.nchaugen.tabletest.junit.Scenario;
import io.github.nchaugen.tabletest.junit.TableTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
            @TempDir Path tempDir) {
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

    @Test
    void shouldBuildTreeFromYamlMetadata(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("TABLETEST-order-test.yaml"), """
                "className": "pkg.orders.OrderTest"
                "slug": "order-test"
                "tableTests":
                - "path": "TABLETEST-order-items.yaml"
                  "methodName": "orderItems"
                  "slug": "order-items"
                """);
        Files.writeString(tempDir.resolve("TABLETEST-order-items.yaml"), "");

        Files.writeString(tempDir.resolve("TABLETEST-product-test.yaml"), """
                "className": "pkg.products.ProductTest"
                "slug": "product-test"
                "tableTests":
                - "path": "TABLETEST-product-price.yaml"
                  "methodName": "productPrice"
                  "slug": "product-price"
                """);
        Files.writeString(tempDir.resolve("TABLETEST-product-price.yaml"), "");

        ReportNode tree = ReportTree.process(tempDir);

        ReportNode expected = new IndexNode(
                "pkg",
                "",
                null,
                List.of(
                        new IndexNode(
                                "orders",
                                "/orders",
                                null,
                                List.of(new IndexNode(
                                        "order-test",
                                        "/orders/order-test",
                                        "TABLETEST-order-test.yaml",
                                        List.of(new TableNode(
                                                "order-items",
                                                "/orders/order-test/order-items",
                                                "TABLETEST-order-items.yaml"))))),
                        new IndexNode(
                                "products",
                                "/products",
                                null,
                                List.of(new IndexNode(
                                        "product-test",
                                        "/products/product-test",
                                        "TABLETEST-product-test.yaml",
                                        List.of(new TableNode(
                                                "product-price",
                                                "/products/product-test/product-price",
                                                "TABLETEST-product-price.yaml")))))));

        assertThat(tree).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected);
    }

    @Test
    void shouldBuildTreeFromYamlMetadataInArbitraryDirectories(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("out/classes"));
        Files.createDirectories(tempDir.resolve("out/classes/sub"));
        Files.createDirectories(tempDir.resolve("out/tables"));

        Files.writeString(tempDir.resolve("out/classes/TABLETEST-order-test.yaml"), """
                "className": "pkg.orders.OrderTest"
                "slug": "order-test"
                "tableTests":
                - "path": "../tables/TABLETEST-order-items.yaml"
                  "methodName": "orderItems"
                  "slug": "order-items"
                """);
        Files.writeString(tempDir.resolve("out/tables/TABLETEST-order-items.yaml"), "");

        Files.writeString(tempDir.resolve("out/classes/sub/TABLETEST-product-test.yaml"), """
                "className": "pkg.products.ProductTest"
                "slug": "product-test"
                "tableTests":
                - "path": "../../tables/TABLETEST-product-price.yaml"
                  "methodName": "productPrice"
                  "slug": "product-price"
                """);
        Files.writeString(tempDir.resolve("out/tables/TABLETEST-product-price.yaml"), "");

        ReportNode tree = ReportTree.process(tempDir);

        ReportNode expected = new IndexNode(
                "pkg",
                "",
                null,
                List.of(
                        new IndexNode(
                                "orders",
                                "/orders",
                                null,
                                List.of(new IndexNode(
                                        "order-test",
                                        "/orders/order-test",
                                        "out/classes/TABLETEST-order-test.yaml",
                                        List.of(new TableNode(
                                                "order-items",
                                                "/orders/order-test/order-items",
                                                "out/tables/TABLETEST-order-items.yaml"))))),
                        new IndexNode(
                                "products",
                                "/products",
                                null,
                                List.of(new IndexNode(
                                        "product-test",
                                        "/products/product-test",
                                        "out/classes/sub/TABLETEST-product-test.yaml",
                                        List.of(new TableNode(
                                                "product-price",
                                                "/products/product-test/product-price",
                                                "out/tables/TABLETEST-product-price.yaml")))))));

        assertThat(tree).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected);
    }

    @Test
    void shouldSupportNestedClassNamesFromYamlMetadata(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("TABLETEST-inner-test.yaml"), """
                "className": "pkg.Outer$InnerTest"
                "slug": "inner-test"
                "tableTests":
                - "path": "TABLETEST-inner.yaml"
                  "methodName": "inner"
                  "slug": "inner"
                """);
        Files.writeString(tempDir.resolve("TABLETEST-inner.yaml"), "");

        ReportNode tree = ReportTree.process(tempDir);

        ReportNode expected = new IndexNode(
                "Outer",
                "",
                null,
                List.of(new IndexNode(
                        "inner-test",
                        "/inner-test",
                        "TABLETEST-inner-test.yaml",
                        List.of(new TableNode("inner", "/inner-test/inner", "TABLETEST-inner.yaml")))));

        assertThat(tree).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected);
    }

    @Test
    void shouldResolveAndNormalizeTablePathsFromYamlMetadata(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("out/tables"));

        Files.writeString(tempDir.resolve("out/TABLETEST-class.yaml"), """
                "className": "pkg.ClassTest"
                "slug": "class-test"
                "tableTests":
                - "path": "tables/../tables/TABLETEST-normalized.yaml"
                  "methodName": "normalized"
                  "slug": "normalized"
                """);
        Files.writeString(tempDir.resolve("out/tables/TABLETEST-normalized.yaml"), "");

        ReportNode tree = ReportTree.process(tempDir);

        ReportNode expected = new IndexNode(
                "pkg",
                "",
                null,
                List.of(new IndexNode(
                        "class-test",
                        "/class-test",
                        "out/TABLETEST-class.yaml",
                        List.of(new TableNode(
                                "normalized", "/class-test/normalized", "out/tables/TABLETEST-normalized.yaml")))));

        assertThat(tree).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected);
    }

    @Test
    void shouldIgnoreMissingTableResourcesInYamlMetadata(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("TABLETEST-class.yaml"), """
                "className": "pkg.ClassTest"
                "slug": "class-test"
                "tableTests":
                - "path": "TABLETEST-present.yaml"
                  "methodName": "present"
                  "slug": "present"
                - "path": "TABLETEST-missing.yaml"
                  "methodName": "missing"
                  "slug": "missing"
                """);
        Files.writeString(tempDir.resolve("TABLETEST-present.yaml"), "");

        ReportNode tree = ReportTree.process(tempDir);

        ReportNode expected = new IndexNode(
                "pkg",
                "",
                null,
                List.of(new IndexNode(
                        "class-test",
                        "/class-test",
                        "TABLETEST-class.yaml",
                        List.of(new TableNode("present", "/class-test/present", "TABLETEST-present.yaml")))));

        assertThat(tree).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected);
    }

    @Test
    void shouldFallbackToMethodNameOrFilenameWhenSlugMissing(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("TABLETEST-fallback-test.yaml"), """
                "className": "pkg.FallbackTest"
                "slug": "fallback-test"
                "tableTests":
                - "path": "TABLETEST-by-method.yaml"
                  "methodName": "byMethod"
                - "path": "TABLETEST-by-filename.yaml"
                """);
        Files.writeString(tempDir.resolve("TABLETEST-by-method.yaml"), "");
        Files.writeString(tempDir.resolve("TABLETEST-by-filename.yaml"), "");

        ReportNode tree = ReportTree.process(tempDir);

        ReportNode expected = new IndexNode(
                "pkg",
                "",
                null,
                List.of(new IndexNode(
                        "fallback-test",
                        "/fallback-test",
                        "TABLETEST-fallback-test.yaml",
                        List.of(
                                new TableNode("by-method", "/fallback-test/by-method", "TABLETEST-by-method.yaml"),
                                new TableNode(
                                        "by-filename", "/fallback-test/by-filename", "TABLETEST-by-filename.yaml")))));

        assertThat(tree).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected);
    }

    @Test
    void shouldIgnoreInvalidYamlMetadata(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("TABLETEST-invalid.yaml"), """
                "className": ""
                "slug": ""
                "tableTests": "not-a-list"
                """);

        ReportNode tree = ReportTree.process(tempDir);

        assertThat(tree).isNull();
    }

    @Test
    void shouldSupportMultiplePackageRootsFromYamlMetadata(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("TABLETEST-foo.yaml"), """
                "className": "com.a.FooTest"
                "slug": "foo-test"
                "tableTests":
                - "path": "TABLETEST-foo-table.yaml"
                  "methodName": "fooTable"
                  "slug": "foo-table"
                """);
        Files.writeString(tempDir.resolve("TABLETEST-foo-table.yaml"), "");

        Files.writeString(tempDir.resolve("TABLETEST-bar.yaml"), """
                "className": "org.b.BarTest"
                "slug": "bar-test"
                "tableTests":
                - "path": "TABLETEST-bar-table.yaml"
                  "methodName": "barTable"
                  "slug": "bar-table"
                """);
        Files.writeString(tempDir.resolve("TABLETEST-bar-table.yaml"), "");

        ReportNode tree = ReportTree.process(tempDir);

        ReportNode expected = new IndexNode(
                null,
                "",
                null,
                List.of(
                        new IndexNode(
                                "com",
                                "/com",
                                null,
                                List.of(new IndexNode(
                                        "a",
                                        "/com/a",
                                        null,
                                        List.of(new IndexNode(
                                                "foo-test",
                                                "/com/a/foo-test",
                                                "TABLETEST-foo.yaml",
                                                List.of(new TableNode(
                                                        "foo-table",
                                                        "/com/a/foo-test/foo-table",
                                                        "TABLETEST-foo-table.yaml"))))))),
                        new IndexNode(
                                "org",
                                "/org",
                                null,
                                List.of(new IndexNode(
                                        "b",
                                        "/org/b",
                                        null,
                                        List.of(new IndexNode(
                                                "bar-test",
                                                "/org/b/bar-test",
                                                "TABLETEST-bar.yaml",
                                                List.of(new TableNode(
                                                        "bar-table",
                                                        "/org/b/bar-test/bar-table",
                                                        "TABLETEST-bar-table.yaml")))))))));

        assertThat(tree).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected);
    }
}
