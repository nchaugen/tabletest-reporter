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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregate pass/fail status of a report subtree, counted in scenarios (executed table
 * rows). A subtree with no executed scenarios is neutral; otherwise it fails when any of
 * its scenarios is broken. Instances are combined with {@link #plus} to roll status up
 * from tables to the indexes that contain them.
 */
record ReportStatus(int totalScenarios, int passedScenarios) {

    static ReportStatus none() {
        return new ReportStatus(0, 0);
    }

    static ReportStatus ofScenarios(int totalScenarios, int passedScenarios) {
        return new ReportStatus(totalScenarios, passedScenarios);
    }

    ReportStatus plus(ReportStatus other) {
        return new ReportStatus(totalScenarios + other.totalScenarios, passedScenarios + other.passedScenarios);
    }

    boolean hasResults() {
        return totalScenarios > 0;
    }

    int brokenScenarios() {
        return totalScenarios - passedScenarios;
    }

    boolean failed() {
        return brokenScenarios() > 0;
    }

    /**
     * Template vocabulary for a status dot: {@code passed}, {@code failed}, or {@code neutral}
     * when the subtree carries no executed scenarios.
     */
    String state() {
        if (!hasResults()) {
            return "neutral";
        }
        return failed() ? "failed" : "passed";
    }

    Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("state", state());
        map.put("total", totalScenarios);
        map.put("passed", passedScenarios);
        map.put("broken", brokenScenarios());
        return map;
    }
}
