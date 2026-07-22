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
package org.tabletest.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.jetbrains.annotations.Nullable;
import org.tabletest.reporter.InputDirectoryResolver;
import org.tabletest.reporter.JunitDirParser;
import org.tabletest.reporter.JunitPropertiesReader;
import org.tabletest.reporter.ReportConfiguration;
import org.tabletest.reporter.ReportConfigurationResolver;
import org.tabletest.reporter.ReportOptions;
import org.tabletest.reporter.ReportResult;
import org.tabletest.reporter.TableTestReporter;

import javax.inject.Inject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Gradle task for generating documentation from TableTest YAML outputs.
 * <p>
 * Reads YAML files produced by the TableTest JUnit extension and generates
 * human-readable documentation in AsciiDoc or Markdown format.
 */
@CacheableTask
public abstract class ReportTableTestsTask extends DefaultTask {

    private final ObjectFactory objects;
    private final Property<String> format;
    private final DirectoryProperty inputDir;
    private final DirectoryProperty outputDir;
    private final DirectoryProperty templateDir;
    private final Property<String> junitOutputDir;
    private final Property<String> indexDepth;
    private final RegularFileProperty configFile;
    private final DirectoryProperty projectDir;
    private final DirectoryProperty defaultInputDir;
    private final ConfigurableFileCollection sourceYamlFiles;
    private final ConfigurableFileCollection configFileInput;

    /**
     * Creates a new task instance with default configuration.
     */
    @Inject
    public ReportTableTestsTask() {
        this.objects = getProject().getObjects();
        this.format = objects.property(String.class);
        this.inputDir = objects.directoryProperty();
        this.outputDir = objects.directoryProperty();
        this.templateDir = objects.directoryProperty();
        this.junitOutputDir = objects.property(String.class);
        this.indexDepth = objects.property(String.class);
        this.configFile = objects.fileProperty();
        this.projectDir = objects.directoryProperty();
        this.defaultInputDir = objects.directoryProperty();
        this.sourceYamlFiles = objects.fileCollection();
        this.sourceYamlFiles.from((Callable<List<FileTree>>) this::candidateYamlTrees);
        this.configFileInput = objects.fileCollection();
        this.configFileInput.from((Callable<List<java.io.File>>) this::existingConfigFile);
        setGroup("documentation");
        setDescription("Generates AsciiDoc or Markdown documentation from TableTest YAML outputs");
    }

    /**
     * Returns the output format property.
     *
     * @return property for specifying output format (asciidoc or markdown)
     */
    @org.gradle.api.tasks.Optional
    @Input
    public Property<String> getFormat() {
        return format;
    }

    /**
     * Returns the input directory property.
     *
     * @return property for directory containing TableTest YAML files
     */
    @org.gradle.api.tasks.Optional
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public DirectoryProperty getInputDir() {
        return inputDir;
    }

    /**
     * Returns the output directory property.
     *
     * @return property for directory where generated documentation will be written
     */
    @OutputDirectory
    public DirectoryProperty getOutputDir() {
        return outputDir;
    }

    /**
     * Returns the template directory property.
     *
     * @return property for optional custom template directory
     */
    @org.gradle.api.tasks.Optional
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public DirectoryProperty getTemplateDir() {
        return templateDir;
    }

    /**
     * Returns the JUnit output directory property.
     *
     * @return property for overriding the default JUnit XML output directory
     */
    @org.gradle.api.tasks.Optional
    @Input
    public Property<String> getJunitOutputDir() {
        return junitOutputDir;
    }

    /**
     * Returns the index depth property.
     *
     * @return property for specifying how many levels to show in index files (1, 2, ..., or "infinite")
     */
    @org.gradle.api.tasks.Optional
    @Input
    public Property<String> getIndexDepth() {
        return indexDepth;
    }

    /**
     * Returns the report configuration file property.
     *
     * @return property for the tabletest-reporter.yaml file holding spec title, intro and chapter order
     */
    @Internal
    public RegularFileProperty getConfigFile() {
        return configFile;
    }

    /**
     * Returns the configuration file as a task input when it exists, so a change to the
     * tabletest-reporter.yaml invalidates the cached report. Tracked separately from
     * {@link #getConfigFile()} because the conventional path is often absent, and a declared-but-
     * missing {@code @InputFile} would fail the build.
     *
     * @return file collection holding the configuration file when present, otherwise empty
     */
    @org.gradle.api.tasks.Optional
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public FileCollection getConfigFileInput() {
        return configFileInput;
    }

    /**
     * Returns the project directory property.
     *
     * @return property for the project base directory used for resolving relative paths
     */
    @Internal
    public DirectoryProperty getProjectDir() {
        return projectDir;
    }

