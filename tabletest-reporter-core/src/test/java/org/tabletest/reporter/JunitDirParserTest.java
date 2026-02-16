package org.tabletest.reporter;

import org.junit.jupiter.api.io.TempDir;
import org.tabletest.junit.Scenario;
import org.tabletest.junit.TableTest;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JunitDirParserTest {

    @TableTest("""
            Scenario       | Value        | Parsed?
            Relative path  | report/junit | report/junit
            Absolute path  | /abs/junit   | /abs/junit
            Null           |              |
            Empty string   | ''           |
            Blank string   | '   '        |
            """)
    void parsesValue(@Scenario String _scenario, String value, Path expected, @TempDir Path tempDir) {
        Optional<Path> result = JunitDirParser.parse(tempDir, value);

        if (expected == null) {
            assertThat(result).isEmpty();
        } else if (expected.isAbsolute()) {
            assertThat(result).contains(expected);
        } else {
            assertThat(result).contains(tempDir.resolve(expected));
        }
    }
}
