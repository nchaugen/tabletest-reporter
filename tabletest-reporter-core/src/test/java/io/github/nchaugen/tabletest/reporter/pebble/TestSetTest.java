package io.github.nchaugen.tabletest.reporter.pebble;

import io.github.nchaugen.tabletest.junit.TableTest;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Test;
import io.pebbletemplates.pebble.loader.StringLoader;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TestSetTest {

    private static final PebbleEngine ENGINE = new PebbleEngine.Builder()
            .extension(new AbstractExtension() {
                @Override
                public Map<String, Test> getTests() {
                    return Map.of(TestSet.NAME, new TestSet());
                }
            })
            .loader(new StringLoader())
            .build();

    @TableTest("""
        Scenario                | Input        | Result?
        Empty set is set        | {}           | set
        Set is set              | {1, 2, 3}    | set
        Nested set is set       | {{1, 2, 3}}  | set
        Empty list is not set   | []           | not set
        List is not set         | [1, 2, 3]    | not set
        Empty map is not set    | [:]          | not set
        Map is not set          | [a: 1, b: 2] | not set
        List of set is not set  | [{}, {1}]    | not set
        Empty scalar is not set | ""           | not set
        Scalar is not set       | abc          | not set
        null is not set         |              | not set
        """)
    void should_recognize_sets(Object input, String expected) throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("input", input);
        StringWriter writer = new StringWriter();
        ENGINE.getTemplate("{% if input is set %}set{% else %}not set{% endif %}")
                .evaluate(writer, context);
        assertThat(writer.toString()).isEqualTo(expected);
    }
}
