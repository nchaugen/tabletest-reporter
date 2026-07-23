package org.tabletest.maven;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AggregateMojoTest {

    @TempDir
    Path reactorDir;

    @Test
    void aggregates_every_module_of_the_reactor_into_one_report() throws Exception {
        MavenProject core = moduleWithOutput("core", "org.example.core.ParserTest", "parser-test");
        MavenProject junit = moduleWithOutput("junit", "org.example.junit.SlugifyTest", "slugify-test");
        Path outDir = reactorDir.resolve("out");

        aggregate(outDir, module("root"), core, junit).execute();

        assertThat(outDir.resolve("core/parser-test/rule.md")).exists();
        assertThat(outDir.resolve("junit/slugify-test/rule.md")).exists();
    }

    @Test
    void a_module_without_test_output_contributes_nothing() throws Exception {
        MavenProject core = moduleWithOutput("core", "org.example.core.ParserTest", "parser-test");
        MavenProject untested = module("untested");
        Path outDir = reactorDir.resolve("out-partial");

        aggregate(outDir, module("root"), core, untested).execute();

        assertThat(outDir.resolve("parser-test/rule.md")).exists();
    }

    @Test
    void fails_when_no_module_has_test_output() {
        Path outDir = reactorDir.resolve("out-none");

        assertThatThrownBy(
                        () -> aggregate(outDir, module("root"), module("core")).execute())
                .isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("No module in the reactor has TableTest output");
    }

    // --- helpers ---

    private AggregateMojo aggregate(Path outDir, MavenProject... reactorProjects) {
        AggregateMojo mojo = new AggregateMojo();
        setField(mojo, "format", "markdown");
        setField(mojo, "outputDirectory", outDir.toFile());
        setField(mojo, "baseDirectory", reactorDir.toFile());
        setField(mojo, "reactorProjects", List.of(reactorProjects));
        return mojo;
    }

    /** A reactor module with its own base and build directory, but no test output. */
    private MavenProject module(String name) {
        MavenProject project = new MavenProject();
        Path moduleDir = reactorDir.resolve(name);
        project.setFile(moduleDir.resolve("pom.xml").toFile());
        Build build = new Build();
        build.setDirectory(moduleDir.resolve("target").toString());
        project.setBuild(build);
        return project;
    }

    /** A reactor module whose tests wrote one class with one table to target/junit-jupiter. */
    private MavenProject moduleWithOutput(String name, String className, String slug) throws IOException {
        MavenProject project = module(name);
        Path classDir = reactorDir.resolve(name).resolve("target/junit-jupiter").resolve(className);
        Files.createDirectories(classDir);
        Files.writeString(classDir.resolve("TABLETEST-" + slug + ".yaml"), """
            "className": "%s"
            "slug": "%s"
            "title": "%s"
            "tableTests":
              - "path": "TABLETEST-rule.yaml"
                "methodName": "rule"
                "slug": "rule"
            """.formatted(className, slug, slug));
        Files.writeString(classDir.resolve("TABLETEST-rule.yaml"), """
            "title": "Rule"
            "headers":
            - "value": "Column A"
            "rows": []
            """);
        return project;
    }

    private static void setField(Object target, String fieldName, Object value) {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field f = type.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                // declared further up the hierarchy
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalArgumentException("No mojo parameter named " + fieldName);
    }
}
