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
 * Transforms test class and method names to web-friendly kebab-case filenames.
 * <p>
 * Applies different transformation strategies based on the naming convention detected:
 * <ul>
 * <li>Names with spaces: slugify only (typical for @DisplayName or Kotlin backtick method names)</li>
 * <li>Names with underscores but no spaces: convert snake_case to kebab-case</li>
 * <li>Names without spaces or underscores: convert camelCase to kebab-case</li>
 * </ul>
 * <p>
 * The separated words are then reduced to a slug that is always usable as a filename, falling
 * back through progressively less familiar forms only for names the previous form cannot serve.
 */
public class Slugger {

    public static String slugify(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return toUsableSlug(separateWords(name), name);
    }

    /**
     * Prefers the ASCII slug, since that is what every existing published URL is made of. A name
     * written in a script the fold has no answer for keeps its own characters instead, and one
     * with no letters or digits anywhere is named after a hash of itself so that two such names
     * still get two filenames.
     */
    private static String toUsableSlug(String words, String name) {
        String ascii = AsciiSlug.of(words);
        if (containsLetter(ascii)) {
            return ascii;
        }
        String ownScript = NativeScriptSlug.of(words);
        return ownScript.isEmpty() ? unnamedAfterItsOwnHash(name) : ownScript;
    }

    private static boolean containsLetter(String slug) {
        return slug.chars().anyMatch(Character::isLetter);
    }

    private static String unnamedAfterItsOwnHash(String name) {
        return "unnamed-%08x".formatted(name.hashCode());
    }

    private static String separateWords(String name) {
        if (name.contains(" ")) {
            return name.replace('_', ' ');
        }
        if (name.contains("_")) {
            return snakeCaseToKebab(name);
        }
        return camelCaseToKebab(name);
    }

    private static String snakeCaseToKebab(String name) {
        return name.replace('_', '-');
    }

    private static String camelCaseToKebab(String name) {
        return CamelCaseSplitter.split(name, '-', Character::toLowerCase);
    }
}
