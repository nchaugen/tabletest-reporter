package org.tabletest.reporter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

// Unpublished: end-to-end wiring of the site link from sidecar file to page, not a user-facing rule.
class SiteLinkReportTest {

    @TempDir
    Path tempDir;

    @Test
    void everyGeneratedPageLinksBackToTheDeclaredSite() throws IOException {
        Path outDir = reportWith("""
                site:
                  label: "TableTest"
                  url: "https://tabletest.org/"
                """);

        assertThat(Files.readString(outDir.resolve("index.html")))
                .contains("<a class=\"site-link\" href=\"https://tabletest.org/\">&uarr; TableTest</a>");
        assertThat(Files.readString(outDir.resolve("auth-test/login-validation.html")))
                .contains("<a class=\"site-link\" href=\"https://tabletest.org/\">&uarr; TableTest</a>");
    }

    @Test
    void withoutASiteSectionNoPageLinksOut() throws IOException {
        Path outDir = reportWith("""
                title: "Example Spec"
                """);

        assertThat(Files.readString(outDir.resolve("index.html"))).doesNotContain("<a class=\"site-link\"");
        assertThat(Files.readString(outDir.resolve("auth-test/login-validation.html")))
                .doesNotContain("<a class=\"site-link\"");
    }

    private Path reportWith(String sidecar) throws IOException {
        Path inDir = Files.createDirectory(tempDir.resolve("in"));
        writeClass(inDir);
        Path outDir = Files.createDirectory(tempDir.resolve("out"));
        Path configFile = tempDir.resolve(ReportConfigFile.DEFAULT_FILE_NAME);
        Files.writeString(configFile, sidecar);

        ReportConfiguration configuration =
                ReportConfigurationResolver.resolve(new ReportOptions("html", null, null, false, configFile));
        new TableTestReporter(configuration).report(inDir, outDir);
        return outDir;
    }

    private void writeClass(Path inDir) throws IOException {
        Path classDir = Files.createDirectory(inDir.resolve("org.example.AuthTest"));
        Files.writeString(classDir.resolve("TABLETEST-auth-test.yaml"), """
                "className": "org.example.AuthTest"
                "slug": "auth-test"
                "title": "Auth Test"
                "tableTests":
                  - "path": "login(String)/TABLETEST-login-validation.yaml"
                    "methodName": "login"
                    "slug": "login-validation"
                """);
        Path tableDir = Files.createDirectory(classDir.resolve("login(String)"));
        Files.writeString(tableDir.resolve("TABLETEST-login-validation.yaml"), """
                "title": "Auth Test"
                "headers":
                  - "value": "Input"
                  - "value": "Valid?"
                "rows":
                    - - "value": "x"
                      - "value": "true"
                """);
    }
}
