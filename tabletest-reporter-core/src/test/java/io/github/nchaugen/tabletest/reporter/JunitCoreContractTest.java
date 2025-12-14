package io.github.nchaugen.tabletest.reporter;

import io.github.nchaugen.tabletest.junit.Description;
import io.github.nchaugen.tabletest.junit.TableTest;
import io.github.nchaugen.tabletest.reporter.junit.TableTestPublisher;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.OutputDirectoryCreator;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Contract tests verifying that the core module can consume YAML files
 * produced by the junit module. These tests run actual JUnit tests using
 * the junit extension, then verify the core module can parse and render
 * the resulting YAML files.
 */
class JunitCoreContractTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldConsumeYamlFilesProducedByJunitExtension() throws IOException {
        EngineTestKit
            .engine("junit-jupiter")
            .selectors(selectClass(SampleTableTest.class))
            .configurationParameter("junit.platform.output.dir", tempDir.toString())
            .enableImplicitConfigurationParameters(true)
            .outputDirectoryCreator(createOutputDirectoryCreator())
            .execute();

        List<Path> yamlFiles = Files.walk(tempDir)
            .filter(p -> p.toString().endsWith(".yaml"))
            .toList();

        assertThat(yamlFiles)
            .describedAs("Junit extension should produce YAML files")
            .isNotEmpty();

        Map<String, Object> tree = ReportTree.process(tempDir);
        assertThat(tree)
            .describedAs("Core should process junit-produced YAML into report tree")
            .isNotNull();

        TableTestReporter reporter = new TableTestReporter();
        Path outDir = tempDir.resolve("output");
        Files.createDirectories(outDir);

        reporter.report(ReportFormat.ASCIIDOC, tempDir, outDir);

        List<Path> renderedFiles = Files.walk(outDir)
            .filter(Files::isRegularFile)
            .toList();

