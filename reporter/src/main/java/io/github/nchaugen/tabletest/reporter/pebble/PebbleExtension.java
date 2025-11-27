package io.github.nchaugen.tabletest.reporter.pebble;

import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;

import java.util.Map;

public class PebbleExtension extends AbstractExtension {

    @Override
    public Map<String, Filter> getFilters() {
        return Map.of(
            "replicate", new ReplicateFilter()
        );
    }

}
