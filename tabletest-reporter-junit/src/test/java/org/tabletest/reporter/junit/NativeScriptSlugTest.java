package org.tabletest.reporter.junit;

import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Characterises the slug that keeps a name in the script it was written in, used when the
 * ASCII fold has nothing to offer.
 */
class NativeScriptSlugTest {

    @Description("""
        Letters, digits, combining marks and underscores of any script survive; every other
        run becomes a single hyphen, exactly as the ASCII slug treats its own alphabet.
        On a name that is already ASCII this agrees with the ASCII slug character for
        character, which is what makes it safe as a fallback tier.
        """)
    @TableTest("""
        Scenario                | Input           | Result?
        Cyrillic                | Москва          | москва
        Greek with uppercase    | Ελληνικά        | ελληνικά
        CJK has no case         | 日本語のテスト  | 日本語のテスト
        Halfwidth katakana      | ﾃｽﾄ             | テスト
        Combining marks survive | हिन्दी परीक्षण    | हिन्दी-परीक्षण
        Punctuation collapses   | Москва: тест!   | москва-тест
        Enclosing punctuation   | «Москва»        | москва
        Digits survive          | тест 42         | тест-42
        Underscore survives     | тест_кейс       | тест_кейс
        Agrees on ASCII names   | Leap Year Rules | leap-year-rules
        Agrees on ASCII digits  | 123             | 123
        Nothing usable left     | '!!!'           | ''
        Empty string            | ''              | ''
        """)
    void shouldKeepNameInItsOwnScript(String input, String expected) {
        assertThat(NativeScriptSlug.of(input)).isEqualTo(expected);
    }
}