    /**
     * Returns the default input directory property.
     *
     * @return property for the default directory containing JUnit Jupiter YAML outputs
     */
    @Internal
    public DirectoryProperty getDefaultInputDir() {
        return defaultInputDir;
    }

    /**
     * Returns the TableTest YAML files across all candidate input directories. Tracked as task
     * inputs so up-to-date checks and the build cache notice new test output even when no
     * explicit input directory is configured.
     *
     * @return file collection of the YAML files the report is generated from
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public FileCollection getSourceYamlFiles() {
        return sourceYamlFiles;
    }

    private List<FileTree> candidateYamlTrees() {
        return candidateInputDirectories().stream().map(this::yamlTreeAt).toList();
    }

    /**
     * The directories the input resolution may read YAML from when no explicit input directory
     * is configured: the JUnit output directory override, the directory named in
     * junit-platform.properties, and the default build/junit-jupiter directory. An explicitly
     * configured input directory is tracked separately through {@link #getInputDir()}.
     */
    private List<Path> candidateInputDirectories() {
        List<Path> candidates = new ArrayList<>();
        Path baseDir = toPath(projectDir);
        if (baseDir != null) {
            JunitDirParser.parse(baseDir, junitOutputDir.getOrNull()).ifPresent(candidates::add);
            JunitPropertiesReader.resolve(baseDir).ifPresent(candidates::add);
        }
        Path defaultInput = toPath(defaultInputDir);
        if (defaultInput != null) {
            candidates.add(defaultInput);
        }
        return candidates;
    }

    private List<java.io.File> existingConfigFile() {
        Path path = resolvedConfigFile();
        return path != null && Files.isRegularFile(path) ? List.of(path.toFile()) : List.of();
    }

    private @Nullable Path resolvedConfigFile() {
        return Optional.ofNullable(configFile.getOrNull())
                .map(file -> file.getAsFile().toPath())
                .orElse(null);
    }

    private FileTree yamlTreeAt(Path directory) {
        ConfigurableFileTree tree = objects.fileTree();
        tree.setDir(directory.toFile());
        tree.include("**/*.yaml");
        return tree;
    }

    /**
     * Executes the task to generate documentation from TableTest YAML files.
     *
     * @throws GradleException if input directory does not exist or report generation fails
     */
    @TaskAction
    public void run() {
        final Path defaultInput = defaultInputDir.get().getAsFile().toPath();
        final Path configuredInput = Optional.ofNullable(toPath(inputDir))
                .filter(path -> !isSamePath(path, defaultInput))
                .orElse(null);
        final Path out = outputDir.get().getAsFile().toPath();

        final Path baseDir = projectDir.get().getAsFile().toPath();
        final String junitOutputDirValue = junitOutputDir.getOrNull();
        final Path junitDir = JunitDirParser.parse(baseDir, junitOutputDirValue).orElse(null);

        Path in = resolveInputDirectory(configuredInput, List.of(defaultInput), baseDir, junitDir);

        ReportConfiguration config = ReportConfigurationResolver.resolve(new ReportOptions(
                format.getOrNull(), toPath(templateDir), indexDepth.getOrNull(), null, resolvedConfigFile()));

        try {
            ReportResult result = new TableTestReporter(config.templateDirectory(), config.indexDepth())
                    .report(config.format(), in, out, config.singleFile(), config.specMetadata());
            logResult(result);
        } catch (Exception e) {
            throw new GradleException("Failed to generate TableTest report: " + e.getMessage(), e);
        }
    }

    private @Nullable Path toPath(DirectoryProperty property) {
        return Optional.ofNullable(property)
                .filter(DirectoryProperty::isPresent)
                .map(dir -> dir.get().getAsFile().toPath())
                .orElse(null);
    }

    private static Path resolveInputDirectory(
            Path configuredInput, List<Path> fallbackCandidates, Path baseDir, Path junitDir) {
        InputDirectoryResolver.Result inputResult =
                InputDirectoryResolver.resolve(configuredInput, fallbackCandidates, baseDir, junitDir);
        return Optional.ofNullable(inputResult.path())
                .filter(Files::exists)
                .orElseThrow(() -> new GradleException(inputResult.formatMissingInputMessage()));
    }

    private void logResult(ReportResult result) {
        if (result.filesGenerated() == 0) {
            getLogger().warn(result.message());
        } else {
            getLogger().lifecycle("Generated {} documentation file(s)", result.filesGenerated());
        }
    }

    private static boolean isSamePath(Path left, Path right) {
        return left.toAbsolutePath().normalize().equals(right.toAbsolutePath().normalize());
    }
}
