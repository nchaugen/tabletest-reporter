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
import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * Latin letters that Unicode has no canonical decomposition for, and which the ASCII fold
     * would therefore drop. Ligatures expand to their component letters and stroked letters fold
     * to their base letter; {@code þ} takes a digraph because it has no Latin base letter at all.
     * Letters the fold already handles are deliberately absent — re-mapping them would move slugs
     * that already serve as published URLs.
     */
    private static final Map<String, String> LETTERS_WITHOUT_ASCII_FORM = Map.of(
            "ß", "ss",
            "æ", "ae",
            "œ", "oe",
            "ø", "o",
            "ł", "l",
            "đ", "d",
            "ð", "d",
            "þ", "th");

    private AsciiSlug() {}

    static String of(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return joinRemainingWordsWithHyphen(foldToAscii(name));
    }

    /**
     * Reduces every character to the simplest form Unicode records for it — accented letters to
     * base letter plus combining mark, compatibility forms such as {@code ﬁ}, {@code ②} and
     * fullwidth letters to the plain characters they stand for — then discards everything still
     * outside ASCII. The letters that have no such form are spelled out first, so the discard
     * never reaches them.
     */
    private static String foldToAscii(String name) {
        String simplified = Normalizer.normalize(name, Normalizer.Form.NFKD).toLowerCase(Locale.ROOT);
        return spellOutLettersWithoutAsciiForm(simplified).replaceAll("[^\\p{ASCII}]", "");
    }

    private static String spellOutLettersWithoutAsciiForm(String name) {
        return name.codePoints()
                .mapToObj(Character::toString)
                .map(letter -> LETTERS_WITHOUT_ASCII_FORM.getOrDefault(letter, letter))
                .collect(Collectors.joining());
    }

    private static String joinRemainingWordsWithHyphen(String ascii) {
        return ascii.replaceAll("[^a-z0-9_]+", "-").replaceAll("^-+|-+$", "");
    }
}
