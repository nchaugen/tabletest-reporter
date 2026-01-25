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
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Comparator.comparingInt;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * Builds a ReportNode tree directly from YAML sources.
 * Finds the deepest common ancestor path and creates intermediate index nodes automatically.
 */
final class TreeBuilder {

    private TreeBuilder() {}

    static ReportNode buildTree(List<Source> sources) {
        if (sources.isEmpty()) {
            return null;
        }

        Map<Path, Map<String, Object>> yamlByPath = indexByPath(sources);

        List<NodeEntry> contentEntries = sources.stream()
                .map(TreeBuilder::parseClassInfo)
                .flatMap(Optional::stream)
                .flatMap(classInfo -> streamNodeEntries(classInfo, yamlByPath))
                .toList();

        if (contentEntries.isEmpty()) {
            return null;
        }

        Map<Path, NodeEntry> nodesByPath = buildNodeMap(contentEntries);
        Path commonRoot = findCommonRoot(contentEntries);

        return commonRoot != null
                ? buildNode(commonRoot, commonRoot, nodesByPath)
                : buildWithSyntheticRoot(nodesByPath);
    }

    private static Map<Path, Map<String, Object>> indexByPath(List<Source> sources) {
        return sources.stream()
                .collect(toMap(source -> source.path().normalize(), Source::yaml, (left, right) -> left));
    }

    private static Stream<NodeEntry> streamNodeEntries(ClassInfo classInfo, Map<Path, Map<String, Object>> yamlByPath) {
        Path classPath = TargetPathResolver.classPathFromClassName(classInfo.className);
        NodeEntry classEntry = new NodeEntry(classPath, classInfo.slug, classInfo.yaml);

        Stream<NodeEntry> tableEntries = classInfo.tableTests.stream()
                .map(entry -> toTableEntry(classPath, classInfo.sourcePath, entry, yamlByPath))
                .flatMap(Optional::stream);

        return Stream.concat(Stream.of(classEntry), tableEntries);
    }

    private static Optional<NodeEntry> toTableEntry(
            Path classPath, Path sourcePath, TableTestRef ref, Map<Path, Map<String, Object>> yamlByPath) {

        if (ref.slug == null) {
            return Optional.empty();
        }

        Path tableResource = TargetPathResolver.resolveTableResource(sourcePath, ref.path);
        if (tableResource == null) {
            return Optional.empty();
        }

        Map<String, Object> tableYaml = yamlByPath.get(tableResource.normalize());
        if (tableYaml == null) {
            return Optional.empty();
        }

        return Optional.of(new NodeEntry(classPath.resolve(ref.slug), ref.slug, tableYaml));
    }

    private static Map<Path, NodeEntry> buildNodeMap(List<NodeEntry> contentEntries) {
        Map<Path, NodeEntry> nodesByPath = contentEntries.stream()
                .collect(toMap(NodeEntry::path, identity(), (a, b) -> a.resource != null ? a : b));

        Set<Path> ancestorPaths = contentEntries.stream()
                .flatMap(entry -> streamAncestors(entry.path))
                .collect(java.util.stream.Collectors.toSet());

        ancestorPaths.stream()
                .filter(path -> !nodesByPath.containsKey(path))
                .forEach(path -> nodesByPath.put(path, new NodeEntry(path, pathName(path), null)));

        return nodesByPath;
    }

    private static Stream<Path> streamAncestors(Path path) {
        return Stream.iterate(path.getParent(), p -> p != null && p.getNameCount() > 0, Path::getParent);
    }

    private static String pathName(Path path) {
        return path.getFileName() != null ? path.getFileName().toString() : null;
    }

    private static Path findCommonRoot(List<NodeEntry> contentEntries) {
        return contentEntries.stream()
                .map(entry -> streamAncestors(entry.path).collect(java.util.stream.Collectors.toSet()))
                .reduce((a, b) -> {
                    a.retainAll(b);
                    return a;
                })
                .flatMap(ancestors -> ancestors.stream().max(comparingInt(Path::getNameCount)))
                .orElse(null);
    }

