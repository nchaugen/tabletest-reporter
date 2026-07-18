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

import io.pebbletemplates.pebble.extension.Test;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Pebble test ({@code value is whitespaceSignificant}) deciding whether a literal
 * contains whitespace or boundaries a reader cannot see in rendered HTML: leading or
 * trailing whitespace, tabs, runs of two or more spaces, or pipe characters. Templates
 * use it to give such literals a visible extent.
 */
public class TestWhitespaceSignificant implements Test {
    public static final String NAME = "whitespaceSignificant";

    private static final Pattern SIGNIFICANT = Pattern.compile("^[ \\t]|[ \\t]$|\\t|[ \\t]{2,}|\\|");

    @Override
    public boolean apply(
            Object input, Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
        return input instanceof String value && SIGNIFICANT.matcher(value).find();
    }

    @Override
    public List<String> getArgumentNames() {
        return List.of();
    }
}
