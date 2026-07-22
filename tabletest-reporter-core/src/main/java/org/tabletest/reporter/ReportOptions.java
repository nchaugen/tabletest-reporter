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

/**
 * The raw, partially-specified report options a single entry point (Maven mojo, CLI, or
 * Gradle task) collected before resolution. Every field is nullable: a null means "this
 * option was not set here, fall back to a lower layer or the built-in default". The
 * resolved, always-valid form is {@link ReportConfiguration}, produced by
 * {@link ReportConfigurationResolver}.
 *
 * @param format the requested output format name (e.g. "markdown"), or null for the default
 * @param templateDirectory a custom template directory, or null for the built-in templates
 * @param indexDepth the requested index depth ("1", "2", ..., "infinite"), or null for the default
 * @param singleFile whether to assemble a single-file report, or null for the default (false)
 */
public record ReportOptions(String format, Path templateDirectory, String indexDepth, Boolean singleFile) {}
