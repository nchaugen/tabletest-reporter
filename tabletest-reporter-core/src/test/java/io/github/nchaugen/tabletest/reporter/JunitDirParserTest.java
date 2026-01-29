package io.github.nchaugen.tabletest.reporter;

import io.github.nchaugen.tabletest.junit.Scenario;
import io.github.nchaugen.tabletest.junit.TableTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JunitDirParserTest {

    @TableTest("""
            Scenario                  | Override     | Parsed?
            Relative path             | report/junit | report/junit
            Absolute path             | /abs/junit   | /abs/junit
            Null falls back           |              |
            Empty string falls back   | ''           |
            Blank string falls back   | '   '        |
            """)
    void parsesOverrideValue(@Scenario String _scenario, String override, Path expected, @TempDir Path tempDir) {
        Optional<Path> result = JunitDirParser.parse(tempDir, override);

        if (expected == null) {
            assertThat(result).isEmpty();
        } else if (expected.isAbsolute()) {
            assertThat(result).contains(expected);
        } else {
            assertThat(result).contains(tempDir.resolve(expected));
        }
    }

    @Test
    void usesSystemPropertyWhenOverrideIsNull(@TempDir Path tempDir) {
        String previous = System.getProperty("junit.platform.reporting.output.dir");
        try {
            System.setProperty("junit.platform.reporting.output.dir", "sys/prop/dir");

            Optional<Path> result = JunitDirParser.parse(tempDir, null);

            assertThat(result).contains(tempDir.resolve("sys/prop/dir"));
        } finally {
            if (previous != null) {
                System.setProperty("junit.platform.reporting.output.dir", previous);
            } else {
                System.clearProperty("junit.platform.reporting.output.dir");
            }
        }
    }
}
