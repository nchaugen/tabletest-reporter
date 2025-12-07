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

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static java.util.stream.Collectors.joining;

public class ContextLoader {

    private final Yaml yaml;

    public ContextLoader() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.DOUBLE_QUOTED);
        options.setIndent(2);
        options.setSplitLines(false);
        options.setDereferenceAliases(true);
        options.setAllowUnicode(true);
        options.setLineBreak(DumperOptions.LineBreak.getPlatformLineBreak());
        options.setPrettyFlow(true);

        yaml = new Yaml(options);
    }

    public Map<String, Object> fromYaml(Path path) {
        try (var lines = Files.lines(path, StandardCharsets.UTF_8)){
            return fromYaml(lines.collect(joining("\n")));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read YAML from " + path, e);
        }
    }

    public Map<String, Object> fromYaml(String value) {
        return yaml.load(value);
    }
}
