package io.github.nchaugen.tabletest.reporter.rendering;

import io.github.nchaugen.tabletest.reporter.ContextLoader;
import io.github.nchaugen.tabletest.reporter.TemplateEngine;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.nchaugen.tabletest.reporter.BuiltInFormat.ASCIIDOC;
import static io.github.nchaugen.tabletest.reporter.rendering.AsciiDocValidator.assertValidAsciiDoc;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for GitHub issue #11: deeply nested map/collection combinations
 * must stay within AsciiDoctor's 4-colon description list delimiter limit.
 */
public class TableWithDeeplyNestedMixedCollectionTest {

    private final TemplateEngine templateEngine = new TemplateEngine();

    @Test
    void map_map_set_map_stays_within_colon_limit() {
        Map<String, Object> context = new ContextLoader().fromYaml("""
            title: Map map set map
            headers:
              - value: "a"
            rows:
              - - value:
                    outer:
                      mid: !!set
                        ? a: "1"
                        : !!null "null"
                        ? b: "2"
                        : !!null "null"
                        ? c: "3"
                        : !!null "null"
            """);

        String rendered = templateEngine.renderTable(ASCIIDOC, context);

        assertThat(rendered).isEqualTo("""
                == ++Map map set map++

                [%header,cols="1"]
                |===
                |++a++

                a|
                ++outer++::
                ++mid++:::
                * {empty}
                ++a++:::: ++1++
                * {empty}
                ++b++:::: ++2++
                * {empty}
                ++c++:::: ++3++

                |===
                """);
        assertValidAsciiDoc(rendered);
    }

    @Test
    void map_depth_cycles_colons_when_exceeding_three_levels() {
        Map<String, Object> context = new ContextLoader().fromYaml("""
            title: Map depth cycling
            headers:
              - value: "a"
            rows:
              - - value:
                    L1:
                      L2:
                        L3:
                          L4: "leaf"
            """);

        String rendered = templateEngine.renderTable(ASCIIDOC, context);

        assertThat(rendered).isEqualTo("""
                == ++Map depth cycling++

                [%header,cols="1"]
                |===
                |++a++

                a|
                ++L1++::
                ++L2++:::
                ++L3++::::
                ++L4++:: ++leaf++

                |===
                """);
        assertValidAsciiDoc(rendered);
    }
}
