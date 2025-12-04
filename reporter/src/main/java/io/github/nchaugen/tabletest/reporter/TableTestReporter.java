package io.github.nchaugen.tabletest.reporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class TableTestReporter {

    private final Context context = new Context();

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

    private OutPathAndContext readContext(Path outPath, Path inPath) {
        return new OutPathAndContext(outPath, context.fromYaml(inPath));
    }

    private OutPathAndContent renderContent(ReportFormat format, Path outPath, Map<String, Object> context) {
        return new OutPathAndContent(outPath, format.renderTable(context));
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
