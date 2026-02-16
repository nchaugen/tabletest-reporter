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
package io.github.nchaugen.tabletest.reporter.junit;

import org.tabletest.junit.Description;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

/**
 * Resolves the description value from either the new {@code org.tabletest.junit.Description}
 * or the deprecated {@code io.github.nchaugen.tabletest.junit.Description} annotation.
 */
final class DescriptionResolver {

    private static final String DEPRECATED_DESCRIPTION_CLASS = "io.github.nchaugen.tabletest.junit.Description";

    static String findDescription(AnnotatedElement element) {
        return findNewDescription(element)
                .or(() -> findDeprecatedDescription(element))
                .orElse(null);
    }

    private static Optional<String> findNewDescription(AnnotatedElement element) {
        return Optional.ofNullable(element.getAnnotation(Description.class)).map(Description::value);
    }

    @SuppressWarnings("unchecked")
    private static Optional<String> findDeprecatedDescription(AnnotatedElement element) {
        try {
            Class<? extends Annotation> descriptionClass =
                    (Class<? extends Annotation>) Class.forName(DEPRECATED_DESCRIPTION_CLASS);
            Annotation annotation = element.getAnnotation(descriptionClass);
            if (annotation != null) {
                return Optional.of((String) descriptionClass.getMethod("value").invoke(annotation));
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }
}
