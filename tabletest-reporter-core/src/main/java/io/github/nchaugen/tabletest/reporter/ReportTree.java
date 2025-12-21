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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

public class ReportTree {

    private static final String FILENAME_PREFIX = "TABLETEST-";
    private static final String YAML_EXTENSION = ".yaml";
    private static final Path ROOT_PATH = Path.of("." + File.separator);

    /**
     * Processes the top-level directory where TableTest .yaml files have been created during test run
     * and builds a typed node hierarchy describing the desired report structure.
     *
     * @param dir junit-jupiter output directory
     * @return typed node hierarchy describing the desired report structure
     */
    public static ReportNode process(Path dir) {
        return Optional.ofNullable(dir)
            .map(ReportTree::findTableTestOutputFiles)
            .map(ReportTree::findTargets)
            .map(ReportTree::buildTree)
            .orElseThrow(() -> new NullPointerException("argument `dir` cannot be null"));
    }

    /**
     * Walks the given directory and returns all files ending with .yaml as paths relative to the given directory.
     * @param dir junit-jupiter output directory
     * @return List of relative paths to .yaml files
     */
    static List<Path> findTableTestOutputFiles(Path dir) {
        try (var paths = Files.walk(dir)) {
            return paths.filter(ReportTree::isTableTestOutputFile)
                .map(dir::relativize)
                .sorted().toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decides whether the given path is a TableTest output file.
     */
    private static boolean isTableTestOutputFile(Path path) {
        return path.toFile().isFile()
            && path.getFileName().toString().startsWith(FILENAME_PREFIX)
            && path.getFileName().toString().endsWith(YAML_EXTENSION);
    }

    /**
     * Builds a list of target output files from the given list of input files:
     * <ul>
     * <li>All TableTest methods (will have a corresponding .yaml input file)</li>
     * <li>All Test classes with TableTest methods (might have corresponding .yaml input files)</li>
     * <li>Packages containing targeted Test classes (if there is more than one)</li>
     * </ul>
     * <p>
     * An index file with a table of contents will be generated for each package and test class.
     * <p>
     * Packages are targeted to organize Test classes in a hierarchical structure, e.g. the test classes
     * com.example.product.ProductTest and com.example.order.OrderTest will be organized in the following structure:
     * <ul>
     * <li>product<ul><li>ProductTest</li></ul></li>
     * <li>order<ul><li>OrderTest</li></ul></li>
     * </ul>
     * @param files list of input TableTest yaml files
     * @return list of target output files
     */
    static List<Target> findTargets(List<Path> files) {
        return Optional.ofNullable(files)
            .map(ReportTree::generateAllTargets)
            .map(ReportTree::pickNearestRoot)
            .map(ReportTree::removeDuplicates)
            .map(ReportTree::sortByTarget)
            .orElseThrow(() -> new NullPointerException("argument `files` cannot be null"));
    }

    /**
     * Builds a typed node hierarchy describing the desired report structure with all available information to make
     * available to the renderer.
     * <p>
     * Index nodes contain:
     * <ul>
     * <li>name: name of the package or test class</li>
     * <li>outPath: sluggified path for the rendered index file</li>
     * <li>resource: path to the corresponding .yaml file (if available)</li>
     * <li>contents: list of child nodes</li>
     * </ul>
     * <p>
     * Table leaf-nodes contain:
     * <ul>
     * <li>name: name of the table (derived from the .yaml file name)</li>
     * <li>outPath: slugified path for the rendered table file</li>
     * <li>resource: path to the corresponding .yaml file</li>
     * </ul>
     * @param targets list of target output files
     * @return desired report structure as a typed node hierarchy, or null if no valid targets
     */
    static ReportNode buildTree(List<Target> targets) {
        if (targets.isEmpty() || targets.size() == 1 && targets.getFirst().hasNoResource()) return null;
        Target root = findRoot(targets);
        return buildTree(root, List.of(root), targets);
    }

    /**
     * Finds the root node in the given list of targets.
     */
    private static Target findRoot(List<Target> targets) {
        return targets.stream().min(comparing(Target::path)).orElseThrow();
    }

    /**
     * Recursively builds the typed node hierarchy
     * @param node next node to process
     * @param path path to next node from root
     * @param targets list of all available targets
     * @return typed node (IndexNode or TableNode)
     */
    private static ReportNode buildTree(Target node, List<Target> path, List<Target> targets) {
        List<Target> children = targets.stream().filter(node::isParentOf).toList();

        String name = node.hasName() ? node.name() : null;
        String outPath = createOutPath(path);
        String resource = node.hasResource() ? node.resource().toString() : null;

        if (children.isEmpty()) {
            return new TableNode(name, outPath, resource);
        } else {
            List<ReportNode> childNodes = children.stream()
                .map(target -> buildTree(target, concat(path, target), targets))
                .toList();
            return new IndexNode(name, outPath, resource, childNodes);
        }
    }

    /**
     * Converts a list of targets to a path string.
     * Filenames are already transformed by the junit module before YAML files are created.
     */
    private static String createOutPath(List<Target> path) {
        return path.stream().map(Target::pathName).collect(joining(File.separator));
    }

    /**
     * Adds a target to the immutable path list
     */
    private static List<Target> concat(List<Target> path, Target node) {
        return Stream.concat(path.stream(), Stream.of(node)).toList();
    }

    /**
     * Sorts a list of targets by their path, the shortest first
     */
    private static List<Target> sortByTarget(List<Target> targets) {
        return targets.stream().sorted(comparing(Target::path)).toList();
    }

    /**
     * Removes duplicate targets from the given list, keeping the ones with most information
     */
    private static List<Target> removeDuplicates(List<Target> targets) {
        return targets.stream()
            .collect(Collectors.groupingBy(Target::path))
            .entrySet().stream()
            .map(ReportTree::pickTargetInstance)
            .map(it -> it.mapPath(ROOT_PATH::resolve))
            .toList();
    }

    /**
     * Picks the point where the package hierarchy starts branching as the root of the output structure
     */
    private static List<Target> pickNearestRoot(List<Target> targets) {
        long resourceCount = countResources(targets);
        Map<Boolean, List<Target>> rootCandidates = partitionByRootCandidacy(targets, resourceCount);
        Target root = findNearestRoot(rootCandidates.get(true));

        return Stream.concat(Stream.of(root), rootCandidates.get(false).stream())
            .map(target -> target.mapPath(root.path::relativize))
            .toList();
    }

    /**
     * Count the number of targets with a resource file, i.e. the original number of input .yaml files.
     */
    private static long countResources(List<Target> targets) {
        return targets.stream().filter(Target::hasResource).count();
    }

    /**
     * Partitions the given list of targets into two groups:
     * <ul>
     * <li></li>Targets that are potential root nodes</li>
     * <li></li>Targets that are not potential root nodes</li>
     * </ul>
     * <p>
     * Potential root nodes are identified by their path being the start of all targets with resource files.
     */
    private static Map<Boolean, List<Target>> partitionByRootCandidacy(List<Target> targets, long resourceCount) {
        return targets.stream()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet().stream()
            .collect(Collectors.partitioningBy(
                e -> e.getValue() == resourceCount,
                Collectors.mapping(Map.Entry::getKey, Collectors.toList())
            ));
    }

    /**
     * Finds the target with the most path segments in the given list of common roots
     */
    private static Target findNearestRoot(List<Target> commonRoots) {
        return commonRoots.stream()
            .max(comparing(target -> target.path().getNameCount()))
            .orElseGet(() -> Target.withPath("").withName(null));
    }

    /**
     * Expands the list of input files into a list of all possible target output files, i.e. adding an index file for
     * each relevant test class and package
     */
    private static List<Target> generateAllTargets(List<Path> files) {
        return files.stream()
            .map(ReportTree::directoryPerTestClassPackageComponents)
            .flatMap(ReportTree::getAllAncestors)
            .toList();
    }

    /**
     * Finds the target instance to keep for a given file path, picking the one with a resource file if available,
     * otherwise picking the first target in the list.
     */
    private static Target pickTargetInstance(Map.Entry<Path, List<Target>> entry) {
        List<Target> candidates = entry.getValue();
        if (candidates.size() == 1) return candidates.getFirst();
        return candidates.stream()
            .filter(Target::hasResource)
            .findFirst()
            .orElse(candidates.getFirst());
    }

    /**
     * Uses the naming standard of junit-jupiter output directories to get the fully qualified name of each
     * test class and creating a target path with directory per package component.
     */
    private static Target directoryPerTestClassPackageComponents(Path file) {
        // JUnit puts published files in a directory per test class
        String className = file.getName(0).toString();

        // Split the fully qualified class name into directory per package component
        Path classNamePath = prefix(file).resolve(className.replace('.', File.separatorChar));

        // Replace fully qualified class name directory with directory per package
        Path remainingPath = (file.getRoot() == null
            ? ROOT_PATH
            : file.getRoot()).resolve(className).relativize(file);

        return Target.withPath(classNamePath.resolve(remainingPath)).withResource(file);
    }

    private static Path prefix(Path file) {
        return file.getRoot() == null ? ROOT_PATH : file.getRoot();
    }

    /**
     * Generates a stream of ancestors for the given target, starting with the parent and ending with the root
     */
    private static Stream<Target> getAllAncestors(Target target) {
        return Stream.iterate(
            target.mapPath(Path::getParent),
            it -> Objects.nonNull(it.path()),
            it -> Target.withPath(it.path().getParent())
        );
    }

    /**
     * Represents a target in the report structure. A Target can be in one of several states:
     * 1. Path-only: Has path, but no resource (intermediate directory node)
     * 2. With resource: Has path and resource - name derived from resource file
     * 3. Named: Has explicit name set via withName() - overrides derived name
     */
    public record Target(String name, Path path, Path resource) {


        public static Target withPath(String target) {
            return withPath(Path.of(target));
        }

        /**
         * Creates a Target with path. Name is derived from path's filename.
         */
        public static Target withPath(Path path) {
            String name = path != null && path.getFileName() != null
                ? path.getFileName().toString()
                : null;
            return new Target(name, path, null);
        }

        /**
         * Adds a resource to this Target. Name is replaced with resource filename (without .yaml extension).
         */
        public Target withResource(Path resource) {
            String name = resource.getFileName().toString()
                .replaceAll("^TABLETEST-", "")
                .replaceAll(YAML_EXTENSION + "$", "");
            return new Target(name, path, resource);
        }

        /**
         * Explicitly sets the name, overriding any derived name.
         */
        public Target withName(String name) {
            return new Target(name, path, resource);
        }

        /**
         * Transforms the path using the given function. Name is preserved (not derived from new path).
         */
        Target mapPath(Function<Path, Path> transform) {
            return new Target(name, transform.apply(path), resource);
        }

        public boolean hasResource() {
            return resource != null;
        }

        public boolean hasNoResource() {
            return !hasResource();
        }

        public boolean isParentOf(Target target) {
            return path.equals(target.path().getParent());
        }

        public boolean hasName() {
            return name != null;
        }

        public String pathName() {
            return isRoot() ? "" : name().toLowerCase();
        }

        private boolean isRoot() {
            return path.getParent() == null;
        }

        @Override
        public String toString() {
            return "Target[name=" + name + ", path=" + path + ", resource=" + resource + "]";
        }
    }

}
