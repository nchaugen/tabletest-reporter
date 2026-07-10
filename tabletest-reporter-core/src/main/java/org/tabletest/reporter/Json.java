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

import java.util.Map;

/**
 * Minimal, dependency-free JSON encoder for the closed set of value types the search index is
 * built from (strings, numbers, booleans, null, maps, and iterables). The output is embedded
 * verbatim in a generated {@code .js} asset, so string escaping additionally neutralises the
 * two Unicode line separators (U+2028/U+2029) that are valid in JSON but terminate a
 * JavaScript string literal.
 */
final class Json {

    private static final int LINE_SEPARATOR = 0x2028;
    private static final int PARAGRAPH_SEPARATOR = 0x2029;

    private Json() {}

    static String encode(Object value) {
        StringBuilder out = new StringBuilder();
        write(out, value);
        return out.toString();
    }

    private static void write(StringBuilder out, Object value) {
        switch (value) {
            case null -> out.append("null");
            case String string -> writeString(out, string);
            case Boolean bool -> out.append(bool.toString());
            case Number number -> out.append(number.toString());
            case Map<?, ?> map -> writeObject(out, map);
            case Iterable<?> iterable -> writeArray(out, iterable);
            default -> writeString(out, value.toString());
        }
    }

    private static void writeObject(StringBuilder out, Map<?, ?> map) {
        out.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            writeString(out, String.valueOf(entry.getKey()));
            out.append(':');
            write(out, entry.getValue());
        }
        out.append('}');
    }

    private static void writeArray(StringBuilder out, Iterable<?> iterable) {
        out.append('[');
        boolean first = true;
        for (Object element : iterable) {
            if (!first) {
                out.append(',');
            }
            first = false;
            write(out, element);
        }
        out.append(']');
    }

    private static void writeString(StringBuilder out, String string) {
        out.append('"');
        for (int i = 0; i < string.length(); i++) {
            appendEscaped(out, string.charAt(i));
        }
        out.append('"');
    }

    private static void appendEscaped(StringBuilder out, char c) {
        switch (c) {
            case '"' -> out.append("\\\"");
            case '\\' -> out.append("\\\\");
            case '\b' -> out.append("\\b");
            case '\f' -> out.append("\\f");
            case '\n' -> out.append("\\n");
            case '\r' -> out.append("\\r");
            case '\t' -> out.append("\\t");
            default -> {
                if (c < 0x20 || c == LINE_SEPARATOR || c == PARAGRAPH_SEPARATOR) {
                    out.append(String.format("\\u%04x", (int) c));
                } else {
                    out.append(c);
                }
            }
        }
    }
}
