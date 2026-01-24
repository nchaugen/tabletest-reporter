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

import io.github.nchaugen.tabletest.junit.Description;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Extracts identity metadata for a JUnit TableTest method.
 */
final class JunitTestIdentityExtractor {

    static TableTestIdentity extract(ExtensionContext context) {
        String methodName = context.getRequiredTestMethod().getName();
        String displayName = JunitDisplayNameResolver.resolve(context, () -> methodName);
        String slug = Slugger.slugify(displayName);
        String title = JunitTitleExtractor.extractMethodTitle(context);
        String description = findDescription(context);
        return new TableTestIdentity(methodName, slug, title, description);
    }

    private static String findDescription(ExtensionContext context) {
        return context.getTestMethod()
                .map(method -> method.getAnnotation(Description.class))
                .map(Description::value)
                .orElse(null);
    }
}
