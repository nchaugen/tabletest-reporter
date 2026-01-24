package io.github.nchaugen.tabletest.reporter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests error handling behaviour of ReportTree for common user mistakes.
 */
class ReportTreeErrorTest {

    @Test
    void process_throws_NullPointerException_when_directory_is_null() {
        assertThatThrownBy(() -> ReportTree.process(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("dir")
                .hasMessageContaining("cannot be null");
    }

    @Test
    void process_throws_RuntimeException_when_directory_does_not_exist(@TempDir Path tempDir) {
        Path nonExistent = tempDir.resolve("does-not-exist");

        assertThatThrownBy(() -> ReportTree.process(nonExistent))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void process_returns_null_for_empty_directory(@TempDir Path tempDir) {
        ReportNode result = ReportTree.process(tempDir);

        assertThat(result).isNull();
    }

    @Test
    void process_returns_null_for_directory_with_no_yaml_files(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("subdir"));
        Files.writeString(tempDir.resolve("file.txt"), "not a yaml file");
        Files.writeString(tempDir.resolve("subdir/another.xml"), "also not yaml");

        ReportNode result = ReportTree.process(tempDir);

        assertThat(result).isNull();
    }

    @Test
    void process_returns_null_for_directory_with_non_tabletest_yaml_files(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("config.yaml"), "key: value");
        Files.writeString(tempDir.resolve("data.yaml"), "items: []");

        ReportNode result = ReportTree.process(tempDir);

        assertThat(result).isNull();
    }

    @Test
    void findTargets_throws_NullPointerException_when_files_list_is_null() {
        assertThatThrownBy(() -> ReportTree.findTargets(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("files")
                .hasMessageContaining("cannot be null");
    }
}
