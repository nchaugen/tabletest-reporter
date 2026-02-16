package org.tabletest.reporter.pebble;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.loader.StringLoader;
import org.tabletest.junit.TableTest;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FilterReplaceAllTest {

    private static final PebbleEngine ENGINE = new PebbleEngine.Builder()
            .extension(new AbstractExtension() {
                @Override
                public Map<String, Filter> getFilters() {
                    return Map.of(FilterReplaceAll.NAME, new FilterReplaceAll());
                }
            })
            .loader(new StringLoader())
            .build();

    @TableTest("""
        Scenario | Input  | Replacements                | Result?
        Tab      | '\t'   | "{'\t': 'tab', '\\|': 'P'}" | tab
        Pipe     | '|'    | "{'\t': 'tab', '\\|': 'P'}" | P
        Combo    | '|\t|' | "{'\t': 'tab', '\\|': 'P'}" | PtabP
        """)
    void should_replace_all(String input, String replacements, String expected) throws IOException {
        StringWriter writer = new StringWriter();
        ENGINE.getTemplate("{{ '%s' | replaceAll(%s) }}".formatted(input, replacements))
                .evaluate(writer);
        assertThat(writer.toString()).isEqualTo(expected);
    }
}
