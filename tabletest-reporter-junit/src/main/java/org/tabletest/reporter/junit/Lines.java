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

import org.junit.jupiter.params.converter.ConvertWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a column whose cells hold the lines of one block of text, written as a list of lines
 * because the table format keeps every row on one line. The column publishes the {@code lines}
 * role, which the built-in HTML report styles as a stacked monospace block rather than a bulleted
 * list, so text whose alignment is the point reads as it was written.
 * <p>
 * The parameter receives the lines joined by newlines when it is a {@code String}, and the lines
 * themselves when it is a {@code List}.
 *
 * <pre>
 * &#64;TableTest("""
 *     Scenario        | Source                            | Table Count?
 *     One table       | ["a | b", "1 | 2"]                | 1
 *     Two tables      | ["a | b", "1 | 2", "", "c", "3"]  | 2
 *     """)
 * void countsTables(&#64;Lines String source, int tableCount) { ... }
 * </pre>
 *
 * @see ColumnRole
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@ColumnRole("lines")
@ConvertWith(LinesConverter.class)
public @interface Lines {}
