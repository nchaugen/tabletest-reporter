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
package io.github.nchaugen.tabletest.maven;

import io.github.nchaugen.tabletest.reporter.FormatLister;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lists all available output formats.
 *
 * <p>Displays built-in formats (asciidoc, markdown) and custom formats
 * if a template directory is configured.
 */
@Mojo(name = "list-formats", requiresProject = false, threadSafe = true)
public final class ListFormatsMojo extends AbstractMojo {

    @Parameter(property = "tabletest.report.templateDirectory")
    private File templateDirectory;

    @Override
    public void execute() {
        Path templateDir = resolveTemplateDirectory();
        String formats = FormatLister.listFormats(templateDir);

        getLog().info("Available output formats:");
        for (String format : formats.split(System.lineSeparator())) {
            getLog().info("  " + format);
        }
    }

    private Path resolveTemplateDirectory() {
        if (templateDirectory == null) {
            return null;
        }

        Path templateDir = templateDirectory.toPath();
        if (!Files.exists(templateDir) || !Files.isDirectory(templateDir)) {
            return null;
        }

        return templateDir;
    }
}
