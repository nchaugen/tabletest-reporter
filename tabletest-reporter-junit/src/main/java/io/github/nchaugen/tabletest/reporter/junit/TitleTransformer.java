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

/**
 * Transforms test class and method names to human-readable titles.
 * <p>
 * Converts camelCase and PascalCase names into space-separated words with
 * proper capitalization. Examples:
 * <ul>
 * <li>LeapYearRules → Leap Year Rules</li>
 * <li>XMLParser → XML Parser</li>
 * <li>parseHTMLDocument → Parse HTML Document</li>
 * </ul>
 */
public class TitleTransformer {

    public static String toTitle(String name) {
        if (name == null || name.isEmpty() || name.contains(" ")) {
            return name;
        }

        if (name.length() == 1) {
            return name.toUpperCase();
        }

        if (name.contains("_")) {
            name = name.replace('_', ' ');
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

                boolean shouldAddSpace = !isFirstChar && (
                    prevIsLowerCase ||
                    prevIsDigit ||
                    (prevIsUpperCase && nextIsLowerCase)
                );

                if (shouldAddSpace) {
                    result.append(' ');
                }

                result.append(current);
            } else {
                if (i == 0) {
                    result.append(Character.toUpperCase(current));
                } else {
                    result.append(current);
                }
            }
        }

        return result.toString();
    }
}