        assertThat(renderedFiles)
            .describedAs("Should render output files from junit-produced YAML")
            .isNotEmpty();
    }

    @Test
    void shouldHandleNestedTestClasses() throws IOException {
        EngineTestKit
            .engine("junit-jupiter")
            .selectors(selectClass(OuterTestClass.class))
            .configurationParameter("junit.platform.output.dir", tempDir.toString())
            .enableImplicitConfigurationParameters(true)
            .outputDirectoryCreator(createOutputDirectoryCreator())
            .execute();

        List<Path> yamlFiles = Files.walk(tempDir)
            .filter(p -> p.toString().endsWith(".yaml"))
            .toList();

        assertThat(yamlFiles)
            .describedAs("Should create YAML files for nested classes and their tables")
            .hasSize(4);

        long testClassFiles = yamlFiles.stream()
            .filter(p -> p.getFileName().toString().contains("-class"))
            .count();

        assertThat(testClassFiles)
            .describedAs("Should create test class YAML files (with '-class' suffix)")
            .isEqualTo(2);

        Path outerClassYaml = yamlFiles.stream()
            .filter(p -> p.getFileName().toString().equals("TABLETEST-outer-test-class.yaml"))
            .findFirst()
            .orElse(null);

        assertThat(outerClassYaml)
            .describedAs("Should create test class YAML for outer class")
            .isNotNull()
            .exists();

        Path nestedClassYaml = yamlFiles.stream()
            .filter(p -> p.getFileName().toString().equals("TABLETEST-nested-test-class.yaml"))
            .findFirst()
            .orElse(null);

        assertThat(nestedClassYaml)
            .describedAs("Should create test class YAML for nested class")
            .isNotNull()
            .exists();

        List<Path> allFiles = Files.walk(tempDir).toList();
        boolean allFilesInRoot = yamlFiles.stream()
            .allMatch(f -> f.getParent().equals(tempDir));

        assertThat(allFilesInRoot)
            .describedAs("Currently, junit extension creates all YAML files in root directory (no subdirectories). Files: " + yamlFiles)
            .isTrue();

        Map<String, Object> tree = ReportTree.process(tempDir);
        assertThat(tree)
            .describedAs("Core should handle nested class structure")
            .isNotNull();

        assertThat(tree.get("type")).isEqualTo("index");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contents = (List<Map<String, Object>>) tree.get("contents");
        assertThat(contents)
            .describedAs("Should have nested structure in report tree")
            .isNotEmpty();
    }

    @Test
    void shouldVerifyOutputDirectoryCreatorBehavior() throws IOException {
        Path customOutputDir = tempDir.resolve("with-custom-creator");
        Files.createDirectories(customOutputDir);

        OutputDirectoryCreator creatorThatCreatesSubdirectories = new OutputDirectoryCreator() {
            @Override
            public Path getRootDirectory() {
                return customOutputDir;
            }

            @Override
            public Path createOutputDirectory(TestDescriptor testDescriptor) throws IOException {
                String uniqueId = testDescriptor.getUniqueId().toString();
                String sanitized = uniqueId.replaceAll("[^a-zA-Z0-9]", "_");
                Path subdir = customOutputDir.resolve(sanitized);
                return Files.createDirectories(subdir);
            }
        };

        EngineTestKit
            .engine("junit-jupiter")
            .selectors(selectClass(OuterTestClass.class))
            .configurationParameter("junit.platform.output.dir", customOutputDir.toString())
            .enableImplicitConfigurationParameters(true)
            .outputDirectoryCreator(creatorThatCreatesSubdirectories)
            .execute();

        List<Path> yamlFiles = Files.walk(customOutputDir)
            .filter(p -> p.toString().endsWith(".yaml"))
            .toList();

        List<Path> directories = Files.walk(customOutputDir)
            .filter(Files::isDirectory)
            .filter(p -> !p.equals(customOutputDir))
            .toList();

        assertThat(yamlFiles)
            .describedAs("Should create YAML files")
            .isNotEmpty();

        assertThat(directories)
            .describedAs("OutputDirectoryCreator that creates subdirectories will have files in subdirectories")
            .isNotEmpty();

        boolean allFilesInSubdirectories = yamlFiles.stream()
            .noneMatch(f -> f.getParent().equals(customOutputDir));

        assertThat(allFilesInSubdirectories)
            .describedAs("When OutputDirectoryCreator creates subdirectories, files go into those subdirectories")
            .isTrue();
    }

    @Test
    void shouldPreserveFilenameTransformations() throws IOException {
        EngineTestKit
            .engine("junit-jupiter")
            .selectors(selectClass(CamelCaseTestClass.class))
            .configurationParameter("junit.platform.output.dir", tempDir.toString())
            .enableImplicitConfigurationParameters(true)
            .outputDirectoryCreator(createOutputDirectoryCreator())
            .execute();

        List<Path> yamlFiles = Files.walk(tempDir)
            .filter(p -> p.toString().endsWith(".yaml"))
            .filter(p -> p.toString().contains("TABLETEST-"))
            .toList();

        assertThat(yamlFiles)
            .describedAs("Should produce transformed filenames")
            .isNotEmpty();

        boolean hasKebabCaseFilename = yamlFiles.stream()
            .anyMatch(p -> p.getFileName().toString().contains("-"));

        assertThat(hasKebabCaseFilename)
            .describedAs("Should transform camelCase to kebab-case")
            .isTrue();

        Map<String, Object> tree = ReportTree.process(tempDir);
        assertThat(tree)
            .describedAs("Core should handle transformed filenames")
            .isNotNull();
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

    @DisplayName("Sample Table Test")
    @Description("A sample test for contract validation")
    @ExtendWith(TableTestPublisher.class)
    static class SampleTableTest {
        @TableTest("""
            Scenario        | Input | Expected?
            Simple addition | 2     | 4
            Another row     | 3     | 9
            """)
        void shouldSquareNumbers(int input, int expected) {
            assertThat(input * input).isEqualTo(expected);
        }
    }

    @DisplayName("Outer Test Class")
    @ExtendWith(TableTestPublisher.class)
    static class OuterTestClass {
        @TableTest("""
            Value | Result?
            1     | 1
            """)
        void outerTest(int value, int result) {
            assertThat(value).isEqualTo(result);
        }

        @Nested
        @DisplayName("Nested Test Class")
        class NestedTestClass {
            @TableTest("""
                Value | Result?
                2     | 2
                """)
            void nestedTest(int value, int result) {
                assertThat(value).isEqualTo(result);
            }
        }
    }

    @DisplayName("CamelCase Test Class")
    @ExtendWith(TableTestPublisher.class)
    static class CamelCaseTestClass {
        @TableTest("""
            Input | Output?
            1     | 1
            """)
        void testMethodInCamelCase(int input, int output) {
            assertThat(input).isEqualTo(output);
        }
    }
}
