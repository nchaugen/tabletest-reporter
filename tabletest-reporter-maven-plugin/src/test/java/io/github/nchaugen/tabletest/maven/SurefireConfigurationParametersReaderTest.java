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

import org.apache.maven.model.Build;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SurefireConfigurationParametersReaderTest {

    @TempDir
    Path baseDir;

    @Test
    void resolves_output_dir_from_configuration_parameters() {
        MavenProject project = new MavenProject();
        project.getProperties()
                .setProperty("custom.junit.parameters", "junit.platform.reporting.output.dir=target/custom-reports");

        Plugin surefire = new Plugin();
        surefire.setGroupId("org.apache.maven.plugins");
        surefire.setArtifactId("maven-surefire-plugin");
        surefire.setConfiguration(surefireConfiguration());

        Build build = new Build();
        build.addPlugin(surefire);
        project.setBuild(build);

        Optional<Path> resolved = SurefireConfigurationParametersReader.resolveOutputDir(project, baseDir);

        assertThat(resolved).contains(baseDir.resolve("target/custom-reports"));
    }

    private static Xpp3Dom surefireConfiguration() {
        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom properties = new Xpp3Dom("properties");
        Xpp3Dom configurationParameters = new Xpp3Dom("configurationParameters");
        configurationParameters.setValue("""
                junit.jupiter.extensions.autodetection.enabled=true
                ${custom.junit.parameters}
                """);
        properties.addChild(configurationParameters);
        configuration.addChild(properties);
        return configuration;
    }
}
