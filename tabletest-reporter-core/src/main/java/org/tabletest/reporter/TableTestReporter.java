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
import java.util.stream.IntStream;

public class TableTestReporter {

    private final TemplateEngine templateEngine;
    private final ReportConfiguration configuration;

    public TableTestReporter() {
        this(null, IndexDepth.DEFAULT);
    }

    public TableTestReporter(Path customTemplateDirectory) {
        this(customTemplateDirectory, IndexDepth.DEFAULT);
    }

    public TableTestReporter(Path customTemplateDirectory, IndexDepth indexDepth) {
        this(new ReportConfiguration(
                BuiltInFormat.ASCIIDOC,
                customTemplateDirectory,
                indexDepth,
                false,
                SpecMetadata.EMPTY,
                PublishSelection.EMPTY,
                SiteLink.NONE,
                FrontMatter.NONE));
    }

    /** Reports against a resolved configuration — the form every entry point uses. */
    public TableTestReporter(ReportConfiguration configuration) {
        this.templateEngine = configuration.templateDirectory() != null
                ? new TemplateEngine(configuration.templateDirectory())
                : new TemplateEngine();
        this.configuration = configuration;
    }

    /**
     * Generates the report described by this reporter's configuration.
     *
     * @param inDir the directory of TableTest YAML output to read
     * @param outDir the directory to write the generated documentation to
     * @return the outcome, carrying the number of files generated
     */
    public ReportResult report(Path inDir, Path outDir) {
        return report(List.of(inDir), outDir);
    }

    /**
     * Generates one report from several directories of TableTest output, so the modules of a
     * multi-module build publish a single spec.
     *
     * @param inDirs the directories of TableTest YAML output to read, in declared order
     * @param outDir the directory to write the generated documentation to
     * @return the outcome, carrying the number of files generated
     */
    public ReportResult report(List<Path> inDirs, Path outDir) {
        return report(configuration, inDirs, outDir);
    }

    public ReportResult report(Format format, Path inDir, Path outDir) {
        return report(format, inDir, outDir, false);
    }

    public ReportResult report(Format format, Path inDir, Path outDir, boolean singleFile) {
        return report(format, inDir, outDir, singleFile, SpecMetadata.EMPTY);
    }

    public ReportResult report(Format format, Path inDir, Path outDir, boolean singleFile, SpecMetadata specMetadata) {
        return report(
                new ReportConfiguration(
                        format,
                        configuration.templateDirectory(),
                        configuration.indexDepth(),
                        singleFile,
                        specMetadata,
                        configuration.publishSelection(),
                        configuration.siteLink(),
                        configuration.frontMatter()),
                List.of(inDir),
                outDir);
    }

    /**
     * Generates the report. The publish selection decides which pages the report holds, and spec
     * metadata (title, intro, feature order/titles) curates those that remain, both applied on top
     * of the built tree before rendering. In single-file mode the whole tree is assembled into one
     * self-contained document (currently HTML only); otherwise one file is written per node.
     */
    private ReportResult report(ReportConfiguration config, List<Path> inDirs, Path outDir) {
        ReportNode built = ReportTree.process(inDirs);
        if (built == null) {
            return ReportResult.empty(inDirs);
        }
        Format format = config.format();
        ReportNode tree =
                config.specMetadata().applyTo(config.publishSelection().applyTo(built));
        GeneratedAt generatedAt = GeneratedAt.now();
        RenderRun run = new RenderRun(format, generatedAt, config.siteLink(), config.frontMatter(), outDir);
        if (config.singleFile()) {
            return reportSingleFile(tree, run);
        }
        int count = report(tree, tree, List.of(), null, run);
        if (format == BuiltInFormat.HTML) {
            writeContent(
                    outDir.resolve(SearchIndex.ASSET_NAME), SearchIndex.of(tree).asJavaScript());
        }
        return ReportResult.success(count);
    }

    private ReportResult reportSingleFile(ReportNode tree, RenderRun run) {
        if (run.format() != BuiltInFormat.HTML) {
            throw new IllegalArgumentException("Single-file mode is currently supported only for the html format, not "
                    + run.format().formatName());
        }
        String content = templateEngine.renderSingle(SingleFileModel.of(tree, run.generatedAt(), run.siteLink()));
        writeContent(run.outDir().resolve("index" + run.format().extension()), content);
        return ReportResult.success(1);
    }

    /**
     * What stays the same for every page of one report run: the format being written, the run
     * timestamp, the link back to the hosting site, and where the files go. Passing one value keeps
     * the recursive walk readable as the render options grow.
     */
    private record RenderRun(
            Format format, GeneratedAt generatedAt, SiteLink siteLink, FrontMatter frontMatter, Path outDir) {}

