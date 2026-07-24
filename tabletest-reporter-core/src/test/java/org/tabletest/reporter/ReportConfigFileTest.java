package org.tabletest.reporter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

// Unpublished: internal sidecar-file loading, not a user-facing rule.
class ReportConfigFileTest {

    @TempDir
    Path projectDir;

    @Test
    void readsSpecMetadataFromAnExistingFile() throws IOException {
        Path file = projectDir.resolve(ReportConfigFile.DEFAULT_FILE_NAME);
        Files.writeString(file, """
                title: "Core Spec"
                intro: "An intro."
                """);

        SpecMetadata metadata = ReportConfigFile.read(file).specMetadata();

        assertThat(metadata.title()).isEqualTo("Core Spec");
        assertThat(metadata.intro()).isEqualTo("An intro.");
    }

    @Test
    void readsSpecMetadataAndPublishSelectionFromTheOneFile() throws IOException {
        Path file = projectDir.resolve(ReportConfigFile.DEFAULT_FILE_NAME);
        Files.writeString(file, """
                title: "Core Spec"
                publish:
                  exclude:
                    - parsing
                """);

        ReportConfigFile settings = ReportConfigFile.read(file);

        assertThat(settings.specMetadata().title()).isEqualTo("Core Spec");
        assertThat(settings.publishSelection().exclude()).containsExactly("parsing");
    }

    @Test
    void missingFileYieldsEmptySettings() {
        Path missing = projectDir.resolve(ReportConfigFile.DEFAULT_FILE_NAME);

        assertThat(ReportConfigFile.read(missing)).isEqualTo(ReportConfigFile.EMPTY);
    }

    @Test
    void nullPathYieldsEmptySettings() {
        assertThat(ReportConfigFile.read(null)).isEqualTo(ReportConfigFile.EMPTY);
    }

    @Test
    void directoryPathYieldsEmptySettings() {
        assertThat(ReportConfigFile.read(projectDir)).isEqualTo(ReportConfigFile.EMPTY);
    }
}
