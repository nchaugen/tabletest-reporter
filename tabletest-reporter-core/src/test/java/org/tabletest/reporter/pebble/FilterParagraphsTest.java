package org.tabletest.reporter.pebble;

import org.tabletest.junit.TableTest;
import org.tabletest.reporter.junit.Lines;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FilterParagraphsTest {

    @TableTest("""
        Scenario                    | Text                              | Paragraphs?
        One line                    | A first line.                     | ['A first line.']
        A line break inside one     | ['A first line', 'and a second.'] | ['A first line and a second.']
        A blank line between two    | ['First.', '', 'Second.']         | ['First.', 'Second.']
        A blank line holding spaces | ['First.', '   ', 'Second.']      | ['First.', 'Second.']
        Blank lines around the text | ['', 'Only this.', '']            | ['Only this.']
        An empty text               | ''                                | []
        No text at all              |                                   | []
        """)
    void splitsTextIntoParagraphs(@Lines String text, List<String> paragraphs) {
        assertThat(new FilterParagraphs().apply(text, Map.of(), null, null, 0)).isEqualTo(paragraphs);
    }
}
