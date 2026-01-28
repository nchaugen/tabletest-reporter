package io.github.nchaugen.tabletest.reporter;

import io.github.nchaugen.tabletest.junit.TableTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InputDirectoryResolverTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void clearSystemProperties() {
        System.clearProperty("junit.platform.reporting.output.dir");
    }

    @TableTest("""
            Scenario                                | Build Dir | JUnit Property | Configured Input Dir | Resolved Dir?
            Configured input wins                   | build     | report/junit   | tabletest            | tabletest
            JUnit property when no configured input | target    | report/junit   |                      | report/junit
            Fallback when no configured or property | build     |                |                      | build/junit-jupiter
            """)
    void resolvesInputDirectory(
            String buildDir, String junitProperty, String configuredInputDir, String expectedResolvedDir)
            throws IOException {
        Path fallbackDir = tempDir.resolve(buildDir).resolve("junit-jupiter");
        Files.createDirectories(fallbackDir);

        if (junitProperty != null) {
            Path reportingDir = resolveFromBase(junitProperty);
            Files.createDirectories(reportingDir);
            System.setProperty("junit.platform.reporting.output.dir", junitProperty);
        }

        Path configuredPath = configuredInputDir == null ? null : Path.of(configuredInputDir);
        Path configuredDir = resolveFromBase(configuredInputDir);
        if (configuredDir != null) {
            Files.createDirectories(configuredDir);
        }

        InputDirectoryResolver.Result result = InputDirectoryResolver.resolve(configuredPath, null, tempDir, null);

        assertThat(result.path()).isEqualTo(resolveFromBase(expectedResolvedDir));
    }

    @Test
    void formatMissingInputMessage_with_null_path_and_no_candidates() {
        InputDirectoryResolver.Result result =
                new InputDirectoryResolver.Result(null, InputDirectoryResolver.ResolutionSource.NONE, List.of());

        assertThat(result.formatMissingInputMessage()).isEqualTo("Input directory does not exist");
    }

    @Test
    void formatMissingInputMessage_with_path_and_no_candidates() {
        Path missing = tempDir.resolve("missing");
        InputDirectoryResolver.Result result =
                new InputDirectoryResolver.Result(missing, InputDirectoryResolver.ResolutionSource.FALLBACK, List.of());

        assertThat(result.formatMissingInputMessage())
                .isEqualTo("Input directory does not exist: " + missing.toAbsolutePath());
    }

    @Test
    void formatMissingInputMessage_with_null_path_and_candidates() {
        Path candidate1 = tempDir.resolve("target/junit-jupiter");
        Path candidate2 = tempDir.resolve("build/junit-jupiter");
        InputDirectoryResolver.Result result = new InputDirectoryResolver.Result(
                null, InputDirectoryResolver.ResolutionSource.NONE, List.of(candidate1, candidate2));

        String message = result.formatMissingInputMessage();

        assertThat(message).startsWith("Input directory does not exist" + System.lineSeparator());
        assertThat(message).contains("Searched locations:");
        assertThat(message).contains("  - " + candidate1.toAbsolutePath());
        assertThat(message).contains("  - " + candidate2.toAbsolutePath());
    }

    @Test
    void formatMissingInputMessage_with_path_and_candidates() {
        Path missing = tempDir.resolve("missing");
        Path candidate = tempDir.resolve("target/junit-jupiter");
        InputDirectoryResolver.Result result = new InputDirectoryResolver.Result(
                missing, InputDirectoryResolver.ResolutionSource.FALLBACK, List.of(candidate));

        String message = result.formatMissingInputMessage();

        assertThat(message).startsWith("Input directory does not exist: " + missing.toAbsolutePath());
        assertThat(message).contains("Searched locations:");
        assertThat(message).contains("  - " + candidate.toAbsolutePath());
    }

    private Path resolveFromBase(String value) {
        if (value == null) {
            return null;
        }
        Path path = Path.of(value);
        return path.isAbsolute() ? path : tempDir.resolve(path);
    }
}
