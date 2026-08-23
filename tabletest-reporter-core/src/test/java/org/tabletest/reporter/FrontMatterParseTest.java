package org.tabletest.reporter;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Unpublished: internal parsing of the report-config sidecar, not a user-facing rule.
class FrontMatterParseTest {

    private static final ContextLoader LOADER = new ContextLoader();

    private static FrontMatter parse(String yaml) {
        Map<String, Object> map = yaml.isBlank() ? Map.of() : LOADER.fromYaml(yaml);
        return FrontMatter.parse(map);
    }

    @Test
    void fileWithoutAFrontMatterSectionDeclaresNone() {
        assertThat(parse("title: \"Core Spec\"")).isEqualTo(FrontMatter.NONE);
        assertThat(parse("").isPresent()).isFalse();
    }

    @Test
    void keepsLiteralEntriesInDeclaredOrder() {
        FrontMatter frontMatter = parse("""
                frontMatter:
                  layout: report
                  type: docs
                  draft: false
                """);

        assertThat(frontMatter.keys()).containsExactly("layout", "type", "draft");
    }

    @Test
    void theThreeDerivedKeysAreFilledByTheReporter() {
        FrontMatter frontMatter = parse("""
                frontMatter:
                  title: true
                  weight: true
                  generated: true
                """);

        List<Map<String, Object>> entries = frontMatter.entriesFor("Leap years", 3, "2026-08-23T09:19:33Z");

        assertThat(entries).extracting(entry -> entry.get("key")).containsExactly("title", "weight", "generated");
        assertThat(entries)
                .extracting(entry -> entry.get("value"))
                .containsExactly("Leap years", 3, "2026-08-23T09:19:33Z");
    }

    @Test
    void aDerivedNameCarryingItsOwnValueStaysLiteral() {
        FrontMatter frontMatter = parse("""
                frontMatter:
                  title: "A title of my own"
                """);

        assertThat(frontMatter.entriesFor("Leap years", 3, "2026-08-23T09:19:33Z"))
                .extracting(entry -> entry.get("value"))
                .containsExactly("A title of my own");
    }

    @Test
    void aDerivedValueTheReporterCannotSupplyDropsItsEntry() {
        FrontMatter frontMatter = parse("""
                frontMatter:
                  layout: report
                  weight: true
                """);

        assertThat(frontMatter.entriesFor("Leap years", null, "2026-08-23T09:19:33Z"))
                .extracting(entry -> entry.get("key"))
                .containsExactly("layout");
    }

    @Test
    void aPlainStringIsWrittenAsItStands() {
        FrontMatter frontMatter = parse("""
                frontMatter:
                  layout: report
                """);

        assertThat(frontMatter.entriesFor("Leap years", 1, null).get(0).get("yaml"))
                .isEqualTo("report");
    }

    @Test
    void aValueYamlWouldMisreadIsQuoted() {
        FrontMatter frontMatter = parse("""
                frontMatter:
                  layout: "report: the spec"
                  version: "2.0"
                  flag: "true"
                """);

        assertThat(frontMatter.entriesFor("Leap years", 1, null))
                .extracting(entry -> entry.get("yaml"))
                .containsExactly("\"report: the spec\"", "\"2.0\"", "\"true\"");
    }

    @Test
    void aStringIsQuotedForYamlAndLeftBareForAsciidoc() {
        FrontMatter frontMatter = parse("""
                frontMatter:
                  layout: "report: the spec"
                """);

        Map<String, Object> entry =
                frontMatter.entriesFor("Leap years", 1, null).get(0);

        assertThat(entry.get("value")).isEqualTo("report: the spec");
        assertThat(entry.get("yaml")).isEqualTo("\"report: the spec\"");
    }

    @Test
    void aNumberAndABooleanNeedNoQuoting() {
        FrontMatter frontMatter = parse("""
                frontMatter:
                  weight: true
                  draft: false
                """);

        assertThat(frontMatter.entriesFor("Leap years", 3, null))
                .extracting(entry -> entry.get("yaml"))
                .containsExactly("3", "false");
    }

    @Test
    void aQuoteInsideAValueNeedsNoQuoting() {
        FrontMatter frontMatter = parse("""
                frontMatter:
                  layout: 'the "spec" report'
                """);

        assertThat(frontMatter.entriesFor("Leap years", 1, null).get(0).get("yaml"))
                .isEqualTo("the \"spec\" report");
    }

    @Test
    void aValueOpeningWithAQuoteIsQuotedAndEscaped() {
        FrontMatter frontMatter = parse("""
                frontMatter:
                  layout: '"spec" report'
                """);

        assertThat(frontMatter.entriesFor("Leap years", 1, null).get(0).get("yaml"))
                .isEqualTo("\"\\\"spec\\\" report\"");
    }
}
