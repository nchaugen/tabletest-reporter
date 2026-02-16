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

        if (name.contains("_")) {
            name = name.replace('_', ' ');
        }

        String result = CamelCaseSplitter.split(name, ' ', ch -> ch);
        if (result.isEmpty()) {
            return result;
        }
        return Character.toUpperCase(result.charAt(0)) + result.substring(1);
    }
}
