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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Extracts human-readable titles from JUnit test classes and methods.
 * <p>
 * Handles both explicit titles from @DisplayName annotations and implicit
 * titles derived from class/method names.
 */
class JunitTitleExtractor {

    /**
     * Extracts title for a test method.
     * Uses @DisplayName if present, otherwise transforms method name to title.
     * Method names with parameter types (e.g., "method(java.lang.String)") are
     * stripped before transformation.
     */
    static String extractMethodTitle(ExtensionContext context) {
        return context.getTestMethod()
            .map(testMethod -> {
                if (testMethod.isAnnotationPresent(DisplayName.class)) {
                    return context.getDisplayName();
                } else {
                    String methodName = testMethod.getName();
                    int paramStart = methodName.indexOf('(');
                    if (paramStart > 0) {
                        methodName = methodName.substring(0, paramStart);
                    }
                    return TitleTransformer.toTitle(methodName);
                }
            })
            .orElse(context.getDisplayName());
    }

    /**
     * Extracts title for a test class.
     * Uses @DisplayName if present, otherwise transforms class simple name to title.
     */
    static String extractClassTitle(ExtensionContext context) {
        return context.getTestClass()
            .map(testClass -> {
                if (testClass.isAnnotationPresent(DisplayName.class)) {
                    return context.getDisplayName();
                } else {
                    return TitleTransformer.toTitle(testClass.getSimpleName());
                }
            })
            .orElse(context.getDisplayName());
    }
}
