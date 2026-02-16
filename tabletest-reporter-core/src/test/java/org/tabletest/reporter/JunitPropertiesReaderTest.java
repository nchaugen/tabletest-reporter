package org.tabletest.reporter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.junit.Scenario;
import org.tabletest.junit.TableTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JunitPropertiesReaderTest {

    @Test
    void resolve_returns_empty_when_file_does_not_exist(@TempDir Path tempDir) {
        Optional<Path> result = JunitPropertiesReader.resolve(tempDir);

        assertThat(result).isEmpty();
    }

    @TableTest("""
            Scenario                       | Output dir value              | Resolved path?
            Deterministic path             | report/out                    | report/out
            Placeholder stripped to parent | "report/{uniqueNumber}"       | report
            Bare placeholder resolves to . | "{uniqueNumber}"              | .
            Placeholder truncates trailing | "build/{uniqueNumber}/report" | build
            Property not present           |                               |
            Empty value                    | ''                            |
            """)
    void resolves_output_dir_to_path(
            @Scenario String _scenario, String outputDirValue, String resolvedPath, @TempDir Path tempDir)
            throws IOException {
        if (outputDirValue != null) {
            writeProperties(tempDir, outputDirValue);
        }

        Optional<Path> result = JunitPropertiesReader.resolve(tempDir);

        if (resolvedPath == null) {
            assertThat(result).isEmpty();
        } else {
            assertThat(result).contains(tempDir.resolve(resolvedPath));
        }
    }

    private void writeProperties(Path baseDir, String outputDirValue) throws IOException {
        Path propertiesFile = baseDir.resolve("src/test/resources/junit-platform.properties");
        Files.createDirectories(propertiesFile.getParent());
        Files.writeString(propertiesFile, "junit.platform.reporting.output.dir=" + outputDirValue);
    }
}
