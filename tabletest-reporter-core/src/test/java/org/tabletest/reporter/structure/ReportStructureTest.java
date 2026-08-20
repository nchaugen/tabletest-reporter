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
        A report is a tree of pages, and nobody chooses its shape. The shape is the package
        hierarchy of the test classes that published, with one page for each class and one page
        for each table.

        In the tables below, a published table is written as the class that ran it and the method
        that holds it. A page that holds other pages is written as a name with its contents. A page
        that holds none is written as a name on its own. This is the tree the sidebar and the index
        pages show.

        The indexDepth option decides how much of that tree gets an index page of its own. It has
        its own feature, alongside this one.
        """)
class ReportStructureTest {

    @TempDir
    Path workingDir;

    @DisplayName("Mirrors the package hierarchy of the classes that published")
    @Description("""
            Each package becomes an index page. Each test class becomes a page inside its package.
            Each table becomes a page inside its class.

            The reporter names a page after the class or the method it came from, and writes the
            name in the kebab case a URL can carry. Pages sit in alphabetical order. A nested test
            class becomes a page inside the page for the class that encloses it.
            """)
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

    @DisplayName("Roots the report at the deepest shared package")
    @Description("""
            The packages above the root are the same for every page. A reader walks through them
            and never makes a choice there, so the reporter leaves them out. The report opens on
            the first page where the classes differ.

            A class in another package therefore raises the root rather than deepening the tree.
            Classes that share no package at all have a root page with no name of its own, written
            below as (root).
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
