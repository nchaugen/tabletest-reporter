package org.tabletest.reporter;

import org.junit.jupiter.api.DisplayName;
import org.tabletest.junit.Description;
import org.tabletest.junit.Scenario;
import org.tabletest.junit.TableTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The arithmetic behind the verdict a page states: a table's status derived from its executed row
 * results, and an index's status summed bottom-up from its children.
 *
 * <p>Conformance rather than a rule. The published rules are in {@code pages/PassFailRollupTest},
 * where they are read off the pages of a report of a run that really passed and failed; these
 * cases reach the counting directly, including the ones a run cannot easily produce.
 */
class StatusRollupTest {

    @DisplayName("Derives a table's status from its executed row results")
    @TableTest("""
        Scenario       | Row results   | State?  | Total? | Passed?
        No results run |               | neutral | 0      | 0
        Empty results  | []            | neutral | 0      | 0
        All passing    | [true, true]  | passed  | 2      | 2
        One broken     | [true, false] | failed  | 2      | 1
        All broken     | [false]       | failed  | 1      | 0
        """)
    void derives_table_status_from_row_results(
            @Scenario String scenario, List<Boolean> rowResults, String state, int total, int passed) {
        ReportStatus status = StatusRollup.of(tableNode(rowResults));

        assertThat(status.state()).isEqualTo(state);
        assertThat(status.totalScenarios()).isEqualTo(total);
        assertThat(status.passedScenarios()).isEqualTo(passed);
    }

    @DisplayName("Sums a feature's status bottom-up from its children")
    @Description("""
            Child A and Child B are two tables in the same feature, given as their row
            results. A neutral child contributes nothing to the counts and never turns
            a passing feature red.
            """)
    @TableTest("""
        Scenario                | Child A | Child B      | State?  | Total? | Passed?
        All children passing    | [true]  | [true, true] | passed  | 3      | 3
        One child broken        | [true]  | [false]      | failed  | 2      | 1
        No child has results    |         |              | neutral | 0      | 0
        Passing plus no-results | [true]  |              | passed  | 1      | 1
        """)
    void aggregates_index_status_from_children(
            @Scenario String scenario,
            List<Boolean> childA,
            List<Boolean> childB,
            String state,
            int total,
            int passed) {
        ReportStatus status = StatusRollup.of(indexNode(tableNode(childA), tableNode(childB)));

        assertThat(status.state()).isEqualTo(state);
        assertThat(status.totalScenarios()).isEqualTo(total);
        assertThat(status.passedScenarios()).isEqualTo(passed);
    }

    private static TableNode tableNode(List<Boolean> rowResults) {
        Map<String, Object> resource = new HashMap<>();
        if (rowResults != null) {
            resource.put(
                    "rowResults",
                    rowResults.stream().map(StatusRollupTest::rowResult).toList());
        }
        return new TableNode("table", "table", resource);
    }

    private static Map<String, Object> rowResult(boolean passed) {
        return Map.of("passed", passed);
    }

    private static IndexNode indexNode(ReportNode... children) {
        return new IndexNode("index", "index", null, List.of(children));
    }
}
