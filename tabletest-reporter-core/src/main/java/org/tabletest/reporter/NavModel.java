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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Builds the sidebar navigation model (home link + recursive tree with status dots) that the
 * {@code renderSidebar} macro consumes. The link target is supplied as a resolver so the same
 * model serves both output modes: multi-file mode resolves each node to its page-relative file,
 * single-file mode to an in-page {@code #anchor}.
 */
final class NavModel {

    private NavModel() {}

    static Map<String, Object> build(ReportNode root, ReportNode current, Function<ReportNode, String> hrefOf) {
        Map<String, Object> home = new HashMap<>();
        home.put("label", label(root));
        home.put("href", hrefOf.apply(root));
        home.put("current", root == current);

        Map<String, Object> nav = new HashMap<>();
        nav.put("home", home);
        nav.put("tree", tree(root, current, hrefOf));
        return nav;
    }

    static String label(ReportNode node) {
        Object title = node.resource() != null ? node.resource().get("title") : null;
        if (title != null) {
            return title.toString();
        }
        return node.name() != null ? node.name() : "Home";
    }

    private static List<Map<String, Object>> tree(
            ReportNode node, ReportNode current, Function<ReportNode, String> hrefOf) {
        if (!(node instanceof IndexNode index)) {
            return List.of();
        }
        return index.contents().stream()
                .map(child -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("label", label(child));
                    item.put("href", hrefOf.apply(child));
                    item.put("type", child.type());
                    item.put("status", StatusRollup.of(child).state());
                    item.put("current", child == current);
                    List<Map<String, Object>> children = tree(child, current, hrefOf);
                    if (!children.isEmpty()) {
                        item.put("contents", children);
                    }
                    return item;
                })
                .toList();
    }
}
