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
 * Marks an annotation as declaring a role for the table column its parameter binds to. The role is
 * published on every cell of that column, where a template or stylesheet can act on it.
 * <p>
 * Published roles are an open vocabulary: the reporter derives {@code scenario}, {@code expectation},
 * {@code passed} and {@code failed} itself, and a declared role is published alongside them without
 * being treated as one of them.
 * <p>
 * The token defaults to the annotated annotation's simple name in kebab case, so {@code @SourceLines}
 * publishes {@code source-lines}. Give {@link #value()} to publish a different token — to spell it
 * differently, or to keep two annotations of the same simple name apart.
 *
 * <pre>
 * &#64;Target(ElementType.PARAMETER)
 * &#64;Retention(RetentionPolicy.RUNTIME)
 * &#64;ColumnRole
 * public &#64;interface Ingredient {
 * }
 * </pre>
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ColumnRole {

    /**
     * @return the token to publish, or empty to derive it from the annotation's simple name.
     */
    String value() default "";
}
