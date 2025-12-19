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
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.pebble.loader.DelegatingLoader;
import io.pebbletemplates.pebble.loader.FileLoader;
import io.pebbletemplates.pebble.loader.Loader;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Template rendering engine.
 *
 * Responsible for creating and configuring the Pebble template engine,
 * loading templates, and rendering them with context data.
 */
public final class TemplateEngine {

    private final PebbleTemplate asciidocTableTemplate;
    private final PebbleTemplate markdownTableTemplate;
    private final PebbleTemplate asciidocIndexTemplate;
    private final PebbleTemplate markdownIndexTemplate;

    public TemplateEngine() {
        this(new ClasspathLoader());
    }

    public TemplateEngine(Path customTemplateDirectory) {
        this(createDelegatingLoader(Objects.requireNonNull(customTemplateDirectory, "customTemplateDirectory")));
    }

    private TemplateEngine(Loader<?> loader) {
        PebbleEngine engine = createEngine(loader);
        this.asciidocTableTemplate = engine.getTemplate("table.adoc.peb");
        this.markdownTableTemplate = engine.getTemplate("table.md.peb");
        this.asciidocIndexTemplate = engine.getTemplate("index.adoc.peb");
        this.markdownIndexTemplate = engine.getTemplate("index.md.peb");
    }

    public String renderTable(ReportFormat format, Map<String, Object> context) {
        return render(tableTemplate(format), context);
    }

    public String renderIndex(ReportFormat format, Map<String, Object> context) {
        return render(indexTemplate(format), context);
    }

    private String render(PebbleTemplate template, Map<String, Object> context) {
        try {
            Writer writer = new StringWriter();
            template.evaluate(writer, context);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render template with context: " + context.get("title"), e);
        }
    }

    private PebbleTemplate tableTemplate(ReportFormat format) {
        return switch (format) {
            case ASCIIDOC -> asciidocTableTemplate;
            case MARKDOWN -> markdownTableTemplate;
        };
    }

    private PebbleTemplate indexTemplate(ReportFormat format) {
        return switch (format) {
            case ASCIIDOC -> asciidocIndexTemplate;
            case MARKDOWN -> markdownIndexTemplate;
        };
    }

    private static PebbleEngine createEngine(Loader<?> loader) {
        return new PebbleEngine.Builder()
            .loader(loader)
            .autoEscaping(false)
            .extension(new PebbleExtension())
            .build();
    }

    private static Loader<?> createDelegatingLoader(Path customTemplateDirectory) {
        FileLoader fileLoader = new FileLoader();
        fileLoader.setPrefix(customTemplateDirectory.toString());
        return new DelegatingLoader(Arrays.asList(fileLoader, new ClasspathLoader()));
    }
}
