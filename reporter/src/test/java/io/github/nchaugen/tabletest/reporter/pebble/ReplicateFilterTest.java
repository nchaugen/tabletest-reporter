package io.github.nchaugen.tabletest.reporter.pebble;

import io.github.nchaugen.tabletest.junit.TableTest;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.loader.StringLoader;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReplicateFilterTest {

    private static final PebbleEngine ENGINE = new PebbleEngine.Builder()
        .extension(
            new AbstractExtension() {
                @Override
                public Map<String, Filter> getFilters() {
                    return Map.of(ReplicateFilter.FILTER_NAME, new ReplicateFilter());
                }
            })
        .loader(new StringLoader())
        .build();

    @TableTest("""
        Scenario             | Input            | Times | Result?
        Single quoted string | "'a'"            | 2     | [a, a]
        Double quoted string | '"a"'            | 2     | [a, a]
        Escaped string       | "'\\''"          | 2     | [&#39;, &#39;]
        Integer              | 2                | 3     | [2, 2, 2]
        Integer result       | (1 + 1)          | 3     | [2, 2, 2]
        Float                | 3.14             | 3     | [3.14, 3.14, 3.14]
        Boolean              | true             | 2     | [true, true]
        List                 | '[1,2,3]'        | 2     | [[1, 2, 3], [1, 2, 3]]
        Dictionary           | '{"a":1, "b":2}' | 2     | ['{a=1, b=2}', '{a=1, b=2}']
        Unknown literal type | a                | 1     | [null]
        Null                 | null             | 2     | [null, null]
        Null alias           | none             | 2     | [null, null]
        """)
    void should_replicate_all_literal_types(String input, int times, List<?> expected) throws IOException {
        StringWriter writer = new StringWriter();
        ENGINE.getTemplate("{{ %s | replicate(%d) }}".formatted(input, times)).evaluate(writer);
        assertThat(writer.toString()).isEqualTo(expected.toString());
    }

    @TableTest("""
        Scenario                     | Input | Times | Result?
        Times is a number            | 1     | 3     | [1, 1, 1]
        Times is null, use default   | 2     | null  | [2, 2]
        Times is string, use default | 3     | a     | [3, 3]
        """)
    void should_default_to_doubling_in_unknown_times(String input, Object times, List<?> expected) throws IOException {
        StringWriter writer = new StringWriter();
        ENGINE.getTemplate("{{ %s | replicate(%s) }}".formatted(input, times)).evaluate(writer);
        assertThat(writer.toString()).isEqualTo(expected.toString());
    }


}