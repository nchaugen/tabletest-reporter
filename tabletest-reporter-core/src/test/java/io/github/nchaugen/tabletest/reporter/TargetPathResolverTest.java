package io.github.nchaugen.tabletest.reporter;

import io.github.nchaugen.tabletest.junit.TableTest;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TargetPathResolver}.
 */
class TargetPathResolverTest {

    @TableTest("""
        Scenario                  | Class Name                        | Expected Path?
        Simple class no package   | MyClass                           | MyClass
        Class with package        | com.example.MyClass               | com/example/MyClass
        Nested class              | com.example.Outer$Inner           | com/example/Outer/Inner
        Deeply nested class       | com.example.Outer$Middle$Inner    | com/example/Outer/Middle/Inner
        Default package nested    | Outer$Inner                       | Outer/Inner
        Single package level      | pkg.MyClass                       | pkg/MyClass
        Deep package hierarchy    | a.b.c.d.e.MyClass                 | a/b/c/d/e/MyClass
        Empty class name          | ''                                | ./
        Blank class name          | '   '                             | ./
        """)
    void classPathFromClassName_converts_class_names(String className, Path expectedPath) {
        Path result = TargetPathResolver.classPathFromClassName(className);

        assertThat(result).isEqualTo(expectedPath);
    }

    @TableTest("""
        Scenario                        | Class Resource     | Table Path         | Expected?
        Relative path with class dir    | some/path/Class    | table.csv          | some/path/table.csv
        Relative path subdirectory      | some/path/Class    | data/table.csv     | some/path/data/table.csv
        Parent directory navigation     | some/path/Class    | ../table.csv       | some/table.csv
        Null class resource             |                    | table.csv          | table.csv
        Absolute-style relative path    | some/path/Class    | other/path/tbl.csv | some/path/other/path/tbl.csv
        Null table path                 | some/path/Class    |                    |
        Empty table path                | some/path/Class    | ''                 |
        Blank table path                | some/path/Class    | '   '              |
        """)
    void resolveTableResource_resolves_paths(Path classResource, String tablePath, Path expected) {

        Path result = TargetPathResolver.resolveTableResource(classResource, tablePath);

        assertThat(result).isEqualTo(expected);
    }
}