    private int report(ReportNode node, ReportNode root, List<ReportNode> ancestors, Integer weight, RenderRun run) {
        Path relativeOutPath = Path.of("./" + node.outPath());

        return switch (node) {
            case IndexNode index -> {
                Map<String, Object> context = createIndexContext(index, relativeOutPath, root, ancestors, weight, run);

                Path outPath = run.outDir()
                        .resolve(relativeOutPath)
                        .resolve("index" + run.format().extension());
                String content = templateEngine.renderIndex(run.format(), context);
                writeContent(outPath, content);

                List<ReportNode> childAncestors = append(ancestors, index);
                List<ReportNode> children = index.contents();
                int childCount = IntStream.range(0, children.size())
                        .map(position -> report(children.get(position), root, childAncestors, position + 1, run))
                        .sum();
                yield 1 + childCount;
            }
            case TableNode table -> {
                Map<String, Object> context = createTableContext(table, root, ancestors, weight, run);

                Path outPath =
                        run.outDir().resolve(relativeOutPath + run.format().extension());
                String content = templateEngine.renderTable(run.format(), context);
                writeContent(outPath, content);
                yield 1;
            }
        };
    }

    private Map<String, Object> createIndexContext(
            IndexNode index,
            Path relativeOutPath,
            ReportNode root,
            List<ReportNode> ancestors,
            Integer weight,
            RenderRun run) {
        Map<String, Object> context = copyContext(index.resource());
        context.put("name", index.name());
        context.put("contents", buildContentsForTemplate(index.contents(), relativeOutPath, 1));
        context.put("status", StatusRollup.of(index).toMap());
        context.put("breadcrumbs", buildBreadcrumbs(ancestors, index));
        context.put("nav", buildNav(root, index));
        context.put("assetRoot", NavLinks.rootPrefix(index, root));
        context.put("generatedAt", run.generatedAt().toMap());
        context.put("site", run.siteLink().toMap());
        context.put("frontMatter", frontMatterFor(index, weight, run));
        return context;
    }

    private Map<String, Object> createTableContext(
            TableNode table, ReportNode root, List<ReportNode> ancestors, Integer weight, RenderRun run) {
        Map<String, Object> context = copyContext(table.resource());
        context.put("name", table.name());
        context.put("breadcrumbs", buildBreadcrumbs(ancestors, table));
        context.put("nav", buildNav(root, table));
        context.put("assetRoot", NavLinks.rootPrefix(table, root));
        context.put("generatedAt", run.generatedAt().toMap());
        context.put("site", run.siteLink().toMap());
        context.put("featureDescription", descriptionOf(ancestors));
        context.put("frontMatter", frontMatterFor(table, weight, run));
        return context;
    }

    /**
     * The front-matter entries for one page. The page's own title and its position among its
     * siblings are what the reporter can fill that a site generator cannot work out for itself: the
     * position carries the declared reading order into a generator that would otherwise sort the
     * pages alphabetically.
     *
     * @return the entries a text template writes, or null when no front matter is declared
     */
    private static List<Map<String, Object>> frontMatterFor(ReportNode node, Integer weight, RenderRun run) {
        if (!run.frontMatter().isPresent()) {
            return null;
        }
        return run.frontMatter()
                .entriesFor(NavModel.label(node), weight, run.generatedAt().datetime());
    }

    /**
     * The description of the page a rule sits under. A rule page shows it above the rule's own
     * description, because the class or feature description is where the notation a rule's columns
     * use is explained, and a reader arriving from the sidebar or a search result never passes the
     * index page that would otherwise carry it.
     *
     * @return the nearest ancestor's description, or null when it has none
     */
    private static Object descriptionOf(List<ReportNode> ancestors) {
        return ancestors.isEmpty()
                ? null
                : ancestors.get(ancestors.size() - 1).resource().get("description");
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
                    contentMap.put("path", contentHref(relativeOutPath, child));
                    contentMap.put("type", child.type());
                    contentMap.put("status", StatusRollup.of(child).state());

                    if (child.resource() != null) {
                        Object title = child.resource().get("title");
                        if (title != null) {
                            contentMap.put("title", title);
                        }
                    }

                    if (child instanceof IndexNode indexChild
                            && currentDepth < configuration.indexDepth().value()) {
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

    /** Relative link target from an index page to a child entry, with '/' separators on every platform. */
    private static String contentHref(Path fromDirectory, ReportNode child) {
        return fromDirectory
                .relativize(Path.of("./" + child.outPath()))
                .toString()
                .replace('\\', '/');
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
