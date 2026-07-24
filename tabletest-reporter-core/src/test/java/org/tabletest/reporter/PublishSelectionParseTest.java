package org.tabletest.reporter;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Unpublished: internal parsing of the report-config sidecar, not a user-facing rule.
class PublishSelectionParseTest {

    private static final ContextLoader LOADER = new ContextLoader();

    private static PublishSelection parse(String yaml) {
        Map<String, Object> map = yaml.isBlank() ? Map.of() : LOADER.fromYaml(yaml);
        return PublishSelection.parse(map);
    }

    @Test
    void readsExcludedAndIncludedPathsInDeclaredOrder() {
        PublishSelection selection = parse("""
                publish:
                  exclude:
                    - parsing
                    - converting/convert-with
                  include:
                    - converting/convert-with/precedence
                """);

        assertThat(selection.exclude()).containsExactly("parsing", "converting/convert-with");
        assertThat(selection.include()).containsExactly("converting/convert-with/precedence");
    }

    @Test
    void fileWithoutAPublishSectionSelectsEverything() {
        assertThat(parse("""
                        title: "Core Spec"
                        """)).isEqualTo(PublishSelection.EMPTY);
    }

    @Test
    void emptyFileSelectsEverything() {
        assertThat(parse("")).isEqualTo(PublishSelection.EMPTY);
        assertThat(parse("").isEmpty()).isTrue();
    }

    @Test
    void excludingWithoutIncludingIsWellFormed() {
        PublishSelection selection = parse("""
                publish:
                  exclude: [parsing]
                """);

        assertThat(selection.exclude()).containsExactly("parsing");
        assertThat(selection.include()).isEmpty();
        assertThat(selection.isEmpty()).isFalse();
    }

    @Test
    void blankAndNonTextEntriesAreSkipped() {
        PublishSelection selection = parse("""
                publish:
                  exclude:
                    - parsing
                    - "   "
                    - 42
                """);

        assertThat(selection.exclude()).containsExactly("parsing");
    }

    @Test
    void aPublishSectionThatIsNotAMappingSelectsEverything() {
        assertThat(parse("""
                        publish: parsing
                        """)).isEqualTo(PublishSelection.EMPTY);
    }

    @Test
    void selectionIsImmutableFromTheListsItWasBuiltWith() {
        List<String> exclude = new java.util.ArrayList<>(List.of("parsing"));
        PublishSelection selection = new PublishSelection(exclude, List.of());

        exclude.add("features");

        assertThat(selection.exclude()).containsExactly("parsing");
    }
}
