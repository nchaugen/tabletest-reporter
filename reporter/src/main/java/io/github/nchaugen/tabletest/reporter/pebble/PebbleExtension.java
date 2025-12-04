package io.github.nchaugen.tabletest.reporter.pebble;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.Test;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.text.MessageFormat;
import java.util.Map;

public class PebbleExtension extends AbstractExtension {

    @Override
    public Map<String, Filter> getFilters() {
        return Map.of(
            FilterReplicate.NAME, new FilterReplicate(),
            FilterReplaceAll.NAME, new FilterReplaceAll(),
            FilterReplaceInMatch.NAME, new FilterReplaceInMatch()
        );
    }

    @Override
    public Map<String, Test> getTests() {
        return Map.of(TestSet.NAME, new TestSet());
    }

    public static void requireNotNull(Map<String, Object> args, String argument, int lineNumber, PebbleTemplate self) {
        if (args.get(argument) == null) {
            throw new PebbleException(
                null,
                MessageFormat.format("The argument ''{0}'' is required.", argument), lineNumber,
                self.getName()
            );
        }
    }

}
