package org.tabletest.reporter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportTreeTest {

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

        Map<String, Object> orderClassYaml = classYaml(
                "pkg.orders.OrderTest",
                "order-test",
                List.of(tableEntry("TABLETEST-order-items.yaml", "orderItems", "order-items")));
        Map<String, Object> productClassYaml = classYaml(
                "pkg.products.ProductTest",
                "product-test",
                List.of(tableEntry("TABLETEST-product-price.yaml", "productPrice", "product-price")));

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
                                        orderClassYaml,
                                        List.of(new TableNode(
                                                "order-items", "/orders/order-test/order-items", emptyYaml()))))),
                        new IndexNode(
                                "products",
                                "/products",
                                null,
                                List.of(new IndexNode(
                                        "product-test",
                                        "/products/product-test",
                                        productClassYaml,
                                        List.of(new TableNode(
                                                "product-price",
                                                "/products/product-test/product-price",
                                                emptyYaml())))))));

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

        Map<String, Object> orderClassYaml = classYaml(
                "pkg.orders.OrderTest",
                "order-test",
                List.of(tableEntry("../tables/TABLETEST-order-items.yaml", "orderItems", "order-items")));
        Map<String, Object> productClassYaml = classYaml(
                "pkg.products.ProductTest",
                "product-test",
                List.of(tableEntry("../../tables/TABLETEST-product-price.yaml", "productPrice", "product-price")));

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
                                        orderClassYaml,
                                        List.of(new TableNode(
                                                "order-items", "/orders/order-test/order-items", emptyYaml()))))),
                        new IndexNode(
                                "products",
                                "/products",
                                null,
                                List.of(new IndexNode(
                                        "product-test",
                                        "/products/product-test",
                                        productClassYaml,
                                        List.of(new TableNode(
                                                "product-price",
                                                "/products/product-test/product-price",
                                                emptyYaml())))))));

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

        Map<String, Object> classYaml = classYaml(
                "pkg.Outer$InnerTest", "inner-test", List.of(tableEntry("TABLETEST-inner.yaml", "inner", "inner")));

        ReportNode expected = new IndexNode(
                "Outer",
                "",
                null,
                List.of(new IndexNode(
                        "inner-test",
                        "/inner-test",
                        classYaml,
                        List.of(new TableNode("inner", "/inner-test/inner", emptyYaml())))));

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

        Map<String, Object> classYaml = classYaml(
                "pkg.ClassTest",
                "class-test",
                List.of(tableEntry("tables/../tables/TABLETEST-normalized.yaml", "normalized", "normalized")));

        ReportNode expected = new IndexNode(
                "pkg",
                "",
                null,
                List.of(new IndexNode(
                        "class-test",
                        "/class-test",
                        classYaml,
                        List.of(new TableNode("normalized", "/class-test/normalized", emptyYaml())))));

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

        Map<String, Object> classYaml = classYaml(
                "pkg.ClassTest",
                "class-test",
                List.of(
                        tableEntry("TABLETEST-present.yaml", "present", "present"),
                        tableEntry("TABLETEST-missing.yaml", "missing", "missing")));

        ReportNode expected = new IndexNode(
                "pkg",
                "",
                null,
                List.of(new IndexNode(
                        "class-test",
                        "/class-test",
                        classYaml,
                        List.of(new TableNode("present", "/class-test/present", emptyYaml())))));

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

        Map<String, Object> fooClassYaml = classYaml(
                "com.a.FooTest", "foo-test", List.of(tableEntry("TABLETEST-foo-table.yaml", "fooTable", "foo-table")));
        Map<String, Object> barClassYaml = classYaml(
                "org.b.BarTest", "bar-test", List.of(tableEntry("TABLETEST-bar-table.yaml", "barTable", "bar-table")));

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
                                                fooClassYaml,
                                                List.of(new TableNode(
                                                        "foo-table", "/com/a/foo-test/foo-table", emptyYaml()))))))),
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
                                                barClassYaml,
                                                List.of(new TableNode(
                                                        "bar-table", "/org/b/bar-test/bar-table", emptyYaml())))))))));

        assertThat(tree).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected);
    }

    private static Map<String, Object> classYaml(String className, String slug, List<Map<String, Object>> tableTests) {
        Map<String, Object> yaml = new LinkedHashMap<>();
        yaml.put("className", className);
        yaml.put("slug", slug);
        yaml.put("tableTests", tableTests);
        return yaml;
    }

    private static Map<String, Object> tableEntry(String path, String methodName, String slug) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("path", path);
        entry.put("methodName", methodName);
        entry.put("slug", slug);
        return entry;
    }

    private static Map<String, Object> emptyYaml() {
        return Map.of();
    }
}
