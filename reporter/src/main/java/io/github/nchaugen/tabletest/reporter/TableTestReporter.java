package io.github.nchaugen.tabletest.reporter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class TableTestReporter {

    public void report(ReportFormat format, Path inDir, Path outDir) throws IOException {
        try (var files = Files.list(inDir)) {
            files
                .filter(it -> it.toString().endsWith(".yaml"))
                .map(inPath -> createOutPath(format, outDir, inPath))
                .map(it -> readContext(it.outPath(), it.inPath()))
                .map(it -> renderContent(format, it.outPath(), it.context()))
                .forEach(it -> writeFile(it.outPath(), it.content()));
        }
    }

    private static OutPathAndInPath createOutPath(ReportFormat format, Path outDir, Path inPath) {
        String fileName = inPath.getFileName().toString().replaceAll(".yaml$", format.extension());
        return new OutPathAndInPath(outDir.resolve(fileName), inPath);
    }

    private static final ObjectMapper MAPPER = new YAMLMapper();
    private static final TypeReference<Map<String, Object>> TYPE_REFERENCE = new TypeReference<>() {
    };

    private OutPathAndContext readContext(Path outPath, Path inPath) {
        try {
            Map<String, Object> context = MAPPER.readValue(inPath.toFile(), TYPE_REFERENCE);
            return new OutPathAndContext(outPath, context);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read YAML file " + inPath, e);
        }
    }

    private OutPathAndContent renderContent(ReportFormat format, Path outPath, Map<String, Object> context) {
        try {
            Writer writer = new StringWriter();
            format.tableTemplate().evaluate(writer, context);
            return new OutPathAndContent(outPath, writer.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render table in " + format + " with context: " + context.get("title"), e);
        }
    }

    private void writeFile(Path outFile, String content) {
        try {
            Files.writeString(outFile, content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write output file " + outFile, e);
        }
    }

    private record OutPathAndInPath(Path outPath, Path inPath) {}
    private record OutPathAndContext(Path outPath, Map<String, Object> context) {}
    private record OutPathAndContent(Path outPath, String content) {}

}
