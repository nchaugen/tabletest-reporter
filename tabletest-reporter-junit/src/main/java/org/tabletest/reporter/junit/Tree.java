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
 * Marks a column whose cells hold a tree, written as a nested collection. The column publishes the
 * {@code tree} role, which the built-in HTML report renders as a tree: each level opens below its
 * parent rather than beside it, and a guide line runs down the level.
 * <p>
 * The default rendering puts a map key beside its value, which walks a deep tree sideways across
 * the page. The cell value itself is unchanged, so a reader still meets the notation they would
 * write.
 *
 * <pre>
 * &#64;TableTest("""
 *     Scenario           | Published tables         | Page tree?
 *     One class, one table | ['pkg.OrderTest#items'] | [pkg: [[order-test: [items]]]]
 *     """)
 * void buildsThePageTree(List&lt;String&gt; publishedTables, &#64;Tree Map&lt;String, Object&gt; pageTree) { ... }
 * </pre>
 *
 * @see ColumnRole
 * @see Lines
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@ColumnRole("tree")
public @interface Tree {}
