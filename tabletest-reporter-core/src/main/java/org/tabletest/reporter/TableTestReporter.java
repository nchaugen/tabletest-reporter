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
package org.tabletest.reporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableTestReporter {

    private final TemplateEngine templateEngine;
    private final IndexDepth indexDepth;

    public TableTestReporter() {
        this(null, IndexDepth.DEFAULT);
    }

    public TableTestReporter(Path customTemplateDirectory) {
        this(customTemplateDirectory, IndexDepth.DEFAULT);
    }

    public TableTestReporter(Path customTemplateDirectory, IndexDepth indexDepth) {
        this.templateEngine =
                customTemplateDirectory != null ? new TemplateEngine(customTemplateDirectory) : new TemplateEngine();
        this.indexDepth = indexDepth;
    }

    public ReportResult report(Format format, Path inDir, Path outDir) {
        return report(format, inDir, outDir, false);
    }

    /**
     * Generates the report. In single-file mode the whole tree is assembled into one
     * self-contained document (currently HTML only); otherwise one file is written per node.
     */
    public ReportResult report(Format format, Path inDir, Path outDir, boolean singleFile) {
        ReportNode tree = ReportTree.process(inDir);
        if (tree == null) {
            return ReportResult.empty(inDir);
        }
        if (singleFile) {
            return reportSingleFile(format, tree, outDir);
        }
        int count = report(tree, tree, List.of(), format, outDir);
        if (format == BuiltInFormat.HTML) {
            writeContent(
                    outDir.resolve(SearchIndex.ASSET_NAME), SearchIndex.of(tree).asJavaScript());
        }
        return ReportResult.success(count);
    }

    private ReportResult reportSingleFile(Format format, ReportNode tree, Path outDir) {
        if (format != BuiltInFormat.HTML) {
            throw new IllegalArgumentException(
                    "Single-file mode is currently supported only for the html format, not " + format.formatName());
        }
        String content = templateEngine.renderSingle(SingleFileModel.of(tree));
        writeContent(outDir.resolve("index" + format.extension()), content);
        return ReportResult.success(1);
    }

    private int report(ReportNode node, ReportNode root, List<ReportNode> ancestors, Format format, Path outDir) {
        Path relativeOutPath = Path.of("./" + node.outPath());

        return switch (node) {
            case IndexNode index -> {
                Map<String, Object> context = createIndexContext(index, relativeOutPath, root, ancestors);

                Path outPath = outDir.resolve(relativeOutPath).resolve("index" + format.extension());
                String content = templateEngine.renderIndex(format, context);
                writeContent(outPath, content);

                List<ReportNode> childAncestors = append(ancestors, index);
                int childCount = index.contents().stream()
                        .mapToInt(child -> report(child, root, childAncestors, format, outDir))
                        .sum();
                yield 1 + childCount;
            }
            case TableNode table -> {
                Map<String, Object> context = createTableContext(table, root, ancestors);

                Path outPath = outDir.resolve(relativeOutPath + format.extension());
                String content = templateEngine.renderTable(format, context);
                writeContent(outPath, content);
                yield 1;
            }
        };
    }

    private Map<String, Object> createIndexContext(
            IndexNode index, Path relativeOutPath, ReportNode root, List<ReportNode> ancestors) {
        Map<String, Object> context = copyContext(index.resource());
        context.put("name", index.name());
        context.put("contents", buildContentsForTemplate(index.contents(), relativeOutPath, 1));
        context.put("status", StatusRollup.of(index).toMap());
        context.put("breadcrumbs", buildBreadcrumbs(ancestors, index));
        context.put("nav", buildNav(root, index));
        context.put("assetRoot", NavLinks.rootPrefix(index, root));
        return context;
    }

    private Map<String, Object> createTableContext(TableNode table, ReportNode root, List<ReportNode> ancestors) {
        Map<String, Object> context = copyContext(table.resource());
        context.put("name", table.name());
        context.put("breadcrumbs", buildBreadcrumbs(ancestors, table));
        context.put("nav", buildNav(root, table));
        context.put("assetRoot", NavLinks.rootPrefix(table, root));
        return context;
    }

    private List<Map<String, Object>> buildBreadcrumbs(List<ReportNode> ancestors, ReportNode current) {
        Path fromDirectory = NavLinks.pageDirectory(current);
        List<ReportNode> trail = append(ancestors, current);
        return trail.stream()
                .map(node -> {
                    boolean isCurrent = node == current;
                    Map<String, Object> crumb = new HashMap<>();
                    crumb.put("label", NavModel.label(node));
                    crumb.put("current", isCurrent);
                    if (!isCurrent) {
                        crumb.put("href", NavLinks.href(fromDirectory, node));
                    }
                    return crumb;
                })
                .toList();
    }

    private Map<String, Object> buildNav(ReportNode root, ReportNode current) {
        Path fromDirectory = NavLinks.pageDirectory(current);
        return NavModel.build(root, current, target -> NavLinks.href(fromDirectory, target));
    }

    private static List<ReportNode> append(List<ReportNode> nodes, ReportNode node) {
        List<ReportNode> result = new ArrayList<>(nodes);
        result.add(node);
        return List.copyOf(result);
    }

    private List<Map<String, Object>> buildContentsForTemplate(
            List<ReportNode> contents, Path relativeOutPath, int currentDepth) {
        return contents.stream()
                .map(child -> {
                    Map<String, Object> contentMap = new HashMap<>();
                    contentMap.put("name", child.name());
                    contentMap.put("path", relativeOutPath.relativize(Path.of("./" + child.outPath())));
                    contentMap.put("type", child.type());
                    contentMap.put("status", StatusRollup.of(child).state());

                    if (child.resource() != null) {
                        Object title = child.resource().get("title");
                        if (title != null) {
                            contentMap.put("title", title);
                        }
                    }

                    if (child instanceof IndexNode indexChild && currentDepth < indexDepth.value()) {
                        List<Map<String, Object>> nested =
                                buildContentsForTemplate(indexChild.contents(), relativeOutPath, currentDepth + 1);
                        if (!nested.isEmpty()) {
                            contentMap.put("contents", nested);
                        }
                    }

                    return contentMap;
                })
                .toList();
    }

    private static void writeContent(Path outPath, String content) {
        try {
            Files.createDirectories(outPath.getParent());
            Files.writeString(outPath, content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write output file " + outPath, e);
        }
    }

    private Map<String, Object> copyContext(Map<String, Object> resource) {
        return new HashMap<>(resource != null ? resource : Collections.emptyMap());
    }
}
