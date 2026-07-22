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
package org.tabletest.reporter;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves the raw {@link ReportOptions} an entry point collected into a validated
 * {@link ReportConfiguration}, applying built-in defaults, parsing the index depth,
 * validating any custom template directory, and resolving the output format. This is the
 * single resolution the Maven mojo, CLI, and Gradle task share instead of each reinventing
 * it. Bad input (an unknown format, a missing or non-directory template path) raises
 * {@link IllegalArgumentException}, which every entry point normalises to its own failure
 * type.
 */
public final class ReportConfigurationResolver {

    private ReportConfigurationResolver() {}

    /**
     * Resolves report options into a validated configuration.
     *
     * @param options the raw options collected by an entry point
     * @return the resolved, always-valid configuration
     * @throws IllegalArgumentException if the format is unknown or the template directory is invalid
     */
    public static ReportConfiguration resolve(ReportOptions options) {
        Path templateDirectory = validatedTemplateDirectory(options.templateDirectory());
        Format format = FormatResolver.resolve(options.format(), templateDirectory);
        IndexDepth indexDepth = IndexDepth.parse(options.indexDepth());
        boolean singleFile = Boolean.TRUE.equals(options.singleFile());
        SpecMetadata specMetadata = SpecMetadataResolver.resolve(options.configFile());
        return new ReportConfiguration(format, templateDirectory, indexDepth, singleFile, specMetadata);
    }

    private static Path validatedTemplateDirectory(Path templateDirectory) {
        if (templateDirectory == null) {
            return null;
        }
        if (!Files.exists(templateDirectory)) {
            throw new IllegalArgumentException(
                    "Template directory does not exist: " + templateDirectory.toAbsolutePath());
        }
        if (!Files.isDirectory(templateDirectory)) {
            throw new IllegalArgumentException(
                    "Template path is not a directory: " + templateDirectory.toAbsolutePath());
        }
        return templateDirectory;
    }
}
