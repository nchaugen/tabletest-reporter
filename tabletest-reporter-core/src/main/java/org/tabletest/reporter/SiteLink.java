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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The link back to the site that hosts the published report. Every other link a report holds is
 * relative within its own tree, so a reader who arrives from a site has no way back; this is the one
 * link that leaves the report. Sourced from the {@code site:} section of a
 * {@code tabletest-reporter.yaml} sidecar file and rendered into the page footer.
 *
 * <p>A link needs a URL, so one declared without a {@code url} is {@link #NONE}. A URL declared
 * without a label labels itself with the URL. The URL reaches the page exactly as written — never
 * resolved against the report's own tree — so a site published at a path of its own is reachable by
 * a root-relative URL.
 *
 * @param label the link text, or null when there is no link
 * @param url the address to link to, or null when there is no link
 */
public record SiteLink(String label, String url) {

    /** The absent case: the report links nowhere, and the footer holds the attribution alone. */
    public static final SiteLink NONE = new SiteLink(null, null);

    public SiteLink {
        if (url == null || url.isBlank()) {
            label = null;
            url = null;
        } else if (label == null || label.isBlank()) {
            label = url;
        }
    }

    /**
     * Parses the link from a raw YAML map (as loaded by {@link ContextLoader}). A document with no
     * {@code site:} section, or one declaring no URL, yields {@link #NONE}.
     */
    public static SiteLink parse(Map<String, Object> yaml) {
        if (yaml == null || !(yaml.get("site") instanceof Map<?, ?> site)) {
            return NONE;
        }
        return new SiteLink(SpecMetadata.stringValue(site, "label"), SpecMetadata.stringValue(site, "url"));
    }

    /** True when a link was declared, so the footer has something to render. */
    public boolean isPresent() {
        return url != null;
    }

    /**
     * The template context entry for this link.
     *
     * @return the label and URL a template renders, or null when no link was declared
     */
    public Map<String, Object> toMap() {
        if (!isPresent()) {
            return null;
        }
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("label", label);
        context.put("url", url);
        return context;
    }
}
