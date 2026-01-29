package io.github.nchaugen.tabletest.reporter;

import io.github.nchaugen.tabletest.junit.TableTest;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class InputDirectoryResolverTest {

    @TempDir
    Path tempDir;

    @TableTest("""
            Scenario                                        | Build dir | Input dir | JUnit dir    | JUnit dir state  | Fallback dir state | Resolved path?       | Source?        | Searched locations?
            Configured input dir always take presedence     | build     | my/config | report/junit | yaml             | yaml               | my/config            | CONFIGURED     | [my/config]
            JUnit with YAML wins over fallback with yaml    | build     |           | report/junit | yaml             | yaml               | report/junit         | JUNIT_PROPERTY | [report/junit, build/junit-jupiter]
            Fallback with YAML wins over empty JUnit        | target    |           | report/junit | {empty, missing} | yaml               | target/junit-jupiter | FALLBACK       | [report/junit, target/junit-jupiter]
            Existing JUnit wins over empty fallback         | target    |           | report/junit | empty            | empty              | report/junit         | JUNIT_PROPERTY | [report/junit, target/junit-jupiter]
            Existing fallback wins over missing JUnit dir   | target    |           |              |                  | {yaml, empty}      | target/junit-jupiter | FALLBACK       | [target/junit-jupiter]
            Unresolved when both Junit and fallback missing | target    |           | report/junit | missing          | missing            |                      | NONE           | [report/junit, target/junit-jupiter, build/junit-jupiter]
            Unresolved when fallback missing                | build     |           |              |                  | missing            |                      | NONE           | [target/junit-jupiter, build/junit-jupiter]
            """)
    void resolvesWithCorrectPriorityAndSource(
            String buildDir,
            String configuredDir,
            String junitDir,
            String junitState,
            String fallbackState,
            String resolvedDir,
            InputDirectoryResolver.ResolutionSource source,
            List<String> searchLocations)
            throws IOException {

        Path fallbackPath = setupDir(tempDir, buildDir + "/junit-jupiter", fallbackState);
        Path configuredPath = setupDir(tempDir, configuredDir, "empty");
        setupDir(tempDir, junitDir, junitState);
        Path junitOutputDir = JunitDirParser.parse(tempDir, junitDir).orElse(null);

        InputDirectoryResolver.Result result = InputDirectoryResolver.resolve(
                configuredPath, fallbackPath == null ? emptyList() : List.of(fallbackPath), tempDir, junitOutputDir);

        Path expectedPath = resolvedDir != null ? tempDir.resolve(resolvedDir) : null;
        assertThat(result.source()).isEqualTo(source);
        assertThat(result.path()).isEqualTo(expectedPath);
        List<Path> expected = searchLocations.stream().map(tempDir::resolve).toList();
        assertThat(result.candidates()).isEqualTo(expected);
    }

    @TableTest("""
            Scenario            | Input dir  | Resolved path?
            Relative path       | sub/dir    | sub/dir
            Absolute path       | /sub/dir   | /sub/dir
            With dot components | sub/../dir | dir
            """)
    void resolvesToNormalizedConfiguredDir(Path configuredDir, Path resolvedDir) {
        InputDirectoryResolver.Result result = InputDirectoryResolver.resolve(configuredDir, List.of(), tempDir, null);

        assertThat(result.path()).isEqualTo(tempDir.resolve(resolvedDir).normalize());
        assertThat(result.source()).isEqualTo(InputDirectoryResolver.ResolutionSource.CONFIGURED);
    }

    @TableTest("""
            Scenario                  | Dir     | Candidates                          | Message?
            No path, no candidates    |         | []                                  | Input directory does not exist
            Path, no candidates       | missing | []                                  | Input directory does not exist: /test/missing
            No path, with candidates  |         | [target/junit-jupiter, build/junit] | Input directory does not exist
            Path, with candidates     | missing | [target/junit-jupiter]              | Input directory does not exist: /test/missing
            """)
    void formatsMissingInputMessage(String dir, List<String> candidates, String expectedMessage) {
        Path basePath = Path.of("/test");
        Path resolvedPath = dir != null ? basePath.resolve(dir) : null;
        List<Path> candidatePaths = candidates.stream().map(basePath::resolve).toList();

        InputDirectoryResolver.Result result = new InputDirectoryResolver.Result(
                resolvedPath, InputDirectoryResolver.ResolutionSource.NONE, candidatePaths);

        String message = result.formatMissingInputMessage();
        assertThat(message).startsWith(expectedMessage);
        candidatePaths.forEach(candidate -> assertThat(message).contains("  - " + candidate.toAbsolutePath()));
    }

    private Path setupDir(Path base, String dir, String state) throws IOException {
        if (dir == null || state == null || "missing".equals(state)) {
            return null;
        }
        Path dirPath = base.resolve(dir);
        Files.createDirectories(dirPath);
        if ("yaml".equals(state)) {
            createTestOutputFile(dirPath);
        }
        return dirPath;
    }

    private void createTestOutputFile(Path dir) throws IOException {
        Files.writeString(dir.resolve("TABLETEST-Test.yaml"), "test: data");
    }
}
