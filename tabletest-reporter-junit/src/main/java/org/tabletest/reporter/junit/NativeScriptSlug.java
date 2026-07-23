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
 * Reduces a name to a lowercase slug written in the script the name itself uses, for names
 * the ASCII fold has no answer for.
 * <p>
 * Letters, digits, combining marks and underscores of any script survive; every other run of
 * characters becomes a single hyphen. The result is composed so that the same name slugs to the
 * same string on a filesystem that stores decomposed and one that stores composed, and so that
 * two spellings of one name — halfwidth and fullwidth katakana, say — slug to one string.
 * <p>
 * A name holding no letter or digit at all still reduces to the empty string. Callers must
 * treat an empty slug as unusable rather than as a name.
 */
final class NativeScriptSlug {

    private NativeScriptSlug() {}

    static String of(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return joinRemainingWordsWithHyphen(compose(name.toLowerCase(Locale.ROOT)));
    }

    /**
     * Composes accented letters back into single characters where Unicode has a composed form,
     * and reduces compatibility forms to the characters they stand for, so a name does not slug
     * differently depending on the form its filesystem or its author's keyboard hands us.
     */
    private static String compose(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFKC);
    }

    private static String joinRemainingWordsWithHyphen(String name) {
        return name.replaceAll("[^\\p{L}\\p{M}\\p{N}_]+", "-").replaceAll("^-+|-+$", "");
    }
}
