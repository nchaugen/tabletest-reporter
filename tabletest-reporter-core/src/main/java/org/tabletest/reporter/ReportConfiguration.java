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

import java.nio.file.Path;
import java.time.Instant;

/**
 * The resolved, always-valid set of report-level options that drive a single report run:
 * the output format, an optional custom template directory, the index depth, whether to
 * assemble a single-file report, and the report-level curation read from the sidecar file. Produced from the raw {@link ReportOptions} of an entry
 * point by {@link ReportConfigurationResolver}; this is the shared value object that the
 * Maven mojo, CLI, and Gradle task all feed the reporter from.
 *
 * @param format the resolved output format
 * @param templateDirectory a validated custom template directory, or null for built-in templates
 * @param indexDepth the resolved index depth
 * @param singleFile whether to assemble the report into a single self-contained file
 * @param specMetadata the resolved spec-level curation, or {@link SpecMetadata#EMPTY} when none
 * @param publishSelection the resolved page selection, or {@link PublishSelection#EMPTY} to publish
 *     every page
 * @param siteLink the link back to the site hosting the report, or {@link SiteLink#NONE} when the
 *     report links nowhere
 * @param frontMatter the front matter for the text formats, or {@link FrontMatter#NONE} when none
 * @param generatedAt the instant the report states it was generated at, or null to read the clock
 *     when the report runs
 */
public record ReportConfiguration(
        Format format,
        Path templateDirectory,
        IndexDepth indexDepth,
        boolean singleFile,
        SpecMetadata specMetadata,
        PublishSelection publishSelection,
        SiteLink siteLink,
        FrontMatter frontMatter,
        Instant generatedAt) {

    /** A configuration that leaves the report to stamp itself with the instant it runs. */
    public ReportConfiguration(
            Format format,
            Path templateDirectory,
            IndexDepth indexDepth,
            boolean singleFile,
            SpecMetadata specMetadata,
            PublishSelection publishSelection,
            SiteLink siteLink,
            FrontMatter frontMatter) {
        this(
                format,
                templateDirectory,
                indexDepth,
                singleFile,
                specMetadata,
                publishSelection,
                siteLink,
                frontMatter,
                null);
    }
}
