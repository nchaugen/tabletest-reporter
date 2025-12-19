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

import static java.util.Collections.emptyList;

public class TableTestReporter {

    private final ContextLoader contextLoader = new ContextLoader();
    private final TemplateEngine templateEngine = new TemplateEngine();

    public void report(ReportFormat format, Path inDir, Path outDir) {
        report(ReportTree.process(inDir), format, inDir, outDir);
    }

    private void report(Map<String, Object> tree, ReportFormat format, Path inDir, Path outDir) {
        Path relativeOutPath = Path.of("./" + tree.get("outPath"));
        List<Map<String, Object>> contents = ((List<Map<String, Object>>) tree.getOrDefault("contents", emptyList()));

        Map<String, Object> context = loadContext(inDir, tree.get("resource"));
        context.put("name", tree.get("name"));
        context.put("contents", contents.stream()
            .map(content -> {
                    Map<String, Object> contentMap = new HashMap<>();
                    contentMap.put("name", content.get("name"));
                    contentMap.put("path", relativeOutPath.relativize(Path.of("./" + content.get("outPath"))));
                    contentMap.put("type", content.get("type"));

                    Object resource = content.get("resource");
                    if (resource != null) {
                        Map<String, Object> childContext = loadContext(inDir, resource);
                        Object title = childContext.get("title");
                        if (title != null) {
                            contentMap.put("title", title);
                        }
                    }

                    return contentMap;
                }
            ).toList());

        boolean isIndex = "index".equals(tree.get("type"));

        Path outPath = isIndex
            ? outDir.resolve(relativeOutPath).resolve("index" + format.extension())
            : outDir.resolve(relativeOutPath + format.extension());

        String content = isIndex
            ? templateEngine.renderIndex(format, context)
            : templateEngine.renderTable(format, context);

        writeContent(outPath, content);

        contents.forEach(child -> report(child, format, inDir, outDir));
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
                : Collections.emptyMap()
        );
    }

}
