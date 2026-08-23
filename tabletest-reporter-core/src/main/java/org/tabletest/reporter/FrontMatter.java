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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The front matter a project declares in the {@code frontMatter:} section of its
 * {@code tabletest-reporter.yaml} sidecar, written above every AsciiDoc and Markdown page so a site
 * generator can read it. Most entries are literal and identical on every page.
 *
 * <p>Three values the reporter knows and a generator cannot work out are asked for by token rather
 * than by key name: {@code $title}, {@code $position} and {@code $timestamp}. The key stays the
 * project's own, because generators do not agree on what to call these — a position is
 * {@code weight} to Hugo, {@code sidebar_position} to Docusaurus, {@code nav_order} to a Jekyll
 * theme, and {@code page-weight} to Antora, which exposes a custom attribute only under that
 * prefix. Write {@code $$} for a literal value that begins with a dollar sign.
 *
 * <p>Declared order is kept, so the generated block reads as it was written. An entry whose derived
 * value the reporter cannot supply for a page is dropped rather than written empty. The HTML format
 * writes no front matter at all — it is a complete page, not source for a generator.
 *
 * @param entries the declared keys in order, each mapped to a literal value or a {@link Derived}
 */
public record FrontMatter(Map<String, Object> entries) {

    /** A value the reporter fills in, asked for by its token rather than by the key it is put under. */
    public enum Derived {
        TITLE("$title"),
        POSITION("$position"),
        TIMESTAMP("$timestamp");

        private final String token;

        Derived(String token) {
            this.token = token;
        }

        static Derived of(String value) {
            return Arrays.stream(values())
                    .filter(derived -> derived.token.equals(value))
                    .findFirst()
                    .orElse(null);
        }
    }

    private static final Logger LOGGER = System.getLogger(FrontMatter.class.getName());

    /** Anything shaped like a token, so one the reporter does not know can be reported rather than written. */
    private static final Pattern TOKEN_SHAPED = Pattern.compile("\\$[a-zA-Z][a-zA-Z0-9-]*");

    /** A character that starts a YAML construct, or a separator that ends a plain scalar early. */
    private static final Pattern INDICATOR = Pattern.compile("^[-?:,\\[\\]{}#&*!|>'\"%@`]|: | #|[\\n\\t]");

    /** A plain value YAML would read back as a number, a boolean or a null rather than as text. */
    private static final Pattern SCALAR_LOOKALIKE =
            Pattern.compile("(?i)true|false|yes|no|on|off|null|~|[+-]?\\d+(\\.\\d+)?([eE][+-]?\\d+)?");

    /** The absent case: no front matter is written, and both text formats report as before. */
    public static final FrontMatter NONE = new FrontMatter(Map.of());

    public FrontMatter {
        entries = new LinkedHashMap<>(entries);
    }

    /**
     * Parses the front matter from a raw YAML map (as loaded by {@link ContextLoader}). A document
     * with no {@code frontMatter:} section yields {@link #NONE}.
     */
    public static FrontMatter parse(Map<String, Object> yaml) {
        if (yaml == null || !(yaml.get("frontMatter") instanceof Map<?, ?> declared)) {
            return NONE;
        }
        Map<String, Object> entries = new LinkedHashMap<>();
        declared.forEach((key, value) -> entries.put(String.valueOf(key), declaredValue(String.valueOf(key), value)));
        return new FrontMatter(entries);
    }

    /** True when at least one entry was declared, so the text formats have a block to write. */
    public boolean isPresent() {
        return !entries.isEmpty();
    }

    /** The declared keys, in the order they were written. */
    public List<String> keys() {
        return List.copyOf(entries.keySet());
    }

    /**
     * The entries for one page, each carrying the {@code key}, the {@code value} as written or as
     * derived, and a {@code yaml} scalar that is quoted and escaped where YAML needs it. An entry
     * whose derived value is null for this page is left out.
     *
     * @param title the page's title, for a {@code $title}
     * @param position the page's place among its siblings, for a {@code $position}
     * @param generatedAt the run timestamp, for a {@code $timestamp}
     */
    public List<Map<String, Object>> entriesFor(String title, Integer position, String generatedAt) {
        List<Map<String, Object>> rendered = new ArrayList<>();
        entries.forEach((key, declared) -> {
            Object value =
                    declared instanceof Derived derived ? derive(derived, title, position, generatedAt) : declared;
            if (value != null) {
                rendered.add(entry(key, value));
            }
        });
        return List.copyOf(rendered);
    }

    /**
     * The value a key was declared with: a {@link Derived} for one of the reporter's tokens, the
     * text after a leading {@code $$} for an escaped literal, and the value itself otherwise. A
     * value that looks like a token the reporter does not know is written as it stands, with a
     * warning, so a typo never fails a report.
     */
    private static Object declaredValue(String key, Object value) {
        if (!(value instanceof String text)) {
            return value;
        }
        if (text.startsWith("$$")) {
            return text.substring(1);
        }
        Derived derived = Derived.of(text);
        if (derived != null) {
            return derived;
        }
        if (TOKEN_SHAPED.matcher(text).matches()) {
            LOGGER.log(
                    Level.WARNING,
                    "Front matter key ''{0}'' asks for ''{1}'', which is not a value the reporter fills."
                            + " Write $$ to keep a literal dollar sign.",
                    key,
                    text);
        }
        return text;
    }

    private static Object derive(Derived derived, String title, Integer position, String generatedAt) {
        return switch (derived) {
            case TITLE -> title;
            case POSITION -> position;
            case TIMESTAMP -> generatedAt;
        };
    }

    private static Map<String, Object> entry(String key, Object value) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("key", key);
        entry.put("value", value);
        entry.put("yaml", yamlScalar(value));
        return entry;
    }

    /**
     * A YAML scalar for the value. A value is written as it stands wherever YAML reads it back as
     * the same string, and quoted where it would not — an empty value, one carrying a structural
     * character, or one a reader would take for a number or a boolean.
     */
    private static String yamlScalar(Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        String text = String.valueOf(value);
        return needsQuoting(text) ? quoted(text) : text;
    }

    private static boolean needsQuoting(String text) {
        return text.isEmpty()
                || !text.equals(text.strip())
                || INDICATOR.matcher(text).find()
                || SCALAR_LOOKALIKE.matcher(text).matches();
    }

    private static String quoted(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
