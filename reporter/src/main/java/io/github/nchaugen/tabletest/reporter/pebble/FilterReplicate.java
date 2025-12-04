package io.github.nchaugen.tabletest.reporter.pebble;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FilterReplicate implements Filter {

    public static final String NAME = "replicate";
    private static final String TIMES = "times";

    @Override
    public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException {
        Object value = args.get(TIMES);
        int times = switch (value) {
            case null -> 2;
            case Integer i -> i;
            case Long l -> l.intValue();
            default -> throw new PebbleException(
                null,
                MessageFormat.format("Unexpected value ''{0}'' for argument ''{1}'' in filter ''{2}''", value, TIMES, NAME),
                lineNumber,
                self.getName()
            );
        };
        return Collections.nCopies(times, input);
    }

    @Override
    public List<String> getArgumentNames() {
        return List.of(TIMES);
    }
}
