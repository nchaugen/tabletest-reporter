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
package io.github.nchaugen.tabletest.maven;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportMojoTest {

    @TempDir
    Path tempDir;

    @Test
    void execute_generates_output_from_minimal_yaml_asciidoc_default() throws Exception {
        // Arrange: create input/output directories and minimal YAML structure
        Path inDir = tempDir.resolve("in");
        Path testClassDir = inDir.resolve("org.example.CalendarTest");
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

        Path outDir = tempDir.resolve("out");

        ReportMojo mojo = new ReportMojo();
        setField(mojo, "format", "asciidoc");
        setField(mojo, "inputDirectory", inDir.toFile());
        setField(mojo, "outputDirectory", outDir.toFile());

        // Act
        mojo.execute();

        // Assert
        assertTrue(Files.exists(outDir.resolve("index.adoc")));
        assertTrue(Files.isDirectory(outDir.resolve("calendar-calculations")));
        assertTrue(Files.exists(outDir.resolve("calendar-calculations").resolve("leap-year-rules.adoc")));
    }

    @Test
    void execute_fails_when_input_directory_missing() throws Exception {
        ReportMojo mojo = new ReportMojo();
        setField(mojo, "format", "markdown");
        setField(mojo, "inputDirectory", new File(tempDir.resolve("missing").toString()));
        setField(mojo, "outputDirectory", tempDir.resolve("out").toFile());

        MojoFailureException ex = assertThrows(MojoFailureException.class, mojo::execute);
        assertTrue(ex.getMessage().toLowerCase().contains("input directory"));
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
