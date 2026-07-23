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

import java.text.Normalizer;
import java.util.Locale;

/**
 * Reduces a name to a lowercase ASCII slug usable as both a filename and a URL segment.
 * <p>
 * Accented letters fold to their base letter, letters with no ASCII equivalent are dropped,
 * and every other run of characters outside {@code [a-z0-9_]} becomes a single hyphen.
 * Underscores survive as written, so a snake_case name keeps its shape.
 * <p>
 * A name written wholly in a script with no ASCII equivalent (Greek, Cyrillic, CJK) reduces
 * to the empty string. Callers must treat an empty slug as unusable rather than as a name.
 */
final class AsciiSlug {

    private AsciiSlug() {}

    static String of(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return joinRemainingWordsWithHyphen(foldToAscii(name).toLowerCase(Locale.ROOT));
    }

    /**
     * Splits accented letters into base letter plus combining mark, then discards everything
     * still outside ASCII — both the marks and any letter that has no ASCII form of its own.
     */
    private static String foldToAscii(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }

    private static String joinRemainingWordsWithHyphen(String ascii) {
        return ascii.replaceAll("[^a-z0-9_]+", "-").replaceAll("^-+|-+$", "");
    }
}
