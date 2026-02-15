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

import io.github.nchaugen.tabletest.junit.Scenario;
import io.github.nchaugen.tabletest.parser.Table;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Derives column roles from JUnit context and the parsed table.
 */
final class JunitColumnRoleExtractor {

    static ColumnRoles extract(ExtensionContext context, Table table) {
        return new ColumnRoles(findScenarioIndex(context, table), findExpectationIndices(context, table));
    }

    private static OptionalInt findScenarioIndex(ExtensionContext context, Table table) {
        OptionalInt explicit = getExplicitScenarioColumn(context);
        return explicit.isPresent() ? explicit : getImplicitScenarioColumn(context, table);
    }

    private static final String NEW_SCENARIO_CLASS = "org.tabletest.junit.Scenario";

    private static OptionalInt getExplicitScenarioColumn(ExtensionContext context) {
        return IntStream.range(0, context.getRequiredTestMethod().getParameterCount())
                .filter(i -> isScenarioAnnotated(context.getRequiredTestMethod().getParameters()[i]))
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    private static boolean isScenarioAnnotated(Parameter parameter) {
        if (parameter.isAnnotationPresent(Scenario.class)) {
            return true;
        }
        try {
            Class<? extends Annotation> newScenario = (Class<? extends Annotation>) Class.forName(NEW_SCENARIO_CLASS);
            return parameter.isAnnotationPresent(newScenario);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static OptionalInt getImplicitScenarioColumn(ExtensionContext context, Table table) {
        return table.headers().size() > context.getRequiredTestMethod().getParameterCount()
                ? OptionalInt.of(0)
                : OptionalInt.empty();
    }

    private static Set<Integer> findExpectationIndices(ExtensionContext context, Table table) {
        String patternString = context.getConfigurationParameter("tabletest.reporter.expectation.pattern")
                .orElse(".*\\?$");
        Pattern pattern = Pattern.compile(patternString);

        return IntStream.range(0, table.headers().size())
                .filter(i -> pattern.matcher(table.header(i)).matches())
                .boxed()
                .collect(Collectors.toSet());
    }
}
