package org.tabletest.reporter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Unpublished: internal mechanism (input-directory resolution), not a user-facing rule.
class InputDirectoriesTest {

    @TempDir
    Path projectDir;

    @Test
    void resolvesRelativeDirectoriesAgainstTheProjectDirectory() throws IOException {
        Files.createDirectories(projectDir.resolve("core/target/junit-jupiter"));

        InputDirectories inputs = InputDirectories.resolve(List.of(Path.of("core/target/junit-jupiter")), projectDir);

        assertThat(inputs.present()).containsExactly(projectDir.resolve("core/target/junit-jupiter"));
        assertThat(inputs.missing()).isEmpty();
        assertThat(inputs.isEmpty()).isFalse();
    }

    @Test
    void separatesDirectoriesThatExistFromThoseThatDoNot() throws IOException {
        Path built = Files.createDirectories(projectDir.resolve("built"));
        Path notBuilt = projectDir.resolve("not-built");

        InputDirectories inputs = InputDirectories.resolve(List.of(built, notBuilt), projectDir);

        assertThat(inputs.present()).containsExactly(built);
        assertThat(inputs.missing()).containsExactly(notBuilt);
        assertThat(inputs.formatSkippedInputMessage()).contains("not-built");
    }

    @Test
    void keepsDeclaredOrderAndCollapsesRepeats() throws IOException {
        Path first = Files.createDirectories(projectDir.resolve("first"));
        Path second = Files.createDirectories(projectDir.resolve("second"));

        InputDirectories inputs = InputDirectories.resolve(Arrays.asList(second, first, second, null), projectDir);

        assertThat(inputs.present()).containsExactly(second, first);
    }

    @Test
    void everyDirectoryMissingLeavesNothingToReportFrom() {
        InputDirectories inputs =
                InputDirectories.resolve(List.of(projectDir.resolve("a"), projectDir.resolve("b")), projectDir);

        assertThat(inputs.isEmpty()).isTrue();
        assertThat(inputs.formatMissingInputMessage())
                .contains("No input directory exists among the configured locations:")
                .contains("a")
                .contains("b");
    }

    @Test
    void noConfiguredDirectoriesLeavesNothingToReportFrom() {
        assertThat(InputDirectories.resolve(null, projectDir).isEmpty()).isTrue();
        assertThat(InputDirectories.resolve(List.of(), projectDir).isEmpty()).isTrue();
    }
}
