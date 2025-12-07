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
import io.pebbletemplates.pebble.extension.AbstractExtension;
import io.pebbletemplates.pebble.extension.Filter;
import io.pebbletemplates.pebble.extension.Test;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.text.MessageFormat;
import java.util.Map;

public class PebbleExtension extends AbstractExtension {

    @Override
    public Map<String, Filter> getFilters() {
        return Map.of(
            FilterReplicate.NAME, new FilterReplicate(),
            FilterReplaceAll.NAME, new FilterReplaceAll(),
            FilterReplaceInMatch.NAME, new FilterReplaceInMatch()
        );
    }

    @Override
    public Map<String, Test> getTests() {
        return Map.of(TestSet.NAME, new TestSet());
    }

    public static void requireNotNull(Map<String, Object> args, String argument, int lineNumber, PebbleTemplate self) {
        if (args.get(argument) == null) {
            throw new PebbleException(
                null,
                MessageFormat.format("The argument ''{0}'' is required.", argument), lineNumber,
                self.getName()
            );
        }
    }

}
