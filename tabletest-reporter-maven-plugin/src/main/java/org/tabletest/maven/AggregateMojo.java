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
package org.tabletest.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.tabletest.reporter.InputDirectories;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Publishes one spec for a whole reactor: every module's TableTest output is found the way the
 * single-project goal finds its own — the JUnit output directory a module configures, else its
 * {@code target/junit-jupiter} — and the modules are merged into a single report written to the
 * aggregator project. A module that produced no output simply contributes nothing, so a partial
 * build still publishes what it has. Runs on the aggregator project only; a build listing its
 * modules' directories explicitly can use the {@code report} goal's {@code inputDirectories}
 * instead.
 */
@Mojo(name = "aggregate", defaultPhase = LifecyclePhase.SITE, aggregator = true, threadSafe = true)
public final class AggregateMojo extends AbstractReportMojo {

    @Parameter(defaultValue = "${reactorProjects}", readonly = true)
    private List<MavenProject> reactorProjects;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            InputDirectories inputs = InputDirectories.resolve(moduleOutputDirectories(), toPath(baseDirectory));
            if (inputs.isEmpty()) {
                throw new MojoFailureException(
                        "No module in the reactor has TableTest output to report. Run the tests first, "
                                + "or point the report goal at the directories to read.");
            }
            getLog().info("Aggregating TableTest output from "
                    + inputs.present().size() + " module(s)");
            generateReport(inputs.present(), outputDirectory.toPath());
        } catch (MojoFailureException e) {
            // Propagate user/config failures as-is without wrapping
            throw e;
        } catch (IllegalArgumentException e) {
            throw new MojoFailureException(e.getMessage(), e);
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to aggregate TableTest report", e);
        }
    }

    /** Where each module of the reactor writes its TableTest output, in reactor order. */
    private List<Path> moduleOutputDirectories() {
        return reactorProjects == null
                ? List.of()
                : reactorProjects.stream().map(AggregateMojo::outputDirectoryOf).toList();
    }

    private static Path outputDirectoryOf(MavenProject module) {
        Path moduleBaseDir = module.getBasedir().toPath();
        return SurefireConfigurationParametersReader.resolveOutputDir(module, moduleBaseDir)
                .orElseGet(() -> buildDirectoryOf(module, moduleBaseDir).resolve("junit-jupiter"));
    }

    private static Path buildDirectoryOf(MavenProject module, Path moduleBaseDir) {
        return Optional.ofNullable(module.getBuild())
                .map(build -> Path.of(build.getDirectory()))
                .orElseGet(() -> moduleBaseDir.resolve("target"));
    }
}
