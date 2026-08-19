package org.tabletest.reporter;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Unpublished: internal parsing of the report-config sidecar, not a user-facing rule.
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
        assertThat(metadata.features()).isEmpty();
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
    void readsNestedFeaturesInDeclaredOrder() {
        SpecMetadata metadata = parse("""
                title: "Formatter"
                features:
                  - name: formatter
                    title: "Table Formatter"
                    features:
                      - { name: extraction, title: "Value Extraction" }
                      - { name: displaywidth, title: "Display Width" }
                  - name: examples
                    title: "Worked Examples"
                """);

        assertThat(metadata.features())
                .containsExactly(
                        new FeatureMetadata(
                                "formatter",
                                "Table Formatter",
                                null,
                                java.util.List.of(
                                        new FeatureMetadata(
                                                "extraction", "Value Extraction", null, java.util.List.of()),
                                        new FeatureMetadata(
                                                "displaywidth", "Display Width", null, java.util.List.of()))),
                        new FeatureMetadata("examples", "Worked Examples", null, java.util.List.of()));
    }

    @Test
    void featureCarriesADescriptionForItsOwnIndexPage() {
        SpecMetadata metadata = parse("""
                features:
                  - name: pages
                    title: "Page contents"
                    description: >
                      What one page carries besides its table.
                """);

        assertThat(metadata.features())
                .containsExactly(new FeatureMetadata(
                        "pages", "Page contents", "What one page carries besides its table.\n", java.util.List.of()));
    }

    @Test
    void featureWithoutNameIsSkipped() {
        SpecMetadata metadata = parse("""
                features:
                  - title: "No name — cannot match a node"
                  - name: examples
                """);

        assertThat(metadata.features())
                .containsExactly(new FeatureMetadata("examples", null, null, java.util.List.of()));
    }

    @Test
    void featureCanReorderWithoutRetitling() {
        SpecMetadata metadata = parse("""
                features:
                  - name: formatter
                  - name: examples
                """);

        assertThat(metadata.features())
                .containsExactly(
                        new FeatureMetadata("formatter", null, null, java.util.List.of()),
                        new FeatureMetadata("examples", null, null, java.util.List.of()));
    }
}
