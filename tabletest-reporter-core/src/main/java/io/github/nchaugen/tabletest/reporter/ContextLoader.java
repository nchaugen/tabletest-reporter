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

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static java.util.stream.Collectors.joining;

public class ContextLoader {

    private final Load yaml;

    public ContextLoader() {
        LoadSettings settings =
                LoadSettings.builder().setAllowNonScalarKeys(true).build();

        yaml = new Load(settings);
    }

    public Map<String, Object> fromYaml(Path path) {
        try (var lines = Files.lines(path, StandardCharsets.UTF_8)) {
            return fromYaml(lines.collect(joining("\n")));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read YAML from " + path, e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fromYaml(String value) {
        return (Map<String, Object>) yaml.loadFromString(value);
    }
}
