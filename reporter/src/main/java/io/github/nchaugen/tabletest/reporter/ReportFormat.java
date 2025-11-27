package io.github.nchaugen.tabletest.reporter;

import io.github.nchaugen.tabletest.reporter.pebble.PebbleExtension;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Map;

public enum ReportFormat {
    ASCIIDOC(".adoc"),
    MARKDOWN(".md");

    private static final PebbleEngine ENGINE = new PebbleEngine.Builder()
        .autoEscaping(false)
        .extension(new PebbleExtension())
        .build();

    private static final PebbleTemplate ASCIIDOC_TABLE_TEMPLATE = ENGINE.getTemplate("table.adoc.peb");
    private static final PebbleTemplate MARKDOWN_TABLE_TEMPLATE = ENGINE.getTemplate("table.md.peb");

    private final String extension;

    ReportFormat(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

    public String renderTable(Map<String, Object> context) {
        try {
            Writer writer = new StringWriter();
            tableTemplate().evaluate(writer, context);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render table in " + this + " with context: " + context.get("title"), e);
        }
    }

    private PebbleTemplate tableTemplate() {
        return switch (this) {
            case ASCIIDOC -> ASCIIDOC_TABLE_TEMPLATE;
            case MARKDOWN -> MARKDOWN_TABLE_TEMPLATE;
        };
    }
}
