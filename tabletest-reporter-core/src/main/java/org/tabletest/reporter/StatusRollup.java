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
package org.tabletest.reporter;

import java.util.List;
import java.util.Map;

/**
 * Computes the aggregate {@link ReportStatus} of a report node bottom-up: a table's status
 * comes from its executed row results, an index's from the sum of its children's. Pure over
 * the node tree — it reads the loaded YAML resource but has no side effects.
 */
final class StatusRollup {

    private StatusRollup() {}

    static ReportStatus of(ReportNode node) {
        return switch (node) {
            case TableNode table -> ofTable(table);
            case IndexNode index ->
                index.contents().stream().map(StatusRollup::of).reduce(ReportStatus.none(), ReportStatus::plus);
        };
    }

    private static ReportStatus ofTable(TableNode table) {
        if (!(table.resource().get("rowResults") instanceof List<?> results) || results.isEmpty()) {
            return ReportStatus.none();
        }
        int passed = (int) results.stream().filter(StatusRollup::isPassed).count();
        return ReportStatus.ofScenarios(results.size(), passed);
    }

    private static boolean isPassed(Object result) {
        return result instanceof Map<?, ?> map && Boolean.TRUE.equals(map.get("passed"));
    }
}
