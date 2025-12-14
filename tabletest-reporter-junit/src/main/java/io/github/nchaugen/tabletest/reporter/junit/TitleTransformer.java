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
        if (name == null || name.isEmpty()) {
            return name;
        }

        if (name.length() == 1) {
            return name.toUpperCase();
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
