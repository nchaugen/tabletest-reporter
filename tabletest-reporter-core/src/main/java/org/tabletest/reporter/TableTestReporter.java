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
        ReportNode tree = ReportTree.process(inDir);
        if (tree == null) {
            return ReportResult.empty(inDir);
        }
        int count = report(tree, format, outDir);
        return ReportResult.success(count);
    }

    private int report(ReportNode node, Format format, Path outDir) {
        Path relativeOutPath = Path.of("./" + node.outPath());

        return switch (node) {
            case IndexNode index -> {
                Map<String, Object> context = createIndexContext(index, relativeOutPath);

                Path outPath = outDir.resolve(relativeOutPath).resolve("index" + format.extension());
                String content = templateEngine.renderIndex(format, context);
                writeContent(outPath, content);

                int childCount = index.contents().stream()
                        .mapToInt(child -> report(child, format, outDir))
                        .sum();
                yield 1 + childCount;
            }
            case TableNode table -> {
                Map<String, Object> context = createTableContext(table);

                Path outPath = outDir.resolve(relativeOutPath + format.extension());
                String content = templateEngine.renderTable(format, context);
                writeContent(outPath, content);
                yield 1;
            }
        };
    }

    private Map<String, Object> createIndexContext(IndexNode index, Path relativeOutPath) {
        Map<String, Object> context = copyContext(index.resource());
        context.put("name", index.name());
        context.put("contents", buildContentsForTemplate(index.contents(), relativeOutPath, 1));
        return context;
    }

    private Map<String, Object> createTableContext(TableNode table) {
        Map<String, Object> context = copyContext(table.resource());
        context.put("name", table.name());
        return context;
    }

    private List<Map<String, Object>> buildContentsForTemplate(
            List<ReportNode> contents, Path relativeOutPath, int currentDepth) {
        return contents.stream()
                .map(child -> {
                    Map<String, Object> contentMap = new HashMap<>();
                    contentMap.put("name", child.name());
                    contentMap.put("path", relativeOutPath.relativize(Path.of("./" + child.outPath())));
                    contentMap.put("type", child.type());

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
