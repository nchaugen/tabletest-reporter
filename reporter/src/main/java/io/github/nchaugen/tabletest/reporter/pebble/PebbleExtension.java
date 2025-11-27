package io.github.nchaugen.tabletest.reporter.pebble;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.text.MessageFormat;
import java.util.Map;

public class PebbleExtension extends AbstractExtension {

    @Override
    public Map<String, Filter> getFilters() {
        return Map.of(
            ReplicateFilter.FILTER_NAME, new ReplicateFilter(),
            ReplaceAllFilter.FILTER_NAME, new ReplaceAllFilter(),
            ReplaceInMatchFilter.FILTER_NAME, new ReplaceInMatchFilter()
        );
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
