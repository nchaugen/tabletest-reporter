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

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.function.Supplier;

/**
 * Resolves a stable display name for JUnit elements, falling back to a default name.
 */
final class JunitDisplayNameResolver {

    private JunitDisplayNameResolver() {}

    static @NonNull String resolve(ExtensionContext context, Supplier<String> defaultName) {
        return context.getElement()
                .filter(it -> it.isAnnotationPresent(DisplayName.class))
                .map(__ -> context.getDisplayName())
                .orElseGet(defaultName);
    }
}
