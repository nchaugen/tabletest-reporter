package org.tabletest.reporter;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Unpublished: internal parsing of the report-config sidecar, not a user-facing rule.
@Tag("unpublished")
class SpecMetadataParseTest {

    private static final ContextLoader LOADER = new ContextLoader();

    private static SpecMetadata parse(String yaml) {
        Map<String, Object> map = yaml.isBlank() ? Map.of() : LOADER.fromYaml(yaml);
        return SpecMetadata.parse(map);
    }

    @Test
    void readsTitleAndIntro() {
        SpecMetadata metadata = parse("""
                title: "TableTest Core — Specification"
                intro: "Generated from the executable suite."
                """);

        assertThat(metadata.title()).isEqualTo("TableTest Core — Specification");
        assertThat(metadata.intro()).isEqualTo("Generated from the executable suite.");
        assertThat(metadata.chapters()).isEmpty();
    }

    @Test
    void emptyFileYieldsEmptyMetadata() {
        assertThat(parse("")).isEqualTo(SpecMetadata.EMPTY);
        assertThat(parse("").isEmpty()).isTrue();
    }

    @Test
    void blankTitleAndIntroReadAsAbsent() {
        SpecMetadata metadata = parse("""
                title: "   "
                intro: ""
                """);

        assertThat(metadata.title()).isNull();
        assertThat(metadata.intro()).isNull();
    }

    @Test
    void readsNestedChaptersInDeclaredOrder() {
        SpecMetadata metadata = parse("""
                title: "Formatter"
                chapters:
                  - name: formatter
                    title: "Table Formatter"
                    chapters:
                      - { name: extraction, title: "Value Extraction" }
                      - { name: displaywidth, title: "Display Width" }
                  - name: examples
                    title: "Worked Examples"
                """);

        assertThat(metadata.chapters())
                .containsExactly(
                        new ChapterMetadata(
                                "formatter",
                                "Table Formatter",
                                java.util.List.of(
                                        new ChapterMetadata("extraction", "Value Extraction", java.util.List.of()),
                                        new ChapterMetadata("displaywidth", "Display Width", java.util.List.of()))),
                        new ChapterMetadata("examples", "Worked Examples", java.util.List.of()));
    }

    @Test
    void chapterWithoutNameIsSkipped() {
        SpecMetadata metadata = parse("""
                chapters:
                  - title: "No name — cannot match a node"
                  - name: examples
                """);

        assertThat(metadata.chapters()).containsExactly(new ChapterMetadata("examples", null, java.util.List.of()));
    }

    @Test
    void chapterCanReorderWithoutRetitling() {
        SpecMetadata metadata = parse("""
                chapters:
                  - name: formatter
                  - name: examples
                """);

        assertThat(metadata.chapters())
                .containsExactly(
                        new ChapterMetadata("formatter", null, java.util.List.of()),
                        new ChapterMetadata("examples", null, java.util.List.of()));
    }
}
