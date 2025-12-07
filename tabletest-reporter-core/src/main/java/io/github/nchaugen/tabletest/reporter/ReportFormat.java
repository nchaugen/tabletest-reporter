/*
 * Copyright 2025-present Nils Christian Haugen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    private static final PebbleTemplate ASCIIDOC_INDEX_TEMPLATE = ENGINE.getTemplate("index.adoc.peb");
    private static final PebbleTemplate MARKDOWN_INDEX_TEMPLATE = ENGINE.getTemplate("index.md.peb");

    private final String extension;

    ReportFormat(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

    public String renderTable(Map<String, Object> context) {
        return render(tableTemplate(), context);
    }

    public String renderIndex(Map<String, Object> context) {
        return render(indexTemplate(), context);
    }

    private String render(PebbleTemplate tableTemplate, Map<String, Object> context) {
        try {
            Writer writer = new StringWriter();
            tableTemplate.evaluate(writer, context);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render file in " + this + " with context: " + context.get("title"), e);
        }
    }

    private PebbleTemplate tableTemplate() {
        return switch (this) {
            case ASCIIDOC -> ASCIIDOC_TABLE_TEMPLATE;
            case MARKDOWN -> MARKDOWN_TABLE_TEMPLATE;
        };
    }

    private PebbleTemplate indexTemplate() {
        return switch (this) {
            case ASCIIDOC -> ASCIIDOC_INDEX_TEMPLATE;
            case MARKDOWN -> MARKDOWN_INDEX_TEMPLATE;
        };
    }
}
