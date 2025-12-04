package io.github.nchaugen.tabletest.reporter.pebble;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Test;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestSet implements Test {
    public static final String NAME = "set";

    @Override
    public boolean apply(
        Object input,
        Map<String, Object> args,
        PebbleTemplate self,
        EvaluationContext context,
        int lineNumber
    ) throws PebbleException {
        return input instanceof Set;
    }

    @Override
    public List<String> getArgumentNames() {
        return List.of();
    }
}
