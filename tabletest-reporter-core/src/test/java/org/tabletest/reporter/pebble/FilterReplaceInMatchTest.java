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

class FilterReplaceInMatchTest {

    private static final PebbleEngine ENGINE = new PebbleEngine.Builder()
            .extension(new AbstractExtension() {
                @Override
                public Map<String, Filter> getFilters() {
                    return Map.of(FilterReplaceInMatch.NAME, new FilterReplaceInMatch());
                }
            })
            .loader(new StringLoader())
            .build();

    @TableTest("""
        Scenario       | Input   | Pattern                   | Replacements                    | Result?
        Single tab     | '\t'    | '(\t+)|([ \t]{2,})'       | "{'\t': '&tab;', ' ': '&spc;'}" | &tab;
        Multiple tab   | '\t\t'  | '(\t+)|([ \t]{2,})'       | "{'\t': '&tab;', ' ': '&spc;'}" | &tab;&tab;
        Tab and space  | ' \t '  | '(\t+)|([ \t]{2,})'       | "{'\t': '&tab;', ' ': '&spc;'}" | &spc;&tab;&spc;
        Single space   | 'A B'   | '(^ +)|( +$)|([ \t]{2,})' | "{'\t': '&tab;', ' ': '&spc;'}" | A B
        Multiple space | 'A   B' | '(^ +)|( +$)|([ \t]{2,})' | "{'\t': '&tab;', ' ': '&spc;'}" | A&spc;&spc;&spc;B
        Leading space  | '  A'   | '(^ +)|( +$)|([ \t]{2,})' | "{'\t': '&tab;', ' ': '&spc;'}" | &spc;&spc;A
        Trailing space | 'A  '   | '(^ +)|( +$)|([ \t]{2,})' | "{'\t': '&tab;', ' ': '&spc;'}" | A&spc;&spc;
        """)
    void should_encode_explicit_whitespace(String input, String pattern, String replacements, String expected)
            throws IOException {
        StringWriter writer = new StringWriter();
        ENGINE.getTemplate("{{ '%s' | replaceInMatch('%s', %s) | raw }}".formatted(input, pattern, replacements))
                .evaluate(writer);
        assertThat(writer.toString()).isEqualTo(expected);
    }
}
