package io.github.nchaugen.tabletest.reporter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

public class Context {
    private static final ObjectMapper YAML_MAPPER = new YAMLMapper();
    private static final TypeReference<Map<String, Object>> TYPE_REFERENCE = new TypeReference<>() {
    };

    static Map<String, Object> fromYaml(File file) {
        try {
            return YAML_MAPPER.readValue(file, TYPE_REFERENCE);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read YAML from " + file, e);
        }
    }

    static Map<String, Object> fromYaml(String value) {
        try {
            return YAML_MAPPER.readValue(value, TYPE_REFERENCE);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read YAML from " + value, e);
        }
    }
}
