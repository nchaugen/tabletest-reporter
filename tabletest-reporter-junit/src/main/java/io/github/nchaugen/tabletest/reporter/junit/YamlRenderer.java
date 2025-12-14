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
package io.github.nchaugen.tabletest.reporter.junit;

import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;

/**
 * Renders table tests and test indices to YAML format.
 */
class YamlRenderer {

    private static final DumpSettings SETTINGS = DumpSettings.builder()
        .setDefaultFlowStyle(FlowStyle.BLOCK)
        .setIndent(2)
        .setDefaultScalarStyle(ScalarStyle.DOUBLE_QUOTED)
        .setSplitLines(false)
        .setDereferenceAliases(true)
        .setMultiLineFlow(false)
        .setUseUnicodeEncoding(true)
        .build();

    private final Dump yaml = new Dump(SETTINGS);

    /**
     * Renders table test data to YAML.
     */
    String render(TableTestData data) {
        return yaml.dumpToString(data.toMap());
    }

    /**
     * Renders test class data to YAML.
     */
    String render(TestClassData data) {
        return yaml.dumpToString(data.toMap());
    }

}
