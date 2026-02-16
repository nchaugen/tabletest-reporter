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

import org.tabletest.junit.TableTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Resolves table test input from either the new {@code org.tabletest.junit.TableTest}
 * or the deprecated {@code io.github.nchaugen.tabletest.junit.TableTest} annotation.
 */
final class TableTestAnnotationResolver {

    private static final String DEPRECATED_TABLETEST_CLASS = "io.github.nchaugen.tabletest.junit.TableTest";

    /**
     * Resolves the table input string from whichever {@code @TableTest} annotation is present.
     *
     * @param method    the test method
     * @param testClass the test class (for resource loading)
     * @return the resolved input string, or empty if no {@code @TableTest} annotation is found
     */
    static Optional<String> resolveInput(Method method, Class<?> testClass) {
        return findNewAnnotation(method, testClass).or(() -> findDeprecatedAnnotation(method, testClass));
    }

    private static Optional<String> findNewAnnotation(Method method, Class<?> testClass) {
        TableTest annotation = method.getAnnotation(TableTest.class);
        if (annotation == null) {
            return Optional.empty();
        }
        return Optional.of(resolveInput(annotation.value(), annotation.resource(), annotation.encoding(), testClass));
    }

    @SuppressWarnings("unchecked")
    private static Optional<String> findDeprecatedAnnotation(Method method, Class<?> testClass) {
        try {
            Class<? extends Annotation> tableTestClass =
                    (Class<? extends Annotation>) Class.forName(DEPRECATED_TABLETEST_CLASS);
            Annotation annotation = method.getAnnotation(tableTestClass);
            if (annotation != null) {
                String value = (String) tableTestClass.getMethod("value").invoke(annotation);
                String resource = (String) tableTestClass.getMethod("resource").invoke(annotation);
                String encoding = (String) tableTestClass.getMethod("encoding").invoke(annotation);
                return Optional.of(resolveInput(value, resource, encoding, testClass));
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    private static String resolveInput(String value, String resource, String encoding, Class<?> testClass) {
        return resource.isBlank() ? value : loadResource(resource, encoding, testClass);
    }

    private static String loadResource(String resource, String encoding, Class<?> testClass) {
        try (InputStream stream = resolveResourceStream(resource, testClass)) {
            return new BufferedReader(new InputStreamReader(stream, encoding))
                    .lines()
                    .collect(Collectors.joining("\n"));
        } catch (IOException cause) {
            throw new RuntimeException("Failed to read resource: " + resource, cause);
        }
    }

    private static InputStream resolveResourceStream(String resource, Class<?> testClass) {
        InputStream stream = testClass.getResourceAsStream(resource);
        if (stream == null) {
            stream = testClass.getResourceAsStream("/" + resource);
        }
        return Objects.requireNonNull(stream, "Could not load resource " + resource);
    }
}
