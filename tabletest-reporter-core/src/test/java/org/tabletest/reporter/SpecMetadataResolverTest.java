package org.tabletest.reporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

// Unpublished: internal sidecar-file loading, not a user-facing rule.
@Tag("unpublished")
class SpecMetadataResolverTest {

    @TempDir
    Path projectDir;

    @Test
    void readsMetadataFromAnExistingFile() throws IOException {
        Path file = projectDir.resolve(SpecMetadataResolver.DEFAULT_FILE_NAME);
        Files.writeString(file, """
                title: "Core Spec"
                intro: "An intro."
                """);

        SpecMetadata metadata = SpecMetadataResolver.resolve(file);

        assertThat(metadata.title()).isEqualTo("Core Spec");
        assertThat(metadata.intro()).isEqualTo("An intro.");
    }

    @Test
    void missingFileYieldsEmptyMetadata() {
        Path missing = projectDir.resolve(SpecMetadataResolver.DEFAULT_FILE_NAME);

        assertThat(SpecMetadataResolver.resolve(missing)).isEqualTo(SpecMetadata.EMPTY);
    }

    @Test
    void nullPathYieldsEmptyMetadata() {
        assertThat(SpecMetadataResolver.resolve(null)).isEqualTo(SpecMetadata.EMPTY);
    }

    @Test
    void directoryPathYieldsEmptyMetadata() {
        assertThat(SpecMetadataResolver.resolve(projectDir)).isEqualTo(SpecMetadata.EMPTY);
    }
}
