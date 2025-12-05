package io.github.nchaugen.tabletest.reporter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
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

    private static final String YAML_EXTENSION = ".yaml";
    public static final Path ROOT_PATH = Path.of("." + File.separator);

    public static Map<String, Object> walk(Path dir) {
        return Optional.ofNullable(dir)
            .map(ReportTree::findTableTestOutputFiles)
            .map(ReportTree::findTargets)
            .map(ReportTree::buildTree)
            .orElseThrow(() -> new NullPointerException("argument `dir` cannot be null"));
    }

    static List<Path> findTableTestOutputFiles(Path dir) {
        try (var paths = Files.walk(dir)) {
            return paths.filter(ReportTree::isTableTestOutputFile)
                .map(dir::relativize)
                .sorted().toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isTableTestOutputFile(Path p) {
        return p.toFile().isFile() && p.toString().endsWith(YAML_EXTENSION);
    }

    static List<Target> findTargets(List<Path> files) {
        return Optional.ofNullable(files)
            .map(ReportTree::generateAllTargets)
            .map(ReportTree::pickNearestRoot)
            .map(ReportTree::removeDuplicates)
            .map(ReportTree::sortByTarget)
            .orElseThrow(() -> new NullPointerException("argument `files` cannot be null"));
    }

    static Map<String, Object> buildTree(List<Target> targets) {
        if (targets.isEmpty() || targets.size() == 1 && targets.getFirst().hasNoResource()) return Map.of();
        Target root = findRoot(targets);
        return buildTree(root, List.of(root), targets);
    }

    private static Target findRoot(List<Target> targets) {
        return targets.stream().min(comparing(Target::path)).orElseThrow();
    }

    private static Map<String, Object> buildTree(Target node, List<Target> path, List<Target> targets) {
        List<Target> children = targets.stream().filter(node::isParentOf).toList();

        Map<String, Object> context = new HashMap<>();
        context.put("type", children.isEmpty() ? "table" : "index");
        if (node.hasName()) context.put("name", node.name());
        context.put("outPath", path.stream().map(Target::pathName).collect(joining(File.separator)));
        if (node.hasResource()) context.put("resource", node.resource().toString());
        if (!children.isEmpty()) {
            context.put(
                "contents", children.stream()
                    .map(target -> buildTree(target, concat(path, target), targets))
                    .toList()
            );
        }
        return context;
    }

    private static List<Target> concat(List<Target> path, Target node) {
        return Stream.concat(path.stream(), Stream.of(node)).toList();
    }

    private static List<Target> sortByTarget(List<Target> targets) {
        return targets.stream().sorted(comparing(Target::path)).toList();
    }

    private static List<Target> removeDuplicates(List<Target> targets) {
        return targets.stream()
            .collect(Collectors.groupingBy(Target::path))
            .entrySet().stream()
            .map(ReportTree::pickTargetInstance)
            .map(it -> it.mapPath(ROOT_PATH::resolve))
            .toList();
    }

    private static List<Target> pickNearestRoot(List<Target> targets) {
        long resourceCount = countResources(targets);
        Map<Boolean, List<Target>> rootCandidates = partitionByRootCandidacy(targets, resourceCount);
        Target root = findNearestRoot(rootCandidates.get(true));

        return Stream.concat(Stream.of(root), rootCandidates.get(false).stream())
            .map(target -> target.mapPath(root.path::relativize))
            .toList();
    }

    private static long countResources(List<Target> targets) {
        return targets.stream().filter(Target::hasResource).count();
    }

    private static Map<Boolean, List<Target>> partitionByRootCandidacy(List<Target> targets, long resourceCount) {
        return targets.stream()
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet().stream()
            .collect(Collectors.partitioningBy(
                e -> e.getValue() == resourceCount,
                Collectors.mapping(Map.Entry::getKey, Collectors.toList())
            ));
    }

    private static Target findNearestRoot(List<Target> commonRoots) {
        return commonRoots.stream()
            .max(comparing(target -> target.path().getNameCount()))
            .orElseGet(() -> Target.withPath("").withName(null));
    }

    private static List<Target> generateAllTargets(List<Path> files) {
        return files.stream()
            .map(ReportTree::directoryPerTestClassPackageComponents)
            .flatMap(ReportTree::getAllAncestors)
            .toList();
    }

    private static Target pickTargetInstance(Map.Entry<Path, List<Target>> entry) {
        List<Target> candidates = entry.getValue();
        if (candidates.size() == 1) return candidates.getFirst();
        return candidates.stream()
            .filter(Target::hasResource)
            .findFirst()
            .orElse(Target.withPath(entry.getKey()));
    }

    private static Target directoryPerTestClassPackageComponents(Path file) {
        // Top level directories are fully qualified name of Test Class
        String className = file.getName(0).toString();

        // Split the qualified class name into directory per package component
        Path classNamePath = prefix(file).resolve(className.replace('.', File.separatorChar));

        // Replace fully qualified class name directory with directory per package
        Path remainingPath = (file.getRoot() == null ? ROOT_PATH : file.getRoot())
            .resolve(className).relativize(file);
        return Target.withPath(classNamePath.resolve(remainingPath)).withResource(file);
    }

    private static Path prefix(Path file) {
        return file.getRoot() == null ? ROOT_PATH : file.getRoot();
    }

    private static Stream<Target> getAllAncestors(Target target) {
        return Stream.iterate(
            target.mapPath(Path::getParent),
            it -> Objects.nonNull(it.path()),
            it -> Target.withPath(it.path().getParent())
        );
    }

    /**
     * Represents a target in the report tree. A Target can be in one of several states:
     * 1. Path-only: Has path, but no resource or root flag (intermediate directory node)
     * 2. With resource: Has path and resource - name derived from resource file (leaf node)
     * 3. Named: Has explicit name set via withName() - overrides derived name
     */
    public record Target(String name, Path path, Path resource) {


        public static Target withPath(String target) {
            return withPath(Path.of(target));
        }

        /**
         * Creates a Target with path. Name is derived from path's filename.
         */
        public static Target withPath(Path target) {
            String name = target != null && target.getFileName() != null ? target.getFileName().toString() : null;
            return new Target(name, target, null);
        }

        public Target withResource(String resource) {
            return withResource(Path.of(resource));
        }

        /**
         * Adds a resource to this Target. Name is replaced with resource filename (without .yaml extension).
         */
        public Target withResource(Path resource) {
            return new Target(resource.getFileName().toString().replaceAll(YAML_EXTENSION + "$", ""), path, resource);
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
            return isRoot() ? "." : name();
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
