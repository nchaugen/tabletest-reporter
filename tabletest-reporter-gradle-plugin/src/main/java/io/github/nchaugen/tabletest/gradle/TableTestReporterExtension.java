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
package io.github.nchaugen.tabletest.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;

import javax.inject.Inject;

/**
 * Gradle extension to configure TableTest Reporter.
 */
public abstract class TableTestReporterExtension {

    private final Property<String> format;
    private final DirectoryProperty inputDir;
    private final DirectoryProperty outputDir;
    private final DirectoryProperty templateDir;

    /**
     * Creates a new extension instance with default configuration values.
     *
     * @param objects Gradle object factory for creating properties
     * @param layout project layout for resolving build directory paths
     * @param providers provider factory for creating providers
     */
    @Inject
    public TableTestReporterExtension(ObjectFactory objects, ProjectLayout layout, ProviderFactory providers) {
        this.format = objects.property(String.class).convention("asciidoc");
        this.inputDir = objects.directoryProperty()
                .convention(layout.getBuildDirectory().dir("junit-jupiter"));
        this.outputDir = objects.directoryProperty()
                .convention(layout.getBuildDirectory().dir("generated-docs/tabletest"));
        this.templateDir = objects.directoryProperty();
    }

    /**
     * Returns the output format property.
     *
     * @return property for specifying output format (asciidoc or markdown)
     */
    public Property<String> getFormat() {
        return format;
    }

    /**
     * Returns the input directory property.
     *
     * @return property for directory containing TableTest YAML files
     */
    public DirectoryProperty getInputDir() {
        return inputDir;
    }

    /**
     * Returns the output directory property.
     *
     * @return property for directory where generated documentation will be written
     */
    public DirectoryProperty getOutputDir() {
        return outputDir;
    }

    /**
     * Returns the template directory property.
     *
     * @return property for optional custom template directory
     */
    public DirectoryProperty getTemplateDir() {
        return templateDir;
    }
}
