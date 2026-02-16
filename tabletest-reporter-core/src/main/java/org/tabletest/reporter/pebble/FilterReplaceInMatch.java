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
package org.tabletest.reporter.pebble;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.tabletest.reporter.pebble.PebbleExtension.requireNotNull;

public class FilterReplaceInMatch implements Filter {

    public static final String NAME = "replaceInMatch";
    private static final String PATTERN = "pattern";
    private static final String REPLACE_PAIRS = "replace_pairs";
    private static final String PASSTHROUGH_MARKER = "passthrough_marker";

    @Override
    public Object apply(
            Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber)
            throws PebbleException {
        if (input == null) return null;
        requireNotNull(args, PATTERN, lineNumber, self);
        requireNotNull(args, REPLACE_PAIRS, lineNumber, self);

        String pattern = (String) args.get(PATTERN);
        Map<?, ?> replacePairs = (Map<?, ?>) args.get(REPLACE_PAIRS);
        String passthroughMarker = (String) args.getOrDefault(PASSTHROUGH_MARKER, "");

        return Arrays.stream(input.toString().splitWithDelimiters(pattern, -1))
                .filter(it -> !it.isEmpty())
                .map(it -> it.matches(pattern)
                        ? encodeCharacters(it, replacePairs)
                        : passthroughMarker + it + passthroughMarker)
                .collect(Collectors.joining());
    }

    private static String encodeCharacters(String data, Map<?, ?> replacePairs) {
        for (Map.Entry<?, ?> entry : replacePairs.entrySet()) {
            data = data.replaceAll(entry.getKey().toString(), entry.getValue().toString());
        }
        return data;
    }

    @Override
    public List<String> getArgumentNames() {
        return List.of(PATTERN, REPLACE_PAIRS, PASSTHROUGH_MARKER);
    }
}
