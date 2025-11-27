package io.github.nchaugen.tabletest.reporter.pebble;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ReplicateFilter implements Filter {
    @Override
    public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException {
        Object value = args.get("times");
        int times = switch (value) {
            case null -> 2;
            case Integer i -> i;
            case Long l -> l.intValue();
            default ->
                throw new IllegalArgumentException("Unexpected value `" + value + "` for argument `times` in filter `replicate` (line " + lineNumber + ")");
        };
        return Collections.nCopies(times, input);
    }

    @Override
    public List<String> getArgumentNames() {
        return List.of("times");
    }
}
