package org.tabletest.reporter.structure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.reporter.ReportStructure;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rules the report's page tree follows, stated as the classes that published and the pages
 * that result. Built through {@link ReportStructure}; {@link ReportTreeTest} keeps the
 * whole-node assertions as conformance.
 */
@DisplayName("The page tree")
@Description("""
        A report is a tree of pages, and nobody chooses its shape: it is the package hierarchy of
        the test classes that published, with a page per class and a page per table. Below, a
        published table is written as the class that ran it and the method that holds it, and the
        pages are listed outermost first, indented one step per level — the same tree the sidebar
        and the index pages show. How much of that tree gets its own index page is the indexDepth
        option, alongside.
        """)
class ReportStructureTest {

    @TempDir
    Path workingDir;

    @DisplayName("Mirrors the package hierarchy of the classes that published")
    @Description("""
            Each package becomes an index page, each test class a page inside its package, and
            each table a page inside its class. Pages are named after the class and method they
            came from, in the kebab-case a URL can carry, and sit in alphabetical order. A nested
            test class is a page inside a page for the class that encloses it.
            """)
    @TableTest("""
        Scenario                        | Published tables                                                 | Report pages?
        One class with one table        | ['pkg.OrderTest#items']                                          | ['pkg', '  order-test', '    items']
        One class with two tables       | ['pkg.OrderTest#items', 'pkg.OrderTest#totals']                  | ['pkg', '  order-test', '    items', '    totals']
        Two classes in one package      | ['pkg.OrderTest#items', 'pkg.ProductTest#price']                 | ['pkg', '  order-test', '    items', '  product-test', '    price']
        Two classes in sibling packages | ['pkg.orders.OrderTest#items', 'pkg.products.ProductTest#price'] | ['pkg', '  orders', '    order-test', '      items', '  products', '    product-test', '      price']
        A nested test class             | ['pkg.OrderTest$WhenEmpty#items']                                | ['OrderTest', '  when-empty', '    items']
        """)
    void mirrorsThePackageHierarchy(List<String> publishedTables, List<String> reportPages) {
        assertThat(ReportStructure.pagesFor(publishedTables, workingDir)).isEqualTo(reportPages);
    }

    @DisplayName("Roots the report at the deepest package every published class shares")
    @Description("""
            The packages above that are the same for every page, so they would be levels a reader
            walks through and never chooses in — they are left out, and the report opens on the
            first page where the classes differ. Adding a class in another package therefore
            raises the root rather than deepening the tree. Classes that share no package at all
            are held by a root page with no name of its own, shown here as (root).
            """)
    @TableTest("""
        Scenario                          | Published tables                                                                 | Report pages?
        Every class in one package        | ['com.example.orders.OrderTest#items']                                           | ['orders', '  order-test', '    items']
        A class in a sibling package      | ['com.example.orders.OrderTest#items', 'com.example.products.ProductTest#price'] | ['example', '  orders', '    order-test', '      items', '  products', '    product-test', '      price']
        Classes sharing no package at all | ['com.a.FooTest#foo', 'org.b.BarTest#bar']                                       | ['(root)', '  com', '    a', '      foo-test', '        foo', '  org', '    b', '      bar-test', '        bar']
        """)
    void rootsTheReportAtTheDeepestSharedPackage(List<String> publishedTables, List<String> reportPages) {
        assertThat(ReportStructure.pagesFor(publishedTables, workingDir)).isEqualTo(reportPages);
    }
}
