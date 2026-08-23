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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a column whose cells hold several blocks of text, each under a name. The column publishes
 * the {@code named-lines} role, which the built-in HTML report renders as one named block per
 * entry: the name as a caption, the lines below it stacked as written.
 * <p>
 * A file and its contents is the case it was built for, so a cell reads as a small directory. The
 * name is a plain string — a map key is never converted, whatever the parameter declares — so a
 * test that writes the blocks out resolves the name itself.
 * <p>
 * Use {@link Lines} where a cell holds one unnamed block. This is that column with a caption on
 * each block, and the blocks are styled the same way.
 *
 * <pre>
 * &#64;TableTest("""
 *     Scenario                   | Template files                                | Table page?
 *     The name of the table page | [table.md.peb: ['# {{ title }}', 'By hand.']] | ['# Leap years', 'By hand.']
 *     """)
 * void usesYourTemplate(&#64;NamedLines Map&lt;String, List&lt;String&gt;&gt; templateFiles, &#64;Lines List&lt;String&gt; tablePage) { ... }
 * </pre>
 *
 * @see ColumnRole
 * @see Lines
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@ColumnRole("named-lines")
public @interface NamedLines {}
