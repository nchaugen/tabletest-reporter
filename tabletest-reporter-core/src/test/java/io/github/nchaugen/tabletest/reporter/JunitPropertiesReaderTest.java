package io.github.nchaugen.tabletest.reporter;

import io.github.nchaugen.tabletest.junit.Scenario;
import io.github.nchaugen.tabletest.junit.TableTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JunitPropertiesReaderTest {

    @Test
    void returns_empty_when_file_does_not_exist(@TempDir Path tempDir) {
        Optional<JunitPropertiesReader.Result> result = JunitPropertiesReader.read(tempDir);

        assertThat(result).isEmpty();
    }

    @TableTest("""
            Scenario             | File Content                                         | Value?           | Deterministic?
            Deterministic path   | junit.platform.reporting.output.dir=report/out       | report/out       | true
            Path with placeholder| junit.platform.reporting.output.dir=r/{uniqueNumber} | r/{uniqueNumber} | false
            Property not present | some.other.property=value                            |                  |
            Empty value          | junit.platform.reporting.output.dir=                 |                  |
            """)
    void reads_output_dir_property(
            @Scenario String _scenario, String fileContent, String value, Boolean deterministic, @TempDir Path tempDir)
            throws IOException {
        Path propertiesFile = tempDir.resolve("src/test/resources/junit-platform.properties");
        Files.createDirectories(propertiesFile.getParent());
        Files.writeString(propertiesFile, fileContent);

        Optional<JunitPropertiesReader.Result> result = JunitPropertiesReader.read(tempDir);

        if (value == null) {
            assertThat(result).isEmpty();
        } else {
            assertThat(result).isPresent();
            assertThat(result.get().value()).isEqualTo(value);
            assertThat(result.get().deterministic()).isEqualTo(deterministic);
        }
    }
}
