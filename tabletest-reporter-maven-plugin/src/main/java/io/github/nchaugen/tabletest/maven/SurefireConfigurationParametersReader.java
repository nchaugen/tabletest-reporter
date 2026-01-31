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

import io.github.nchaugen.tabletest.reporter.JunitDirParser;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SurefireConfigurationParametersReader {

    private static final String SUREFIRE_PLUGIN_KEY = "org.apache.maven.plugins:maven-surefire-plugin";
    private static final String PROPERTIES_NODE = "properties";
    private static final String PARAMETERS_NODE = "configurationParameters";
    private static final String OUTPUT_DIR_KEY = "junit.platform.reporting.output.dir";
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private SurefireConfigurationParametersReader() {}

    static Optional<Path> resolveOutputDir(MavenProject project, Path baseDir) {
        if (project == null || baseDir == null) {
            return Optional.empty();
        }
        return findConfigurationParameters(project)
                .flatMap(parameters -> extractOutputDir(parameters, project.getProperties()))
                .flatMap(value -> JunitDirParser.parse(baseDir, value));
    }

    private static Optional<Xpp3Dom> findConfigurationParameters(MavenProject project) {
        Optional<Plugin> plugin = project.getBuildPlugins().stream()
                .filter(candidate -> SUREFIRE_PLUGIN_KEY.equals(candidate.getKey()))
                .findFirst();
        if (plugin.isEmpty()) {
            return Optional.empty();
        }
        Object configuration = plugin.get().getConfiguration();
        if (!(configuration instanceof Xpp3Dom dom)) {
            return Optional.empty();
        }
        Xpp3Dom properties = dom.getChild(PROPERTIES_NODE);
        if (properties == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(properties.getChild(PARAMETERS_NODE));
    }

    private static Optional<String> extractOutputDir(Xpp3Dom parametersNode, Properties properties) {
        if (parametersNode.getChildCount() > 0) {
            for (Xpp3Dom child : parametersNode.getChildren()) {
                if (OUTPUT_DIR_KEY.equals(child.getName())) {
                    String value =
                            resolvePlaceholders(child.getValue(), properties).trim();
                    return value.isBlank() ? Optional.empty() : Optional.of(value);
                }
            }
        }

        String rawParameters = parametersNode.getValue();
        if (rawParameters == null || rawParameters.isBlank()) {
            return Optional.empty();
        }
        String resolved = resolvePlaceholders(rawParameters, properties);
        return parseOutputDir(resolved);
    }

    private static Optional<String> parseOutputDir(String parameters) {
        String[] lines = parameters.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int separator = trimmed.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String key = trimmed.substring(0, separator).trim();
            if (!OUTPUT_DIR_KEY.equals(key)) {
                continue;
            }
            String value = trimmed.substring(separator + 1).trim();
            if (!value.isBlank()) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    private static String resolvePlaceholders(String value, Properties properties) {
        if (value == null) {
            return "";
        }
        Matcher matcher = PROPERTY_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String propertyName = matcher.group(1);
            String replacement =
                    Optional.ofNullable(properties.getProperty(propertyName)).orElse("");
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
