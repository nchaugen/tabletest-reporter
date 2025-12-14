# TableTest Reporter

TableTest Reporter generates documentation from your [TableTest](https://github.com/nchaugen/tabletest) tests. It turns your test tables into readable AsciiDoc or Markdown documentation that you can publish alongside your project docs.

## Quick Start

1. Add the JUnit extension to your test dependencies
2. Run your tests (YAML files are generated automatically)
3. Run the reporter plugin to generate documentation

## Requirements

- Java 21+
- JUnit 5.12+
- [TableTest](https://github.com/nchaugen/tabletest) for your tests

Popular frameworks like Spring Boot (3.5.0+) and Quarkus (3.21.2+) include compatible JUnit versions.

## Step 1: Add the JUnit Extension

Add the TableTest Reporter JUnit extension to your test dependencies. This extension automatically collects test information when you run your tests.

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

The extension activates automatically via JUnit's ServiceLoader mechanism—no configuration or annotations required.

## Step 2: Write Your Tests

Write your TableTest tests as usual. The reporter uses standard JUnit annotations to enhance the generated documentation:

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

**What gets collected:**
- **Titles:** From `@DisplayName` annotations (class and method level)
- **Descriptions:** From TableTest `@Description` annotations
- **Table structure:** Headers, rows, and column roles (scenario vs expectation columns)
- **Test results:** Pass/fail status for each row

## Step 3: Run Your Tests

Run your tests normally. The extension automatically generates YAML files in `<buildDir>/junit-jupiter/`:

```bash
# Maven
mvn test

# Gradle
./gradlew test
```

Each TableTest method produces a YAML file with prefix `TABLETEST-`. File names are web-friendly kebab-case versions of your test names:
- `"Login Validation"` → `TABLETEST-login-validation.yaml`
- `testUserPermissions` → `TABLETEST-test-user-permissions.yaml`
- `leap_year_rules` → `TABLETEST-leap-year-rules.yaml`

## Step 4: Generate Documentation

Choose your build tool and run the reporter to generate AsciiDoc or Markdown documentation.

### Maven Plugin

Add the plugin to your `pom.xml`:

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
    </plugin>
  </plugins>
</build>
```

Run the plugin:
```bash
mvn tabletest-reporter:report
```

Documentation is generated to `target/generated-docs/tabletest/`.

**Configuration options:**
```xml
<configuration>
  <format>asciidoc</format>  <!-- or 'markdown' -->
  <inputDirectory>${project.build.directory}/junit-jupiter</inputDirectory>
  <outputDirectory>${project.build.directory}/generated-docs/tabletest</outputDirectory>
</configuration>
```

Or use command-line properties:
```bash
mvn tabletest-reporter:report -Dtabletest.report.format=markdown
```

### Gradle Plugin

Add the plugin to your `build.gradle.kts`:

```kotlin
plugins {
  id("io.github.nchaugen.tabletest-reporter") version "0.1.1"
}
```

Run the task:
```bash
./gradlew reportTableTests
```

Documentation is generated to `build/generated-docs/tabletest/`.

**Configuration options:**
```kotlin
tableTestReporter {
  format.set("markdown")  // default: "asciidoc"
  inputDir.set(layout.buildDirectory.dir("junit-jupiter"))
  outputDir.set(layout.buildDirectory.dir("generated-docs/tabletest"))
}
```

## Output Structure

The reporter generates documentation that mirrors your test package structure:

```
generated-docs/tabletest/
├── index.adoc                           # Root index with all packages
└── com/
    └── example/
        ├── index.adoc                   # Package index
        └── authentication-test/         # Test class directory
            ├── index.adoc               # Test class index
            ├── login-validation.adoc    # Individual test method
            └── password-reset.adoc      # Individual test method
```

**Generated structure:**
- **Root index:** Lists all packages with links
- **Package indexes:** List all test classes in that package (each in `index.adoc`)
- **Test class directories:** Each test class gets its own directory
- **Test class index:** Lists all test methods in that class (`index.adoc` in the class directory)
- **Test method pages:** Individual files with full table data and results

Directory and file names are kebab-case versions of your test class and method names, making them URL-friendly for web publishing.

## Publishing Your Documentation

The generated AsciiDoc or Markdown files can be published with standard documentation tools:

- **AsciiDoc:** Use Asciidoctor Maven/Gradle plugins to convert to HTML
- **Markdown:** Use your static site generator (Jekyll, Hugo, MkDocs, etc.)
- **GitHub Pages:** Commit the generated files to your docs directory

---

## Advanced Topics

### For Plugin Developers

**CLI Usage:**

The CLI can be used standalone if you're building custom tooling:

```bash
java -jar tabletest-reporter-cli.jar \
  -f markdown \
  -i target/junit-jupiter \
  -o target/generated-docs/tabletest
```

**Building from Source:**

```bash
# Build core, CLI, and Maven plugin
mvn clean install

# Build Gradle plugin (separate subproject)
cd tabletest-reporter-gradle-plugin
gradle publishToMavenLocal
```

