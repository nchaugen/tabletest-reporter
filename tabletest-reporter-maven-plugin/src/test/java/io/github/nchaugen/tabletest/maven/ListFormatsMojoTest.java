package io.github.nchaugen.tabletest.maven;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListFormatsMojoTest {

    @TempDir
    Path tempDir;

    @Test
    void execute_logs_formats() {
        ListFormatsMojo mojo = new ListFormatsMojo();
        List<String> logMessages = captureLogMessages(mojo);

        mojo.execute();

        assertThat(logMessages).anyMatch(msg -> msg.contains("asciidoc"));
    }

    @Test
    void execute_includes_custom_formats_when_template_directory_provided() throws Exception {
        Path templateDir = setupCustomTemplateDirectory(tempDir);

        ListFormatsMojo mojo = new ListFormatsMojo();
        setField(mojo, "templateDirectory", templateDir.toFile());
        List<String> logMessages = captureLogMessages(mojo);

        mojo.execute();

        assertThat(logMessages).anyMatch(msg -> msg.contains("html"));
    }

    private Path setupCustomTemplateDirectory(Path parent) throws IOException {
        Path templateDir = parent.resolve("templates");
        Files.createDirectories(templateDir);
        Files.writeString(templateDir.resolve("table.html.peb"), "template");
        Files.writeString(templateDir.resolve("index.html.peb"), "template");
        Files.writeString(templateDir.resolve("table.xml.peb"), "template");
        Files.writeString(templateDir.resolve("index.xml.peb"), "template");
        return templateDir;
    }

    private List<String> captureLogMessages(ListFormatsMojo mojo) {
        List<String> messages = new ArrayList<>();
        Log mockLog = new Log() {
            @Override
            public boolean isDebugEnabled() {
                return false;
            }

            @Override
            public void debug(CharSequence content) {}

            @Override
            public void debug(CharSequence content, Throwable error) {}

            @Override
            public void debug(Throwable error) {}

            @Override
            public boolean isInfoEnabled() {
                return true;
            }

            @Override
            public void info(CharSequence content) {
                messages.add(content.toString());
            }

            @Override
            public void info(CharSequence content, Throwable error) {
                messages.add(content.toString());
            }

            @Override
            public void info(Throwable error) {}

            @Override
            public boolean isWarnEnabled() {
                return false;
            }

            @Override
            public void warn(CharSequence content) {}

            @Override
            public void warn(CharSequence content, Throwable error) {}

            @Override
            public void warn(Throwable error) {}

            @Override
            public boolean isErrorEnabled() {
                return false;
            }

            @Override
            public void error(CharSequence content) {}

            @Override
            public void error(CharSequence content, Throwable error) {}

            @Override
            public void error(Throwable error) {}
        };
        mojo.setLog(mockLog);
        return messages;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
