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
package io.github.nchaugen.tabletest.reporter.pebble;

import io.pebbletemplates.pebble.error.PebbleException;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FilterReplicate implements Filter {

    public static final String NAME = "replicate";
    private static final String TIMES = "times";

    @Override
    public Object apply(
            Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber)
            throws PebbleException {
        Object value = args.get(TIMES);
        int times =
                switch (value) {
                    case null -> 2;
                    case Integer i -> i;
                    case Long l -> l.intValue();
                    default ->
                        throw new PebbleException(
                                null,
                                MessageFormat.format(
                                        "Unexpected value ''{0}'' for argument ''{1}'' in filter ''{2}''",
                                        value, TIMES, NAME),
                                lineNumber,
                                self.getName());
                };
        return Collections.nCopies(times, input);
    }

    @Override
    public List<String> getArgumentNames() {
        return List.of(TIMES);
    }
}
