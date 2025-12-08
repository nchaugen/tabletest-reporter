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
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.file.ProjectLayout;

import javax.inject.Inject;

/**
 * Gradle extension to configure TableTest Reporter.
 */
public abstract class TableTestReporterExtension {

    private final Property<String> format;
    private final DirectoryProperty inputDir;
    private final DirectoryProperty outputDir;

    @Inject
    public TableTestReporterExtension(ObjectFactory objects, ProjectLayout layout, ProviderFactory providers) {
        this.format = objects.property(String.class).convention("asciidoc");
        this.inputDir = objects.directoryProperty()
            .convention(layout.getBuildDirectory().dir("junit-jupiter"));
        this.outputDir = objects.directoryProperty()
            .convention(layout.getBuildDirectory().dir("generated-docs/tabletest"));
    }

    public Property<String> getFormat() {
        return format;
    }

    public DirectoryProperty getInputDir() {
        return inputDir;
    }

    public DirectoryProperty getOutputDir() {
        return outputDir;
    }
}
