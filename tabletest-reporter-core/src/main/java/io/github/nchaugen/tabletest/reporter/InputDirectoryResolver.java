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
package io.github.nchaugen.tabletest.reporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class InputDirectoryResolver {

    public enum ResolutionSource {
        CONFIGURED,
        JUNIT_PROPERTY,
        FALLBACK,
        NONE
    }

    public record Result(Path path, ResolutionSource source, List<Path> candidates) {

        /**
         * Formats a human-readable error message describing the missing input directory
         * and the candidate locations that were searched.
         */
        public String formatMissingInputMessage() {
            String message = Optional.ofNullable(path)
                    .map(p -> "Input directory does not exist: " + p.toAbsolutePath())
                    .orElse("Input directory does not exist");
            if (candidates.isEmpty()) {
                return message;
            }
            String searched = candidates.stream()
                    .map(candidate -> "  - " + candidate.toAbsolutePath())
                    .collect(Collectors.joining(System.lineSeparator()));
            return message + System.lineSeparator() + "Searched locations:" + System.lineSeparator() + searched;
        }
    }

    private InputDirectoryResolver() {}

    public static Result resolve(
            Path configuredInputDir, List<Path> fallbackCandidates, Path baseDir, Path junitOutputDir) {
        Path base = baseDir != null ? baseDir : Path.of(".");
        Optional<Path> configured = Optional.ofNullable(configuredInputDir).map(input -> normalize(base, input));
        if (configured.isPresent()) {
            Path configuredPath = configured.get();
            return new Result(configuredPath, ResolutionSource.CONFIGURED, List.of(configuredPath));
        }

        List<Path> junitDirs = junitPropertyPaths(junitOutputDir, base);
        List<Path> fallbacks = normalizeCandidates(base, fallbackCandidates);

        List<Path> candidates =
                Stream.concat(junitDirs.stream(), fallbacks.stream()).distinct().toList();

        Optional<Path> junitWithOutputs =
                junitDirs.stream().filter(InputDirectoryResolver::hasOutputs).findFirst();
        if (junitWithOutputs.isPresent()) {
            return new Result(junitWithOutputs.get(), ResolutionSource.JUNIT_PROPERTY, candidates);
        }

        Optional<Path> fallbackWithOutputs =
                fallbacks.stream().filter(InputDirectoryResolver::hasOutputs).findFirst();
        if (fallbackWithOutputs.isPresent()) {
            return new Result(fallbackWithOutputs.get(), ResolutionSource.FALLBACK, candidates);
        }

        Optional<Path> existing = candidates.stream().filter(Files::exists).findFirst();
        ResolutionSource source = existing.map(
                        path -> junitDirs.contains(path) ? ResolutionSource.JUNIT_PROPERTY : ResolutionSource.FALLBACK)
                .orElse(ResolutionSource.NONE);

        return new Result(existing.orElse(null), source, candidates);
    }

    private static List<Path> junitPropertyPaths(Path junitOutputDir, Path baseDir) {
        Stream<Path> pluginProvided = Optional.ofNullable(junitOutputDir).stream();
        Stream<Path> fromProperties = JunitPropertiesReader.resolve(baseDir).stream();
        return Stream.concat(pluginProvided, fromProperties).distinct().toList();
    }

    private static List<Path> defaultCandidates(Path baseDir) {
        return List.of(baseDir.resolve("target/junit-jupiter"), baseDir.resolve("build/junit-jupiter"));
    }

    private static List<Path> normalizeCandidates(Path baseDir, List<Path> candidates) {
        List<Path> rawCandidates = candidates == null || candidates.isEmpty() ? defaultCandidates(baseDir) : candidates;
        return rawCandidates.stream()
                .filter(Objects::nonNull)
                .map(candidate -> normalize(baseDir, candidate))
                .distinct()
                .toList();
    }

    private static Path normalize(Path baseDir, Path input) {
        if (input.isAbsolute()) {
            return input.normalize();
        }
        return baseDir.resolve(input).normalize();
    }

    private static boolean hasOutputs(Path directory) {
        if (!Files.isDirectory(directory)) {
            return false;
        }
        return !TestOutputFileFinder.findTestOutputFiles(directory).isEmpty();
    }
}
