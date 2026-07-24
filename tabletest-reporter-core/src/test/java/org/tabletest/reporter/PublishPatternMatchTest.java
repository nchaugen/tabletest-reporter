package org.tabletest.reporter;

import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Unpublished: internal pattern matching behind the publish selection, not a user-facing rule.
class PublishPatternMatchTest {

    @Description("""
            A pattern names report pages by their path of page names, the path their URL shows.
            The page path is given here as its segments, so [parsing, lists] is the page
            published at parsing/lists.
            """)
    @TableTest("""
        Scenario                              | Pattern       | Page path              | Matches?
        Whole path named exactly              | parsing/lists | [parsing, lists]       | true
        Feature name matches the feature page | parsing       | [parsing]              | true
        Feature name alone stops at that page | parsing       | [parsing, lists]       | false
        Wildcard stands for any one name      | *             | [parsing]              | true
        Wildcard spans a single level only    | *             | [parsing, lists]       | false
        Wildcard matches part of a name       | ma*           | [maps]                 | true
        Partial wildcard must still fit       | ma*           | [lists]                | false
        Deep wildcard spans any depth         | **            | [parsing, lists]       | true
        Deep wildcard leads a named page      | **/lists      | [parsing, lists]       | true
        Deep wildcard spans no level at all   | **/lists      | [lists]                | true
        Deep wildcard trails a named feature  | parsing/**    | [parsing, lists, deep] | true
        Trailing deep wildcard needs no level | parsing/**    | [parsing]              | true
        Single wildcard is not a deep one     | parsing/*     | [parsing, lists, deep] | false
        Different feature does not match      | parsing/lists | [features, lists]      | false
        """)
    void matchesPagePathsBySegment(String pattern, List<String> pagePath, boolean matches) {
        assertThat(PublishPattern.parse(pattern).matches(pagePath)).isEqualTo(matches);
    }

    @TableTest("""
        Scenario               | Written as     | Matches page path?
        Surrounding whitespace | '  parsing  '  | [parsing]
        Leading separator      | /parsing/lists | [parsing, lists]
        Trailing separator     | parsing/       | [parsing]
        """)
    void readsAPatternIgnoringSeparatorNoise(String writtenAs, List<String> matchesPagePath) {
        assertThat(PublishPattern.parse(writtenAs).matches(matchesPagePath)).isTrue();
    }
}
