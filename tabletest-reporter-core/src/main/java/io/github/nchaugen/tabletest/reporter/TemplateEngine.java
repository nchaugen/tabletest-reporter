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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * Template rendering engine.
 * Responsible for creating and configuring the Pebble template engine,
 * loading templates, and rendering them with context data.
 */
public final class TemplateEngine {

    private final PebbleEngine engine;
    private final Path customTemplateDirectory;
    private final PebbleTemplate asciidocTableTemplate;
    private final PebbleTemplate markdownTableTemplate;
    private final PebbleTemplate asciidocIndexTemplate;
    private final PebbleTemplate markdownIndexTemplate;
    private final Map<String, PebbleTemplate> customTableTemplates;
    private final Map<String, PebbleTemplate> customIndexTemplates;

    public TemplateEngine() {
        this(new ClasspathLoader(), null);
    }

    public TemplateEngine(Path customTemplateDirectory) {
        this(createDelegatingLoader(requireNonNull(customTemplateDirectory, "customTemplateDirectory")),
            customTemplateDirectory);
    }

    private TemplateEngine(Loader<?> loader, Path customTemplateDirectory) {
        this.engine = createEngine(loader);
        this.customTemplateDirectory = customTemplateDirectory;
        this.customTableTemplates = new HashMap<>();
        this.customIndexTemplates = new HashMap<>();

        String asciidocTableName = discoverTemplate(customTemplateDirectory, "table.adoc.peb", "*-table.adoc.peb");
        String markdownTableName = discoverTemplate(customTemplateDirectory, "table.md.peb", "*-table.md.peb");
        String asciidocIndexName = discoverTemplate(customTemplateDirectory, "index.adoc.peb", "*-index.adoc.peb");
        String markdownIndexName = discoverTemplate(customTemplateDirectory, "index.md.peb", "*-index.md.peb");

        this.asciidocTableTemplate = engine.getTemplate(asciidocTableName);
        this.markdownTableTemplate = engine.getTemplate(markdownTableName);
        this.asciidocIndexTemplate = engine.getTemplate(asciidocIndexName);
        this.markdownIndexTemplate = engine.getTemplate(markdownIndexName);
    }

    public String renderTable(Format format, Map<String, Object> context) {
        return render(tableTemplate(format), context);
    }

    public String renderIndex(Format format, Map<String, Object> context) {
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

    private PebbleTemplate tableTemplate(Format format) {
        if (format instanceof BuiltInFormat reportFormat) {
            return switch (reportFormat) {
                case ASCIIDOC -> asciidocTableTemplate;
                case MARKDOWN -> markdownTableTemplate;
            };
        }
        return customTableTemplates.computeIfAbsent(format.formatName(),
            name -> loadCustomTemplate("table." + name + ".peb"));
    }

    private PebbleTemplate indexTemplate(Format format) {
        if (format instanceof BuiltInFormat reportFormat) {
            return switch (reportFormat) {
                case ASCIIDOC -> asciidocIndexTemplate;
                case MARKDOWN -> markdownIndexTemplate;
            };
        }
        return customIndexTemplates.computeIfAbsent(format.formatName(),
            name -> loadCustomTemplate("index." + name + ".peb"));
    }

    private PebbleTemplate loadCustomTemplate(String templateName) {
        if (customTemplateDirectory == null) {
            throw new IllegalStateException("Cannot load custom template '" + templateName +
                "' without a custom template directory");
        }

        Path templatePath = customTemplateDirectory.resolve(templateName);
        if (!Files.isRegularFile(templatePath)) {
            throw new IllegalArgumentException("Custom template not found: " + templatePath);
        }

        return engine.getTemplate(templateName);
    }

    private String discoverTemplate(Path customTemplateDirectory, String defaultName, String pattern) {
        if (customTemplateDirectory == null) {
            return defaultName;
        }

        // Check for exact match first (replacement)
        Path exactMatch = customTemplateDirectory.resolve(defaultName);
        if (Files.exists(exactMatch)) {
            return defaultName;
        }

        // Look for pattern match (extension)
        try (Stream<Path> files = Files.list(customTemplateDirectory)) {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            return files
                .filter(Files::isRegularFile)
                .filter(p -> matcher.matches(p.getFileName()))
                .sorted()
                .findFirst()
                .map(p -> p.getFileName().toString())
                .orElse(defaultName);
        } catch (IOException e) {
            return defaultName;
        }
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
