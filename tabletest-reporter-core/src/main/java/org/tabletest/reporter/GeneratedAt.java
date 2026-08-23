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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;

/**
 * The moment a report run produced the documentation, in the two forms the footer needs: the
 * machine-readable value of a {@code <time datetime>} attribute and the label a reader sees.
 * Timestamps are stated in UTC so a report generated in CI reads the same everywhere.
 */
record GeneratedAt(Instant instant) {

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm 'UTC'", Locale.ENGLISH)
            .withZone(ZoneOffset.UTC);

    static GeneratedAt now() {
        return new GeneratedAt(Instant.now());
    }

    /**
     * The instant the build pinned the report to, or the clock when it pinned none. A build that
     * wants the same bytes from the same tests pins one; every other build reads the clock.
     */
    static GeneratedAt at(Instant pinned) {
        return pinned == null ? now() : new GeneratedAt(pinned);
    }

    Map<String, Object> toMap() {
        return Map.of("datetime", datetime(), "label", label());
    }

    String datetime() {
        return DateTimeFormatter.ISO_INSTANT.format(instant.truncatedTo(ChronoUnit.SECONDS));
    }

    private String label() {
        return LABEL.format(instant);
    }
}
