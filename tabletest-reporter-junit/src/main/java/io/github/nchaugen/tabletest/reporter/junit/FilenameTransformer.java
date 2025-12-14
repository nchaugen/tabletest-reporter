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

import com.github.slugify.Slugify;

/**
 * Transforms test class and method names to web-friendly kebab-case filenames.
 * <p>
 * Applies different transformation strategies based on the naming convention detected:
 * <ul>
 * <li>Names with spaces: slugify only (typical for @DisplayName or Kotlin backtick method names)</li>
 * <li>Names with underscores but no spaces: convert snake_case to kebab-case</li>
 * <li>Names without spaces or underscores: convert camelCase to kebab-case</li>
 * </ul>
 */
public class FilenameTransformer {

    private static final Slugify SLUGIFIER = Slugify.builder().build();

    public static String transform(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        if (name.contains(" ")) {
            String normalized = name.replace('_', ' ');
            return SLUGIFIER.slugify(normalized);
        }

        if (name.contains("_")) {
            return snakeCaseToKebab(name);
        }

        return camelCaseToKebab(name);
    }

    private static String snakeCaseToKebab(String name) {
        String withHyphens = name.replace('_', '-');
        return SLUGIFIER.slugify(withHyphens);
    }

    private static String camelCaseToKebab(String name) {
        if (name.length() <= 1) {
            return name.toLowerCase();
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

                boolean shouldAddHyphen = !isFirstChar && (
                    prevIsLowerCase ||
                    prevIsDigit ||
                    (prevIsUpperCase && nextIsLowerCase)
                );

                if (shouldAddHyphen) {
                    result.append('-');
                }

                result.append(Character.toLowerCase(current));
            } else {
                result.append(current);
            }
        }

        return SLUGIFIER.slugify(result.toString());
    }
}
