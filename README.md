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
- JUnit 5.12+

Popular frameworks SpringBoot and Quarkus includes JUnit in their distributions. SpringBoot versions from 3.5.0 and Quarkus versions from 3.21.2 include a recent enough JUnit version for TableTest-Reporter to work.

## Modules

- `tabletest-reporter-junit` – JUnit extension to collect TableTest information during test runs
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

## Collecting TableTest Results and Metadata

The `tabletest-reporter-junit` module is a JUnit extension that automatically publishes YAML files during test execution. It collects TableTest results and metadata, making them available for report generation by the CLI, Maven plugin, or Gradle plugin.

### Installation

Add the dependency to your test scope:

**Maven:**
```xml
<dependency>
    <groupId>io.github.nchaugen</groupId>
    <artifactId>tabletest-reporter-junit</artifactId>
    <version>0.1.1</version>
    <scope>test</scope>
</dependency>
```

**Gradle:**
```kotlin
testImplementation("io.github.nchaugen:tabletest-reporter-junit:0.1.1")
```

### Automatic Activation

The extension is automatically activated via JUnit's ServiceLoader mechanism when present on the test classpath. No explicit configuration or annotations are required.

When you run your tests, the extension publishes YAML files to `<buildDir>/junit-jupiter/` containing:

- Test metadata (titles and descriptions)
- Table structure (headers and rows)
- Column roles (scenario, expectation)
- Row execution results (pass/fail)

### Metadata Collection

The extension collects the following metadata:

**Titles:** Use JUnit's standard `@DisplayName` annotation on test classes and methods. These become titles in the generated documentation.

**Descriptions:** Use the TableTest `@Description` annotation to provide detailed explanations for test classes and methods.

**Column Roles:**
- Scenario columns (either implicitly defined or explictly marked with the `@Scenario` annotation)
- Expectation columns are detected when headers end with `?`

**Example:**
```java
@DisplayName("User Authentication")
@Description("Tests for user login and authentication scenarios")
class AuthenticationTest {

    @DisplayName("Login Validation")
    @Description("Validates login with various username/password combinations")
    @TableTest("""
        Scenario | Username | Password | Expected?
        Success  | admin    | secret   | true
        Failure  | guest    | wrong    | false
        """)
    void testLogin(String username, String password, boolean shouldSucceed) {
        // test implementation
    }
}
```

### Integration Workflow

1. **Write tests** with `@TableTest` annotations
2. **Run tests** – YAML files are automatically generated
3. **Generate reports** using one of:
   - CLI: `java -jar tabletest-reporter-cli.jar`
   - Maven: `mvn tabletest-reporter:report`
   - Gradle: `./gradlew reportTableTests`


## CLI usage

Fat JAR path after build: `tabletest-reporter-cli/target/tabletest-reporter-cli-0.1.0-SNAPSHOT.jar`

Defaults:
- format: `asciidoc`
- input: `<buildDir>/junit-jupiter` (prefers `./target/junit-jupiter`, else `./build/junit-jupiter`)
- output: `<buildDir>/generated-docs/tabletest` (mirrors the same build dir)

Run with defaults (from a project where TableTest has produced YAML under the build directory):

```
java -jar tabletest-reporter-cli/target/tabletest-reporter-cli-0.1.0-SNAPSHOT.jar
```

Explicit arguments:

```
java -jar tabletest-reporter-cli/target/tabletest-reporter-cli-0.1.0-SNAPSHOT.jar \
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
      <version>0.1.1</version>
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
  id("io.github.nchaugen.tabletest-reporter") version "0.1.1"
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

