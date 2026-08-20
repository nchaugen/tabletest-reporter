package org.tabletest.reporter.junit;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Derives the token a declared column role is published as, and decides whether that token can be
 * published. A token reaches a stylesheet as a CSS class and an AsciiDoc role, so it is held to
 * lower-case words joined by single hyphens.
 */
final class ColumnRoleTokens {

    private static final Pattern PUBLISHABLE = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    private ColumnRoleTokens() {}

    /**
     * @return the token declared explicitly, or the annotation's simple name in kebab case.
     */
    static String tokenFor(String declaredToken, String annotationName) {
        return declaredToken.isEmpty()
                ? CamelCaseSplitter.split(annotationName, '-', Character::toLowerCase)
                : declaredToken;
    }

    static boolean isMalformed(String token) {
        return !PUBLISHABLE.matcher(token).matches();
    }

    /**
     * @return true if the token is one the reporter derives itself. Such a token is still published,
     * so the column is styled as that role, but nothing about the run is decided by it.
     */
    static boolean isComputedRole(String token) {
        return Arrays.stream(CellRole.values()).map(CellRole::token).anyMatch(token::equals);
    }
}
