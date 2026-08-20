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

import org.junit.jupiter.api.extension.ExtensionContext;
import org.tabletest.junit.ParameterType;
import org.tabletest.junit.Scenario;
import org.tabletest.parser.Table;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Derives column roles from JUnit context and the parsed table.
 */
final class JunitColumnRoleExtractor {

    static ColumnRoles extract(ExtensionContext context, Table table) {
        return new ColumnRoles(findScenarioIndex(context, table), findExpectationIndices(context, table));
    }

    /**
     * Collects the columns whose set value expands the row. A column expands when the parameter
     * bound to it is not itself a set.
     */
    static ExpandingColumns extractExpanding(ExtensionContext context, Table table) {
        Parameter[] parameters = context.getRequiredTestMethod().getParameters();
        int unboundColumns = Math.max(0, table.columnCount() - parameters.length);

        return new ExpandingColumns(IntStream.range(0, parameters.length)
                .filter(index -> !ParameterType.of(parameters[index]).isSet())
                .map(index -> index + unboundColumns)
                .boxed()
                .collect(Collectors.toSet()));
    }

    /**
     * Collects the roles each column declares through the annotations on the parameter it binds to.
     * A table with more columns than parameters leaves its first column unbound, so parameters are
     * matched to columns from the right.
     */
    static DeclaredColumnRoles extractDeclared(ExtensionContext context, Table table) {
        Parameter[] parameters = context.getRequiredTestMethod().getParameters();
        int unboundColumns = Math.max(0, table.columnCount() - parameters.length);

        return new DeclaredColumnRoles(IntStream.range(0, parameters.length)
                .mapToObj(index -> Map.entry(index + unboundColumns, publishableRolesOf(parameters[index])))
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    /**
     * Reports the roles a parameter declares that can be published, warning about the rest. A token
     * shadowing a role the reporter derives is published anyway — the column is styled as that role
     * without being treated as one.
     */
    private static List<String> publishableRolesOf(Parameter parameter) {
        List<String> declared = declaredRolesOf(parameter);
        declared.stream().filter(ColumnRoleTokens::isMalformed).forEach(token -> warnMalformed(parameter, token));
        declared.stream().filter(ColumnRoleTokens::isComputedRole).forEach(token -> warnShadowed(parameter, token));
        return declared.stream()
                .filter(token -> !ColumnRoleTokens.isMalformed(token))
                .toList();
    }

    private static List<String> declaredRolesOf(Parameter parameter) {
        return Arrays.stream(parameter.getAnnotations())
                .flatMap(annotation -> declaredRolesOf(annotation.annotationType(), new HashSet<>()))
                .distinct()
                .toList();
    }

    /**
     * Searches an annotation and its meta-annotations for a {@link ColumnRole} declaration. Each
     * annotation type is visited at most once, so cyclic meta-annotations cannot recurse forever.
     */
    private static Stream<String> declaredRolesOf(
            Class<? extends Annotation> annotationType, Set<Class<? extends Annotation>> visited) {
        if (isLanguageMetaAnnotation(annotationType) || !visited.add(annotationType)) {
            return Stream.empty();
        }
        ColumnRole declaration = annotationType.getAnnotation(ColumnRole.class);
        return declaration != null
                ? Stream.of(ColumnRoleTokens.tokenFor(declaration.value(), annotationType.getSimpleName()))
                : Arrays.stream(annotationType.getAnnotations())
                        .flatMap(meta -> declaredRolesOf(meta.annotationType(), visited));
    }

    /**
     * Language-provided meta-annotations (Retention, Target, etc.) cannot declare a role and are
     * skipped. Matches on the type name rather than {@code Class.getPackage()}, which can return null.
     */
    private static boolean isLanguageMetaAnnotation(Class<? extends Annotation> annotationType) {
        String typeName = annotationType.getName();
        return typeName.startsWith("java.lang.annotation.") || typeName.startsWith("kotlin.annotation.");
    }

    private static void warnMalformed(Parameter parameter, String token) {
        System.err.printf(
                "tabletest-reporter: column role \"%s\" declared on parameter \"%s\" is not published - "
                        + "a role must be lower-case words joined by single hyphens%n",
                token, parameter.getName());
    }

    private static void warnShadowed(Parameter parameter, String token) {
        System.err.printf(
                "tabletest-reporter: column role \"%s\" declared on parameter \"%s\" is a role the reporter "
                        + "derives itself - the column is styled as one but is not treated as one%n",
                token, parameter.getName());
    }

    private static OptionalInt findScenarioIndex(ExtensionContext context, Table table) {
        OptionalInt explicit = getExplicitScenarioColumn(context);
        return explicit.isPresent() ? explicit : getImplicitScenarioColumn(context, table);
    }

    private static final String DEPRECATED_SCENARIO_CLASS = "io.github.nchaugen.tabletest.junit.Scenario";

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
            Class<? extends Annotation> deprecatedScenario =
                    (Class<? extends Annotation>) Class.forName(DEPRECATED_SCENARIO_CLASS);
            return parameter.isAnnotationPresent(deprecatedScenario);
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
