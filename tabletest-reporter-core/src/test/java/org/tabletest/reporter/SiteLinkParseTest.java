package org.tabletest.reporter;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Unpublished: internal parsing of the report-config sidecar, not a user-facing rule.
class SiteLinkParseTest {

    private static final ContextLoader LOADER = new ContextLoader();

    private static SiteLink parse(String yaml) {
        Map<String, Object> map = yaml.isBlank() ? Map.of() : LOADER.fromYaml(yaml);
        return SiteLink.parse(map);
    }

    @Test
    void readsLabelAndUrl() {
        SiteLink site = parse("""
                site:
                  label: "TableTest"
                  url: "https://tabletest.org/"
                """);

        assertThat(site.label()).isEqualTo("TableTest");
        assertThat(site.url()).isEqualTo("https://tabletest.org/");
        assertThat(site.isPresent()).isTrue();
    }

    @Test
    void urlWithoutLabelLabelsItselfWithTheUrl() {
        SiteLink site = parse("""
                site:
                  url: "https://tabletest.org/"
                """);

        assertThat(site.label()).isEqualTo("https://tabletest.org/");
    }

    @Test
    void labelWithoutUrlYieldsNoLink() {
        assertThat(parse("""
                site:
                  label: "TableTest"
                """)).isEqualTo(SiteLink.NONE);
    }

    @Test
    void blankUrlReadsAsAbsent() {
        assertThat(parse("""
                site:
                  label: "TableTest"
                  url: "   "
                """)).isEqualTo(SiteLink.NONE);
    }

    @Test
    void fileWithoutASiteSectionYieldsNoLink() {
        assertThat(parse("""
                title: "Core Spec"
                """)).isEqualTo(SiteLink.NONE);
        assertThat(parse("").isPresent()).isFalse();
    }

    @Test
    void absentLinkRendersNothingIntoTheContext() {
        assertThat(SiteLink.NONE.toMap()).isNull();
    }

    @Test
    void presentLinkReachesTheContextAsLabelAndUrl() {
        SiteLink site = new SiteLink("TableTest", "https://tabletest.org/");

        assertThat(site.toMap())
                .containsExactly(Map.entry("label", "TableTest"), Map.entry("url", "https://tabletest.org/"));
    }
}
