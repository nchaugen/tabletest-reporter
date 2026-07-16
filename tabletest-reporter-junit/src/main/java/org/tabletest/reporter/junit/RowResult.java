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

/**
 * Records the test execution result for a single test invocation.
 *
 * @param rowIndex     The JUnit invocation index (1-based). With value-set expansion a table
 *                     row produces several invocations, so this does not map 1:1 to table rows
 * @param passed       Whether the test passed for this invocation
 * @param cause        The exception if the test failed, null if passed
 * @param displayName  The display name of the test invocation
 */
public record RowResult(int rowIndex, boolean passed, Throwable cause, String displayName) {}
