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
package org.tabletest.reporter.junit;

import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ArgumentConverter;

import java.util.List;

/**
 * Hands a {@link Lines} column's cell to the test as the text its lines make up, or as the lines
 * themselves, according to the parameter's own type. A cell holding a single value counts as one
 * line, so a one-line block needs no list notation.
 */
public class LinesConverter implements ArgumentConverter {

    @Override
    public Object convert(Object source, ParameterContext context) throws ArgumentConversionException {
        Class<?> parameterType = context.getParameter().getType();
        if (source == null) {
            return null;
        }
        if (String.class.equals(parameterType)) {
            return toText(source);
        }
        if (parameterType.isAssignableFrom(List.class)) {
            return toLines(source);
        }
        throw new ArgumentConversionException(
                "@Lines parameter must be a String or a List, but was " + parameterType.getTypeName());
    }

    /**
     * @return the lines of the cell, one per element, or the whole cell as a single line.
     */
    static List<String> toLines(Object source) {
        return source instanceof List<?> lines
                ? lines.stream().map(String::valueOf).toList()
                : List.of(String.valueOf(source));
    }

    /**
     * @return the text the cell's lines make up, joined by newlines.
     */
    static String toText(Object source) {
        return String.join("\n", toLines(source));
    }
}
