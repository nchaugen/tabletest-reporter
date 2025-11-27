package io.github.nchaugen.tabletest.reporter;

import io.github.nchaugen.tabletest.reporter.pebble.PebbleExtension;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;

public enum ReportFormat {
    ASCIIDOC(".adoc"),
    MARKDOWN(".md");

    private static final PebbleEngine ENGINE = new PebbleEngine.Builder().extension(new PebbleExtension()).build();

    private static final PebbleTemplate ASCIIDOC_TABLE_TEMPLATE = ENGINE.getTemplate("table.adoc.peb");
    private static final PebbleTemplate MARKDOWN_TABLE_TEMPLATE = ENGINE.getTemplate("table.md.peb");

    private final String extension;

    ReportFormat(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

    public PebbleTemplate tableTemplate() {
        return switch (this) {
            case ASCIIDOC -> ASCIIDOC_TABLE_TEMPLATE;
            case MARKDOWN -> MARKDOWN_TABLE_TEMPLATE;
        };
    }
}
