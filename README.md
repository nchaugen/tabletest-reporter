# TableTest Reporter

TableTest Reporter generates documentation from [TableTest](https://github.com/nchaugen/tabletest) results (YAML emitted by the JUnit `TableTest` extension). It renders AsciiDoc or Markdown files that you can publish with your documentation tooling.


## Features

- Generates documentation for TableTest methods and test classes
- Produces index per package and test class with navigation
- Built-in AsciiDoc and Markdown output formats
- Extensible templates (Pebble)
- Thin runners: CLI, Maven plugin, Gradle plugin


## Requirements

- Java 21+


## Modules

- `tabletest-reporter-core` – core rendering engine (pure Java library)
- `tabletest-reporter-cli` – command-line runner (fat JAR)
- `tabletest-reporter-maven-plugin` – Maven plugin goal `tabletest-reporter:report`
- `tabletest-reporter-gradle-plugin` – Gradle plugin (standalone subproject)


## Build

Build core, CLI, and Maven plugin with Maven:

```
mvn -q clean install
```

This also installs the artifacts to your local Maven repository for use by the Gradle plugin.


## CLI usage

Fat JAR path after build: `tabletest-reporter-cli/target/tabletest-reporter-cli-0.0.1-SNAPSHOT.jar`

Defaults:
- format: `asciidoc`
- input: `<buildDir>/junit-jupiter` (prefers `./target/junit-jupiter`, else `./build/junit-jupiter`)
- output: `<buildDir>/generated-docs/tabletest` (mirrors the same build dir)

Run with defaults (from a project where TableTest has produced YAML under the build directory):

```
java -jar tabletest-reporter-cli/target/tabletest-reporter-cli-0.0.1-SNAPSHOT.jar
```

Explicit arguments:

```
java -jar tabletest-reporter-cli/target/tabletest-reporter-cli-0.0.1-SNAPSHOT.jar \
  -f markdown \
  -i target/junit-jupiter \
  -o target/generated-docs/tabletest
```

Exit codes:
- `0` success
- `2` usage error (unknown format or missing input directory)
- `1` unexpected runtime failure


## Maven plugin usage

Ad-hoc goal invocation (defaults shown below):

```
mvn io.github.nchaugen:tabletest-reporter-maven-plugin:report
```

Properties (all optional):
- `-Dtabletest.report.format=asciidoc|markdown` (default: `asciidoc`)
- `-Dtabletest.report.inputDirectory=...` (default: `${project.build.directory}/junit-jupiter`)
- `-Dtabletest.report.outputDirectory=...` (default: `${project.build.directory}/generated-docs/tabletest`)

POM configuration example (bind to `site` or run on demand):

```xml
<build>
  <plugins>
    <plugin>
      <groupId>io.github.nchaugen</groupId>
      <artifactId>tabletest-reporter-maven-plugin</artifactId>
      <version>0.0.1-SNAPSHOT</version>
      <executions>
        <execution>
          <goals>
            <goal>report</goal>
          </goals>
        </execution>
      </executions>
      <!-- Optional overrides -->
      <configuration>
        <format>asciidoc</format>
        <inputDirectory>${project.build.directory}/junit-jupiter</inputDirectory>
        <outputDirectory>${project.build.directory}/generated-docs/tabletest</outputDirectory>
      </configuration>
    </plugin>
  </plugins>
  
</build>
```


## Gradle plugin usage

The Gradle plugin lives in `tabletest-reporter-gradle-plugin` as a standalone subproject.

Publish locally (once per version):

```
# From repo root – ensure core is installed so Gradle can resolve it
mvn -q -DskipTests install

# From the Gradle plugin subproject
cd tabletest-reporter-gradle-plugin
gradle publishToMavenLocal
```

In a consumer Gradle project:

`settings.gradle.kts`:

```
pluginManagement {
  repositories { mavenLocal(); gradlePluginPortal() }
}
```

`build.gradle.kts`:

```
plugins {
  id("io.github.nchaugen.tabletest-reporter") version "0.0.1-SNAPSHOT"
}

tableTestReporter {
  // optional overrides
  // format.set("markdown")
  // inputDir.set(layout.buildDirectory.dir("junit-jupiter"))
  // outputDir.set(layout.buildDirectory.dir("generated-docs/tabletest"))
}
```

Run:

```
./gradlew reportTableTests
```

Defaults:
- format: `asciidoc`
- input: `build/junit-jupiter`
- output: `build/generated-docs/tabletest`


## Templates

TableTest Reporter uses [Pebble Templates](https://pebbletemplates.io) as the templating engine. You can tweak the provided AsciiDoc/Markdown templates or supply your own to customize the output.

