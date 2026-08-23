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
 * Numbers the lines of a block of text in the built-in HTML report. The column publishes the
 * {@code numbered} role alongside whatever else it declares, so this sits beside {@link Lines} or
 * {@link NamedLines} rather than replacing either:
 *
 * <pre>
 * void readsASourceFile(&#64;Lines &#64;Numbered List&lt;String&gt; source) { ... }
 * void writesTheFiles(&#64;NamedLines &#64;Numbered Map&lt;String, List&lt;String&gt;&gt; files) { ... }
 * </pre>
 *
 * A number earns its place where a block is long enough that a reader needs to point at a line, or
 * where a description refers to one. On a block of two or three lines it is a column of digits as
 * wide as the text beside it, so it is asked for rather than given.
 *
 * @see ColumnRole
 * @see Lines
 * @see NamedLines
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@ColumnRole("numbered")
public @interface Numbered {}
