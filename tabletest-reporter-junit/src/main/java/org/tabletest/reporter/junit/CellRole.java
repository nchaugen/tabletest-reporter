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
package org.tabletest.reporter.junit;

public enum CellRole {
    EXPECTATION,
    SCENARIO,
    PASSED,
    FAILED,
    VALUE_SET;

    /**
     * @return the token this role is published as, one of the names the reporter derives itself.
     * Published roles are an open vocabulary of such tokens, so a role a test declares is carried
     * as a token alongside these rather than as an enum constant.
     */
    public String token() {
        return name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    }
}
