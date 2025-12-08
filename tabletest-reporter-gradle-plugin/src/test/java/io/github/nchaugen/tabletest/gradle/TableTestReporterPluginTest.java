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

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TableTestReporterPluginTest {

    @TempDir
    Path projectDir;

    private Project project;

    @BeforeEach
    void setUp() {
        project = ProjectBuilder.builder()
            .withProjectDir(projectDir.toFile())
            .build();

        project.getPluginManager().apply(TableTestReporterPlugin.class);
    }

    @Test
    void reportTask_generates_output_from_minimal_yaml() throws IOException {
        // Arrange: create minimal input structure under build/junit-jupiter
        Path buildDir = project.getLayout().getBuildDirectory().get().getAsFile().toPath();
        Path inputRoot = buildDir.resolve("junit-jupiter");
        Path testClassDir = inputRoot.resolve("org.example.CalendarTest");
        Path tableDir = testClassDir.resolve("leapYearRules(java.time.Year, boolean)");
        Files.createDirectories(tableDir);

        Files.writeString(testClassDir.resolve("Calendar Calculations.yaml"), """
            "title": "Calendar"
            "description": "Various rules for calendar calculations."
            """
        );

        Files.writeString(tableDir.resolve("Leap Year Rules.yaml"), """
            "title": "Leap Year Rules with Single Example"
            "description": "The leap year rules should be well-known."
            "headers":
              - "value": "Scenario"
              - "value": "Year"
              - "value": "Is Leap Year?"
            "rows":
                - - "value": "Not divisible by 4"
                  - "value": "2001"
                  - "value": "No"
                - - "value": "Divisible by 4"
                  - "value": "2004"
                  - "value": "Yes"
            """
        );

        // Act: run the task directly
        ReportTableTestsTask task = (ReportTableTestsTask) project.getTasks().getByName("reportTableTests");
        task.run();

        // Assert: default AsciiDoc outputs exist under build/generated-docs/tabletest
        Path outRoot = buildDir.resolve("generated-docs").resolve("tabletest");
        assertTrue(Files.exists(outRoot.resolve("index.adoc")));
        assertTrue(Files.isDirectory(outRoot.resolve("calendar-calculations")));
        assertTrue(Files.exists(outRoot.resolve("calendar-calculations").resolve("leap-year-rules.adoc")));
    }
}
