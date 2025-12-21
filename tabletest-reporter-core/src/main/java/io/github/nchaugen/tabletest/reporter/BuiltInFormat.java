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

/**
 * Built-in output formats for generated documentation.
 *
 * <p>For custom user-defined formats, see {@link CustomFormat}.
 */
public enum BuiltInFormat implements Format {
    ASCIIDOC("asciidoc", ".adoc"),
    MARKDOWN("markdown", ".md");

    private final String formatName;
    private final String extension;

    BuiltInFormat(String formatName, String extension) {
        this.formatName = formatName;
        this.extension = extension;
    }

    @Override
    public String formatName() {
        return formatName;
    }

    @Override
    public String extension() {
        return extension;
    }
}
