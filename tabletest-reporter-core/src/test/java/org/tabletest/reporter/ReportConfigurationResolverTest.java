package org.tabletest.reporter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tabletest.junit.TableTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Unpublished: internal mechanism (report-option resolution), not a user-facing rule.
class ReportConfigurationResolverTest {

    @TempDir
    Path tempDir;

    @TableTest("""
        Scenario               | Format   | Index depth | Single file | Resolved format? | Resolved depth? | Resolved single file?
        Defaults when unset    |          |             |             | asciidoc         | 2147483647      | false
        Markdown alias         | md       | 2           |             | markdown         | 2               | false
        Html with single-file  | html     | 1           | true        | html             | 1               | true
        Infinite depth keyword | markdown | infinite    | false       | markdown         | 2147483647      | false
        """)
    void resolvesFormatIndexDepthAndSingleFile(
            String format,
            String indexDepth,
            Boolean singleFile,
            String resolvedFormat,
            int resolvedDepth,
            boolean resolvedSingleFile) {
        ReportConfiguration config =
                ReportConfigurationResolver.resolve(new ReportOptions(format, null, indexDepth, singleFile, null));

        assertThat(config.format().formatName()).isEqualTo(resolvedFormat);
        assertThat(config.indexDepth().value()).isEqualTo(resolvedDepth);
        assertThat(config.singleFile()).isEqualTo(resolvedSingleFile);
    }

    @Test
    void passesValidTemplateDirectoryThrough() {
        ReportConfiguration config =
                ReportConfigurationResolver.resolve(new ReportOptions("asciidoc", tempDir, null, null, null));

        assertThat(config.templateDirectory()).isEqualTo(tempDir);
    }

    @Test
    void resolvesEmptyCurationWhenNoConfigFile() {
        ReportConfiguration config =
                ReportConfigurationResolver.resolve(new ReportOptions(null, null, null, null, null));

        assertThat(config.specMetadata()).isEqualTo(SpecMetadata.EMPTY);
        assertThat(config.publishSelection()).isEqualTo(PublishSelection.EMPTY);
    }

    @Test
    void readsSpecMetadataAndPublishSelectionFromConfiguredFile() throws IOException {
        Path configFile = Files.writeString(tempDir.resolve("tabletest-reporter.yaml"), """
                title: "Core Spec"
                publish:
                  exclude: [parsing]
                """);

        ReportConfiguration config =
                ReportConfigurationResolver.resolve(new ReportOptions(null, null, null, null, configFile));

        assertThat(config.specMetadata().title()).isEqualTo("Core Spec");
        assertThat(config.publishSelection().exclude()).containsExactly("parsing");
    }

    @Test
    void rejectsMissingTemplateDirectory() {
        Path missing = tempDir.resolve("does-not-exist");

        assertThatThrownBy(
                        () -> ReportConfigurationResolver.resolve(new ReportOptions(null, missing, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Template directory does not exist:");
    }

    @Test
    void rejectsTemplatePathThatIsAFile() throws IOException {
        Path file = Files.createFile(tempDir.resolve("template.txt"));

        assertThatThrownBy(() -> ReportConfigurationResolver.resolve(new ReportOptions(null, file, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Template path is not a directory:");
    }
}
