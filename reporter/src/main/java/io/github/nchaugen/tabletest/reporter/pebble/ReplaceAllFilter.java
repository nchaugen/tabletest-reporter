package io.github.nchaugen.tabletest.reporter.pebble;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.List;
import java.util.Map;

import static io.github.nchaugen.tabletest.reporter.pebble.PebbleExtension.requireNotNull;

public class ReplaceAllFilter implements Filter {

    public static final String FILTER_NAME = "replaceAll";
    private static final String REPLACE_PAIRS = "replace_pairs";

    @Override
    public Object apply(Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) throws PebbleException {
        if (input == null) return null;
        requireNotNull(args, REPLACE_PAIRS, lineNumber, self);

        Map<?, ?> replacePair = (Map<?, ?>) args.get(REPLACE_PAIRS);
        String data = input.toString();
        for (Map.Entry<?, ?> entry : replacePair.entrySet()) {
            data = data.replaceAll(entry.getKey().toString(), entry.getValue().toString());
        }

        return data;
    }

    @Override
    public List<String> getArgumentNames() {
        return List.of(REPLACE_PAIRS);
    }
}
