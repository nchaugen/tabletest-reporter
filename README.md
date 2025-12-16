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

Add the TableTest Reporter JUnit extension to your test dependencies and enable JUnit's automatic extension detection.

### Add Dependency

**Maven:**
```xml
<dependency>
    <groupId>io.github.nchaugen</groupId>
    <artifactId>tabletest-reporter-junit</artifactId>
    <version>0.2.0</version>
    <scope>test</scope>
</dependency>
```

**Gradle:**
```kotlin
testImplementation("io.github.nchaugen:tabletest-reporter-junit:0.2.0")
```

### Enable Automatic Extension Detection

The extension uses JUnit's ServiceLoader mechanism to activate automatically. You must configure JUnit to enable automatic extension detection.

**Recommended: JUnit Platform Properties**

Create `src/test/resources/junit-platform.properties`:

```properties
junit.jupiter.extensions.autodetection.enabled=true
```

This approach works with any build tool (Maven, Gradle, etc.) and keeps JUnit configuration with your test code.

**Alternative: Build Tool Configuration**

If you prefer to configure this in your build file:

<details>
<summary>Maven (Surefire Plugin)</summary>

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-surefire-plugin</artifactId>
      <version>3.5.2</version>
      <configuration>
        <properties>
          <configurationParameters>
            junit.jupiter.extensions.autodetection.enabled=true
          </configurationParameters>
        </properties>
      </configuration>
    </plugin>
  </plugins>
</build>
```

**Note for Quarkus projects:** If your Quarkus project already has `junit-platform.properties` for other JUnit configuration, you should use the Surefire plugin configuration above instead of adding `junit.jupiter.extensions.autodetection.enabled=true` to the properties file. Older Quarkus versions had conflicts with custom `junit-platform.properties` settings.

</details>

<details>
<summary>Gradle (Test Task)</summary>

```kotlin
tasks.test {
    useJUnitPlatform()
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
}
```
</details>

With this configuration, the extension activates automatically—no test annotations required.

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
      <version>0.2.0</version>
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
  id("io.github.nchaugen.tabletest-reporter") version "0.2.0"
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

The reporter generates documentation that mirrors your test package structure. The output starts at the point where your package structure branches.

**Single test class** (`com.example.AuthenticationTest`):
```
generated-docs/tabletest/
├── index.adoc                    # Test class index
├── login-validation.adoc         # Test method
└── password-reset.adoc           # Test method
```

**Multiple classes in one package** (`com.example.AuthenticationTest` and `com.example.OrderTest`):
```
generated-docs/tabletest/
├── index.adoc                    # Package index for 'example'
├── authentication-test/
│   ├── index.adoc                # Test class index
│   ├── login-validation.adoc
│   └── password-reset.adoc
└── order-test/
    ├── index.adoc
    └── place-order.adoc
```

**Multiple packages** (`com.example.*` and `com.tools.*`):
```
generated-docs/tabletest/
├── index.adoc                    # Package index for 'com'
├── example/
│   ├── index.adoc                # Package index for 'example'
│   └── authentication-test/
│       ├── index.adoc
│       └── login-validation.adoc
└── tools/
    ├── index.adoc                # Package index for 'tools'
    └── parser-test/
        ├── index.adoc
        └── parse-json.adoc
```

The structure eliminates redundant directory levels—only the branching parts of your package hierarchy appear in the output. Directory and file names are kebab-case versions of your package, class, and method names.

## Publishing Your Documentation

The generated AsciiDoc or Markdown files can be published with standard documentation tools:

- **AsciiDoc:** Use Asciidoctor Maven/Gradle plugins to convert to HTML
- **Markdown:** Use your static site generator (Jekyll, Hugo, MkDocs, etc.)

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