    private static ReportNode buildWithSyntheticRoot(Map<Path, NodeEntry> nodesByPath) {
        Set<Path> topLevelPaths =
                nodesByPath.keySet().stream().map(path -> path.getName(0)).collect(java.util.stream.Collectors.toSet());

        List<ReportNode> children = topLevelPaths.stream()
                .sorted()
                .map(path -> buildNode(path, null, nodesByPath))
                .sorted(Comparator.comparing(ReportNode::name, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();

        return new IndexNode(null, "", null, children);
    }

    private static ReportNode buildNode(Path nodePath, Path commonRoot, Map<Path, NodeEntry> nodesByPath) {
        NodeEntry entry = nodesByPath.get(nodePath);
        String name = entry != null ? entry.name : null;
        Map<String, Object> resource = entry != null ? entry.resource : null;
        String outPath = buildOutPath(nodePath, commonRoot, nodesByPath);

        List<Path> childPaths = nodesByPath.keySet().stream()
                .filter(p -> isDirectChildOf(nodePath, p))
                .sorted()
                .toList();

        if (childPaths.isEmpty()) {
            return new TableNode(name, outPath, resource);
        }

        List<ReportNode> children = childPaths.stream()
                .map(path -> buildNode(path, commonRoot, nodesByPath))
                .sorted(Comparator.comparing(ReportNode::name, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();

        return new IndexNode(name, outPath, resource, children);
    }

    private static boolean isDirectChildOf(Path parent, Path candidate) {
        Path candidateParent = candidate.getParent();
        return candidateParent != null && candidateParent.equals(parent);
    }

    private static String buildOutPath(Path nodePath, Path commonRoot, Map<Path, NodeEntry> nodesByPath) {
        if (commonRoot == null) {
            return buildOutPathSegments(nodePath, nodePath, nodesByPath);
        }
        if (nodePath.equals(commonRoot)) {
            return "";
        }
        Path relativePath = commonRoot.relativize(nodePath);
        return buildOutPathSegments(relativePath, commonRoot, nodesByPath);
    }

    private static String buildOutPathSegments(Path path, Path basePath, Map<Path, NodeEntry> nodesByPath) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.getNameCount(); i++) {
            Path fullPath = basePath.equals(path) ? path.subpath(0, i + 1) : basePath.resolve(path.subpath(0, i + 1));
            NodeEntry entry = nodesByPath.get(fullPath);
            String segment = entry != null && entry.name != null
                    ? entry.name.toLowerCase()
                    : path.getName(i).toString().toLowerCase();
            sb.append(File.separator).append(segment);
        }
        return sb.toString();
    }

    // --- YAML Parsing ---

    private static Optional<ClassInfo> parseClassInfo(Source source) {
        Map<String, Object> yaml = source.yaml();
        if (yaml.isEmpty()) {
            return Optional.empty();
        }

        String className = stringValue(yaml, "className");
        String slug = stringValue(yaml, "slug");
        Object tableTestsValue = yaml.get("tableTests");

        if (className == null || slug == null || !(tableTestsValue instanceof List<?>)) {
            return Optional.empty();
        }

        List<TableTestRef> tableTests = parseTableTestRefs(tableTestsValue);
        return Optional.of(new ClassInfo(source.path(), yaml, className, slug, tableTests));
    }

    private static List<TableTestRef> parseTableTestRefs(Object tableTestsValue) {
        if (!(tableTestsValue instanceof List<?> entries)) {
            return List.of();
        }
        return entries.stream()
                .filter(Map.class::isInstance)
                .map(entry -> parseTableTestRef((Map<?, ?>) entry))
                .flatMap(Optional::stream)
                .toList();
    }

    private static Optional<TableTestRef> parseTableTestRef(Map<?, ?> map) {
        String path = stringValue(map, "path");
        if (path == null) {
            return Optional.empty();
        }
        return Optional.of(new TableTestRef(path, stringValue(map, "methodName"), stringValue(map, "slug")));
    }

    private static String stringValue(Map<?, ?> map, String key) {
        return map.get(key) instanceof String s && !s.isBlank() ? s : null;
    }

    // --- Data Records ---

    private record ClassInfo(
            Path sourcePath, Map<String, Object> yaml, String className, String slug, List<TableTestRef> tableTests) {}

    private record TableTestRef(String path, String methodName, String slug) {}

    private record NodeEntry(Path path, String name, Map<String, Object> resource) {}
}
