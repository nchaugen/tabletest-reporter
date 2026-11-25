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

    @TableTest("""
        Scenario                        | Text                                       | Lines?
        A sentence over two lines       | ['A first line', 'and a second.']          | ['A first line and a second.']
        A dash list after a sentence    | ['Two kinds:', '- first,', '- second.']    | ['Two kinds:', '- first,', '- second.']
        A star list                     | ['* first,', '* second.']                  | ['* first,', '* second.']
        A numbered list                 | ['1. first,', '2. second.']                | ['1. first,', '2. second.']
        An item continued on a new line | ['- first, which', '  goes on.', '- next'] | ['- first, which goes on.', '- next']
        A minus sign that starts a line | ['From', '-1 to 1.']                       | ['From -1 to 1.']
        """)
    void keepsAListItemOnItsOwnLine(@Lines String text, List<String> lines) {
        assertThat(paragraphsOf(text))
                .singleElement()
                .extracting(paragraph -> paragraph.lines().toList())
                .isEqualTo(lines);
    }

    @SuppressWarnings("unchecked")
    private static List<String> paragraphsOf(String text) {
        return (List<String>) new FilterParagraphs().apply(text, Map.of(), null, null, 0);
    }
}
