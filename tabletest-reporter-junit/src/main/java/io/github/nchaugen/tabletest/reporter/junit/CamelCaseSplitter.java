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
package io.github.nchaugen.tabletest.reporter.junit;

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
     * Splits camelCase/PascalCase name with custom separator and character transformation.
     *
     * @param name              the camelCase name to split
     * @param separator         separator to insert between words
     * @param charTransform     transformation to apply to each character
     * @return the split name with separators and transformed characters
     */
    static String split(String name, char separator, Function<Character, Character> charTransform) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        if (name.length() == 1) {
            return String.valueOf(charTransform.apply(name.charAt(0)));
        }

        StringBuilder result = new StringBuilder();
        char[] chars = name.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char current = chars[i];
            boolean isUpperCase = Character.isUpperCase(current);

            if (isUpperCase) {
                boolean isFirstChar = i == 0;
                boolean nextIsLowerCase = i + 1 < chars.length && Character.isLowerCase(chars[i + 1]);
                boolean prevIsLowerCase = i > 0 && Character.isLowerCase(chars[i - 1]);
                boolean prevIsDigit = i > 0 && Character.isDigit(chars[i - 1]);
                boolean prevIsUpperCase = i > 0 && Character.isUpperCase(chars[i - 1]);

                boolean shouldAddSeparator = !isFirstChar && (
                    prevIsLowerCase ||
                    prevIsDigit ||
                    (prevIsUpperCase && nextIsLowerCase)
                );

                if (shouldAddSeparator) {
                    result.append(separator);
                }

                result.append(charTransform.apply(current));
            } else {
                result.append(charTransform.apply(current));
            }
        }

        return result.toString();
    }
}
