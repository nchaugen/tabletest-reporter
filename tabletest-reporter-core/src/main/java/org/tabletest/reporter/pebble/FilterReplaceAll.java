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

import java.util.List;
import java.util.Map;

import static org.tabletest.reporter.pebble.PebbleExtension.requireNotNull;

public class FilterReplaceAll implements Filter {

    public static final String NAME = "replaceAll";
    private static final String REPLACE_PAIRS = "replace_pairs";

    @Override
    public Object apply(
            Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber)
            throws PebbleException {
        if (input == null) return null;
        requireNotNull(args, REPLACE_PAIRS, lineNumber, self);

        Map<?, ?> replacePair = (Map<?, ?>) args.get(REPLACE_PAIRS);
        String data = input.toString();
        for (Map.Entry<?, ?> entry : replacePair.entrySet()) {
            data = data.replaceAll(entry.getKey().toString(), entry.getValue().toString());
        }

        return data;
    }

    @Override
    public List<String> getArgumentNames() {
        return List.of(REPLACE_PAIRS);
    }
}
