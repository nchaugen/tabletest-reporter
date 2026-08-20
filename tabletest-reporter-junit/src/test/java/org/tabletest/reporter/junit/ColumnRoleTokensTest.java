package org.tabletest.reporter.junit;

import org.junit.jupiter.api.DisplayName;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColumnRoleTokensTest {

    @DisplayName("Names a declared column role by its annotation unless the annotation names it")
    @Description("""
        The token reaches a stylesheet as a CSS class and an AsciiDoc role, so the derived form is
        kebab case rather than the annotation's own casing.
        """)
    @TableTest("""
        Scenario                    | Declared Token | Annotation Name | Published Token?
        No token declared           | ''             | Lines           | lines
        Annotation of several words | ''             | SourceLines     | source-lines
        Acronym in the annotation   | ''             | HTMLSource      | html-source
        Token declared              | source-lines   | Lines           | source-lines
        """)
    void namesRoleByAnnotation(String declaredToken, String annotationName, String publishedToken) {
        assertEquals(publishedToken, ColumnRoleTokens.tokenFor(declaredToken, annotationName));
    }

    @DisplayName("Refuses a token that is not lower-case words joined by single hyphens")
    @Description("""
        A refused token is left unpublished, since it would otherwise reach an HTML class attribute
        and an AsciiDoc role unescaped. A dotted token is the case worth noting: it is what a
        qualified annotation name would produce, and AsciiDoc reads each dot as a further role.
        """)
    @TableTest("""
        Scenario                     | Token          | Malformed?
        Single lower-case word       | lines          | false
        Two words joined by a hyphen | source-lines   | false
        Digit inside a word          | h2             | false
        Capital letter               | Lines          | true
        Dots of a qualified name     | org.x.lines    | true
        Space between words          | 'source lines' | true
        Leading hyphen               | -lines         | true
        Trailing hyphen              | lines-         | true
        Doubled hyphen               | source--lines  | true
        Nothing at all               | ''             | true
        """)
    void refusesMalformedToken(String token, boolean malformed) {
        assertEquals(malformed, ColumnRoleTokens.isMalformed(token));
    }

    @DisplayName("Tells a role the reporter derives from one only a test declares")
    @Description("""
        A declared token matching a derived role is still published — the column is styled as that
        role without being treated as one — so this only reports the overlap.
        """)
    @TableTest("""
        Scenario                  | Token                                   | Reporter Derives It?
        Role the reporter derives | {scenario, expectation, passed, failed} | true
        Role only a test declares | lines                                   | false
        """)
    void tellsDerivedRoleFromDeclared(String token, boolean reporterDerivesIt) {
        assertEquals(reporterDerivesIt, ColumnRoleTokens.isComputedRole(token));
    }
}
