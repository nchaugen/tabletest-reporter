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
import io.github.nchaugen.tabletest.junit.Scenario;
import io.github.nchaugen.tabletest.junit.TableTest;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.OutputDirectoryCreator;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

class TableTestPublisherEndToEndTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPublishYamlForPassingTableTest() throws IOException {
        var results = EngineTestKit
            .engine("junit-jupiter")
            .selectors(selectClass(AllRowsPassTest.class))
            .configurationParameter("junit.platform.output.dir", tempDir.toString())
            .enableImplicitConfigurationParameters(true)
            .outputDirectoryCreator(createOutputDirectoryCreator())
            .execute();

        results.testEvents()
            .assertStatistics(stats -> stats
                .started(3)
                .succeeded(3)
                .failed(0));

        Path yamlFile = findYamlFile(tempDir, "All rows pass");
        assertTrue(Files.exists(yamlFile), "YAML file should exist");

        assertEquals(
            """
                "title": "All rows pass"
                "description": "Verifying published result when all rows pass."
                "headers":
                - "value": "a"
                - "value": "b"
                - "value": "sum?"
                  "roles":
                  - "expectation"
                  - "scenario"
                "rows":
                - - "value": "1"
                  - "value": "1"
                  - "value": "2"
                    "roles":
                    - "expectation"
                    - "scenario"
                - - "value": "2"
                  - "value": "2"
                  - "value": "4"
                    "roles":
                    - "expectation"
                    - "scenario"
                - - "value": "3"
                  - "value": "3"
                  - "value": "6"
                    "roles":
                    - "expectation"
                    - "scenario"
                "rowResults":
                - "rowIndex": !!int "1"
                  "passed": !!bool "true"
                  "displayName": "[1] 2"
                - "rowIndex": !!int "2"
                  "passed": !!bool "true"
                  "displayName": "[2] 4"
                - "rowIndex": !!int "3"
                  "passed": !!bool "true"
                  "displayName": "[3] 6"
                """,
            Files.readString(yamlFile)
        );
    }

    @Test
    void shouldPublishYamlForFailingTableTest() throws IOException {
        var results = EngineTestKit
            .engine("junit-jupiter")
            .selectors(selectClass(OneRowFailsTest.class))
            .configurationParameter("junit.platform.output.dir", tempDir.toString())
            .enableImplicitConfigurationParameters(true)
            .outputDirectoryCreator(createOutputDirectoryCreator())
            .execute();

        results.testEvents()
            .assertStatistics(stats -> stats
                .started(3)
                .succeeded(2)
                .failed(1));

        Path yamlFile = findYamlFile(tempDir, "One row pass");
        assertTrue(Files.exists(yamlFile), "YAML file should exist");

        assertEquals(
            """
                "title": "One row pass"
                "description": "Verifying published result when there is a row failure."
                "headers":
                - "value": "Scenario"
                  "roles":
                  - "scenario"
                - "value": "a"
                - "value": "b"
                - "value": "sum?"
                  "roles":
                  - "expectation"
                "rows":
                - - "value": "Should pass"
                    "roles":
                    - "scenario"
                  - "value": "1"
                  - "value": "1"
                  - "value": "2"
                    "roles":
                    - "expectation"
                - - "value": "Should fail"
                    "roles":
                    - "scenario"
                  - "value": "2"
                  - "value": "2"
                  - "value": "5"
                    "roles":
                    - "expectation"
                - - "value": "Should also pass"
                    "roles":
                    - "scenario"
                  - "value": "3"
                  - "value": "3"
                  - "value": "6"
                    "roles":
                    - "expectation"
                "rowResults":
                - "rowIndex": !!int "1"
                  "passed": !!bool "true"
                  "displayName": "[1] Should pass"
                - "rowIndex": !!int "2"
                  "passed": !!bool "false"
                  "displayName": "[2] Should fail"
                  "errorMessage": "expected: <5> but was: <4>"
                - "rowIndex": !!int "3"
                  "passed": !!bool "true"
                  "displayName": "[3] Should also pass"
                """,
            Files.readString(yamlFile)
        );
    }

    private @NonNull OutputDirectoryCreator createOutputDirectoryCreator() {
        return new OutputDirectoryCreator() {
            @Override
            public Path getRootDirectory() {
                return tempDir;
            }

            @Override
            public Path createOutputDirectory(TestDescriptor testDescriptor) throws IOException {
                return tempDir;
            }
        };
    }

    @Test
    void shouldPublishTestClassYaml() throws IOException {
        var results = EngineTestKit
            .engine("junit-jupiter")
            .selectors(selectClass(AllRowsPassTest.class))
            .configurationParameter("junit.platform.output.dir", tempDir.toString())
            .enableImplicitConfigurationParameters(true)
            .outputDirectoryCreator(createOutputDirectoryCreator())
            .execute();

        results.testEvents()
            .assertStatistics(stats -> stats
                .started(3)
                .succeeded(3)
                .failed(0));

        Path classYamlFile = findYamlFile(tempDir, "Verifying YAML Output");
        assertTrue(Files.exists(classYamlFile), "Class YAML file should exist");

        assertEquals(
            """
                "title": "Verifying YAML Output"
                "description": "This test class verified that the published YAML files contain the expected output."
                """,
            Files.readString(classYamlFile)
        );
    }

    private Path findYamlFile(Path baseDir, String name) throws IOException {
        try (var paths = Files.walk(baseDir)) {
            return paths
                .filter(p -> p.toString().contains(name))
                .filter(p -> p.getFileName().toString().startsWith("TABLETEST-"))
                .filter(p -> p.getFileName().toString().endsWith(".yaml"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("TABLETEST-*.yaml file not found for " + name));
        }
    }

    // Test classes that will be executed by EngineTestKit
    // These must be public for EngineTestKit to discover them

    @DisplayName("Verifying YAML Output")
    @Description("This test class verified that the published YAML files contain the expected output.")
    @ExtendWith(TableTestPublisher.class)
    public static class AllRowsPassTest {
        @DisplayName("All rows pass")
        @Description("Verifying published result when all rows pass.")
        @TableTest("""
            a | b | sum?
            1 | 1 | 2
            2 | 2 | 4
            3 | 3 | 6
            """)
        public void allRowsPass(int a, int b, @Scenario int sum) {
            assertEquals(sum, a + b);
        }
    }

    @ExtendWith(TableTestPublisher.class)
    public static class OneRowFailsTest {
        @DisplayName("One row pass")
        @Description("Verifying published result when there is a row failure.")
        @TableTest("""
            Scenario         | a | b | sum?
            Should pass      | 1 | 1 | 2
            Should fail      | 2 | 2 | 5
            Should also pass | 3 | 3 | 6
            """)
        public void oneRowFails(int a, int b, int sum) {
            assertEquals(sum, a + b);
        }
    }
}
