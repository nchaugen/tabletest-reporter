package org.tabletest.reporter.structure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.ReportStructure;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rules the report's page tree follows, stated as the classes that published and the pages
 * that result. Built through {@link ReportStructure}; {@link ReportTreeTest} keeps the
 * whole-node assertions as conformance.
 */
@DisplayName("The page tree")
@Description("""
        Nobody chooses the shape of a report. It follows the packages of the test classes that
        published. Below, a published table is written as the class that ran it and the method that
        holds it.

        The indexDepth option decides how much of the tree gets an index page. It has its own
        feature, alongside this one.
        """)
class ReportStructureTest {

    @TempDir
    Path workingDir;

    @DisplayName("Makes one page for each package, class, and table")
    @TableTest("""
        Scenario                        | Published tables                                                 | Page tree?
        One class with one table        | ['pkg.OrderTest#items']                                          | [pkg: [[order-test: [items]]]]
        One class with two tables       | ['pkg.OrderTest#items', 'pkg.OrderTest#totals']                  | [pkg: [[order-test: [items, totals]]]]
        Two classes in one package      | ['pkg.OrderTest#items', 'pkg.ProductTest#price']                 | [pkg: [[order-test: [items]], [product-test: [price]]]]
        Two classes in sibling packages | ['pkg.orders.OrderTest#items', 'pkg.products.ProductTest#price'] | [pkg: [[orders: [[order-test: [items]]]], [products: [[product-test: [price]]]]]]
        A nested test class             | ['pkg.OrderTest$WhenEmpty#items']                                | [OrderTest: [[when-empty: [items]]]]
        """)
    void mirrorsThePackageHierarchy(List<String> publishedTables, Map<String, Object> pageTree) {
        assertThat(ReportStructure.pageTreeFor(publishedTables, workingDir)).isEqualTo(pageTree);
    }

    @DisplayName("Opens the report at the deepest shared package")
    @Description("""
            A package above the root is the same for every page, so a reader would walk through it
            and never make a choice there.
            """)
    @TableTest("""
        Scenario                          | Published tables                                                                 | Page tree?
        Every class in one package        | ['com.example.orders.OrderTest#items']                                           | [orders: [[order-test: [items]]]]
        A class in a sibling package      | ['com.example.orders.OrderTest#items', 'com.example.products.ProductTest#price'] | [example: [[orders: [[order-test: [items]]]], [products: [[product-test: [price]]]]]]
        Classes sharing no package at all | ['com.a.FooTest#foo', 'org.b.BarTest#bar']                                       | ['(root)': [[com: [[a: [[foo-test: [foo]]]]]], [org: [[b: [[bar-test: [bar]]]]]]]]
        """)
    void rootsTheReportAtTheDeepestSharedPackage(List<String> publishedTables, Map<String, Object> pageTree) {
        assertThat(ReportStructure.pageTreeFor(publishedTables, workingDir)).isEqualTo(pageTree);
    }
}
