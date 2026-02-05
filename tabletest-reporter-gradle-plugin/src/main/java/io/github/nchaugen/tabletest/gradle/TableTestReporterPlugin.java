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
package io.github.nchaugen.tabletest.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.testing.Test;
import org.gradle.process.CommandLineArgumentProvider;

import java.util.Optional;

/**
 * Gradle plugin for generating TableTest documentation.
 * <p>
 * Registers the {@code tableTestReporter} extension for configuration and the
 * {@code reportTableTests} task for generating documentation from TableTest YAML files.
 */
public class TableTestReporterPlugin implements Plugin<Project> {

    private static final String JUNIT_OUTPUT_DIR_PROPERTY = "junit.platform.reporting.output.dir";

    /**
     * Creates a new plugin instance.
     */
    public TableTestReporterPlugin() {}

    @Override
    public void apply(Project project) {
        // Register extension with defaults
        TableTestReporterExtension ext =
                project.getExtensions().create("tableTestReporter", TableTestReporterExtension.class);

        // Register task and wire conventions from extension
        project.getTasks().register("reportTableTests", ReportTableTestsTask.class, t -> {
            t.getFormat().convention(ext.getFormat());
            t.getInputDir().convention(ext.getInputDir());
            t.getOutputDir().convention(ext.getOutputDir());
            t.getTemplateDir().convention(ext.getTemplateDir());
            t.getJunitOutputDir().convention(project.provider(() -> resolveJunitOutputDir(project)));
            t.getIndexDepth().convention(ext.getIndexDepth());
        });

        // Register list formats task
        project.getTasks().register("listTableTestReportFormats", ListFormatsTask.class, t -> t.getTemplateDir()
                .convention(ext.getTemplateDir()));

        // Make `build` depend on generation by default? Keep opt-in to avoid surprises.
        // Users will run `./gradlew reportTableTests` explicitly.
    }

    private static String resolveJunitOutputDir(Project project) {
        Task testTask = project.getTasks().findByName("test");
        if (!(testTask instanceof Test test)) {
            return null;
        }
        String fromSystemProperties = Optional.ofNullable(
                        test.getSystemProperties().get(JUNIT_OUTPUT_DIR_PROPERTY))
                .map(Object::toString)
                .filter(value -> !value.isBlank())
                .orElse(null);
        if (fromSystemProperties != null) {
            return fromSystemProperties;
        }

        String fromJvmArgs = findSystemPropertyValue(test.getJvmArgs());
        if (fromJvmArgs != null) {
            return fromJvmArgs;
        }

        return findSystemPropertyValueFromProviders(test.getJvmArgumentProviders());
    }

    private static String findSystemPropertyValueFromProviders(
            Iterable<? extends CommandLineArgumentProvider> providers) {
        for (CommandLineArgumentProvider provider : providers) {
            String value = findSystemPropertyValue(provider.asArguments());
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String findSystemPropertyValue(Iterable<String> args) {
        for (String arg : args) {
            String value = parseSystemPropertyValue(arg);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String parseSystemPropertyValue(String arg) {
        if (arg == null) {
            return null;
        }
        String prefix = "-D" + JUNIT_OUTPUT_DIR_PROPERTY + "=";
        if (!arg.startsWith(prefix)) {
            return null;
        }
        String value = arg.substring(prefix.length());
        return value.isBlank() ? null : value;
    }
}
