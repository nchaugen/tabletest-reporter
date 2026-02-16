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

import java.util.function.Function;

/**
 * Splits camelCase and PascalCase names into separate words.
 * <p>
 * Detects word boundaries in camelCase/PascalCase naming conventions and
 * inserts separators at appropriate positions. Handles acronyms correctly
 * (e.g., "XMLParser" → "XML", "Parser").
 */
class CamelCaseSplitter {

    /**
     * Splits camelCase/PascalCase name with custom separator.
     * Character transformation is applied after splitting.
     *
     * @param name          the camelCase name to split
     * @param separator     separator to insert between words
     * @param charTransform transformation to apply to each character
     * @return the split name with separators and transformed characters
     */
    static String split(String name, char separator, Function<Character, Character> charTransform) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        String splitName = splitAtWordBoundaries(name, separator);
        return applyCharTransform(splitName, charTransform);
    }

    /**
     * Splits camelCase at word boundaries, inserting separator.
     * Pure function with no side effects.
     */
    private static String splitAtWordBoundaries(String name, char separator) {
        if (name.length() == 1) {
            return name;
        }

        StringBuilder result = new StringBuilder();
        char[] chars = name.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            if (shouldInsertSeparatorBefore(chars, i)) {
                result.append(separator);
            }
            result.append(chars[i]);
        }

        return result.toString();
    }

    /**
     * Determines if a separator should be inserted before position i.
     * Handles acronyms correctly (e.g., "XMLParser" → "XML-Parser").
     */
    private static boolean shouldInsertSeparatorBefore(char[] chars, int i) {
        if (i == 0) {
            return false;
        }

        char current = chars[i];
        if (!Character.isUpperCase(current)) {
            return false;
        }

        char prev = chars[i - 1];
        boolean prevIsLowerCase = Character.isLowerCase(prev);
        boolean prevIsDigit = Character.isDigit(prev);

        if (prevIsLowerCase || prevIsDigit) {
            return true;
        }

        // Handle acronym boundary: "XMLParser" → i points to 'P'
        boolean prevIsUpperCase = Character.isUpperCase(prev);
        boolean nextIsLowerCase = i + 1 < chars.length && Character.isLowerCase(chars[i + 1]);
        return prevIsUpperCase && nextIsLowerCase;
    }

    /**
     * Applies character transformation to each character.
     */
    private static String applyCharTransform(String text, Function<Character, Character> transform) {
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            result.append(transform.apply(text.charAt(i)));
        }
        return result.toString();
    }
}
