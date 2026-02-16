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

import static java.util.Objects.requireNonNull;

/**
 * User-defined custom output format.
 *
 * <p>Custom formats are discovered from template directories containing
 * {@code table.{format}.peb} and {@code index.{format}.peb} files.
 *
 * <p>The format name serves as both the format identifier and the file extension.
 * For example, a format named "html" will produce files with the ".html" extension.
 */
public record CustomFormat(String name) implements Format {

    public CustomFormat {
        requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Format name cannot be blank");
        }
        if (name.startsWith(".")) {
            throw new IllegalArgumentException("Format name cannot start with a dot: " + name);
        }
    }

    @Override
    public String formatName() {
        return name;
    }

    @Override
    public String extension() {
        return "." + name;
    }
}
