package io.github.nchaugen.tabletest.reporter;

import io.github.nchaugen.tabletest.junit.Scenario;
import io.github.nchaugen.tabletest.junit.TableTest;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TestOutputFileFinderTest {

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

        assertThat(TestOutputFileFinder.findTestOutputFiles(tempDir)).containsExactlyElementsOf(expected);
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
}
