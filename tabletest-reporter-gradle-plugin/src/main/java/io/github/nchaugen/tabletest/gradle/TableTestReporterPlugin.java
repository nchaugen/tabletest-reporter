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
import org.gradle.api.tasks.TaskProvider;

public class TableTestReporterPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // Register extension with defaults
        TableTestReporterExtension ext = project.getExtensions()
            .create("tableTestReporter", TableTestReporterExtension.class);

        // Register task and wire conventions from extension
        TaskProvider<ReportTableTestsTask> task = project.getTasks().register(
            "reportTableTests", ReportTableTestsTask.class, t -> {
                t.getFormat().convention(ext.getFormat());
                t.getInputDir().convention(ext.getInputDir());
                t.getOutputDir().convention(ext.getOutputDir());
            }
        );

        // Make `build` depend on generation by default? Keep opt-in to avoid surprises.
        // Users will run `./gradlew reportTableTests` explicitly.
    }
}
