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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableTestReporter {

    private final ContextLoader contextLoader = new ContextLoader();
    private final TemplateEngine templateEngine;

    public TableTestReporter() {
        this.templateEngine = new TemplateEngine();
    }

    public TableTestReporter(Path customTemplateDirectory) {
        this.templateEngine = new TemplateEngine(customTemplateDirectory);
    }

    public ReportResult report(Format format, Path inDir, Path outDir) {
        ReportNode tree = ReportTree.process(inDir);
        if (tree == null) {
            return ReportResult.empty(inDir);
        }
        int count = report(tree, format, inDir, outDir);
        return ReportResult.success(count);
    }

    private int report(ReportNode node, Format format, Path inDir, Path outDir) {
        Path relativeOutPath = Path.of("./" + node.outPath());

        return switch (node) {
            case IndexNode index -> {
                Map<String, Object> context = createIndexContext(index, inDir, relativeOutPath);

                Path outPath = outDir.resolve(relativeOutPath).resolve("index" + format.extension());
                String content = templateEngine.renderIndex(format, context);
                writeContent(outPath, content);

                int childCount = index.contents().stream()
                        .mapToInt(child -> report(child, format, inDir, outDir))
                        .sum();
                yield 1 + childCount;
            }
            case TableNode table -> {
                Map<String, Object> context = createTableContext(table, inDir);

                Path outPath = outDir.resolve(relativeOutPath + format.extension());
                String content = templateEngine.renderTable(format, context);
                writeContent(outPath, content);
                yield 1;
            }
        };
    }

    private Map<String, Object> createIndexContext(IndexNode index, Path inDir, Path relativeOutPath) {
        Map<String, Object> context = loadContext(inDir, index.resource());
        context.put("name", index.name());
        context.put("contents", buildContentsForTemplate(index.contents(), inDir, relativeOutPath));
        return context;
    }

    private Map<String, Object> createTableContext(TableNode table, Path inDir) {
        Map<String, Object> context = loadContext(inDir, table.resource());
        context.put("name", table.name());
        return context;
    }

    private List<Map<String, Object>> buildContentsForTemplate(
            List<ReportNode> contents, Path inDir, Path relativeOutPath) {
        return contents.stream()
                .map(child -> {
                    Map<String, Object> contentMap = new HashMap<>();
                    contentMap.put("name", child.name());
                    contentMap.put("path", relativeOutPath.relativize(Path.of("./" + child.outPath())));
                    contentMap.put("type", child.type());

                    if (child.resource() != null) {
                        Map<String, Object> childContext = loadContext(inDir, child.resource());
                        Object title = childContext.get("title");
                        if (title != null) {
                            contentMap.put("title", title);
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

    private Map<String, Object> loadContext(Path inDir, Object resourcePath) {
        return new HashMap<>(
                resourcePath != null
                        ? contextLoader.fromYaml(inDir.resolve((String) resourcePath))
                        : Collections.emptyMap());
    }
}
