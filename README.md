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
    <version>0.3.2</version>
    <scope>test</scope>
</dependency>
```

**Gradle:**
```kotlin
testImplementation("io.github.nchaugen:tabletest-reporter-junit:0.3.2")
```

### Enable Automatic Extension Detection

The extension uses JUnit's ServiceLoader mechanism to activate automatically. You must enable JUnit's automatic extension detection by setting `junit.jupiter.extensions.autodetection.enabled=true`.

**Which approach should I use?**

| Build Tool | Scenario                          | Recommended Approach                       |
|------------|-----------------------------------|--------------------------------------------|
| **Any**    | Standard projects                 | Option 1: `junit-platform.properties`      |
| **Maven**  | Simple setup (no Surefire config) | Option 2: Maven property                   |
| **Maven**  | Already using Surefire plugin     | Option 3: Surefire config                  |
| **Maven**  | Quarkus projects                  | Option 2: Maven property (avoids conflict) |
| **Gradle** | Standard projects                 | Option 4: Gradle config                    |
| **CLI**    | Running tests directly            | Option 5: Command-line argument            |

<details>
<summary><b>Option 1: JUnit Platform Properties</b> (Recommended for most projects)</summary>

Create `src/test/resources/junit-platform.properties`:

```properties
junit.jupiter.extensions.autodetection.enabled=true
```

**Pros:**
- Works with any build tool (Maven, Gradle, CLI)
- Configuration stays with test code
- Can also configure TableTest Reporter options (see [Configuration Options](#configuration-options))

**Cons:**
- Conflicts with Quarkus (which bundles its own `junit-platform.properties`)

</details>

<details>
<summary><b>Option 2: Maven Property</b> (Simplest for Maven)</summary>

Add to your `pom.xml`:

```xml
<properties>
    <junit.jupiter.extensions.autodetection.enabled>true</junit.jupiter.extensions.autodetection.enabled>
</properties>
```

**Pros:**
- Simplest Maven approach (no plugin configuration needed)
- Works with Quarkus (no conflict)
- Can be overridden from command line

**Cons:**
- Maven-specific

</details>

<details>
<summary><b>Option 3: Maven Surefire Configuration</b></summary>

If you're already configuring the Surefire plugin, add:

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

**Pros:**
- Standard approach when using Surefire plugin
- All test configuration in one place

**Cons:**
- Requires Surefire plugin configuration
- More verbose than Maven property approach

</details>

<details>
<summary><b>Option 4: Gradle Configuration</b></summary>

Add to your `build.gradle.kts`:

```kotlin
tasks.test {
    useJUnitPlatform()
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
}
```

**Custom output directory:** If you need YAML files written to a specific location, configure `junit.platform.reporting.output.dir`:

```kotlin
tasks.test {
    useJUnitPlatform()
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")

    // Optional: custom output directory
    val outputDir = layout.buildDirectory
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf("-Djunit.platform.reporting.output.dir=${outputDir.get().asFile.absolutePath}")
    }
}
```

This is not needed for standard projects — YAML files are written to `build/junit-jupiter/` by default. The reporter's Gradle plugin automatically detects custom output directories from the test task configuration. See [Input Directory Resolution](#input-directory-resolution) for details.

**Pros:**
- Standard Gradle approach for JUnit configuration
- All test configuration in build file

**Cons:**
- Gradle-specific

</details>

<details>
<summary><b>Option 5: Command Line</b></summary>

For one-off test runs or CI/CD:

**Maven:**
```bash
mvn test -Djunit.jupiter.extensions.autodetection.enabled=true
```

**Gradle:**
```bash
./gradlew test -Djunit.jupiter.extensions.autodetection.enabled=true
```

**Pros:**
- No configuration files needed
- Useful for CI/CD or one-off runs
- Easy to override existing configuration

**Cons:**
- Must remember to pass the argument every time
- Not suitable as permanent solution

</details>

---

With any of these configurations, the extension activates automatically—no test annotations required.

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

**Important: Scenario Column Requirement**

To enable pass/fail indicators (`.passed`/`.failed` CSS roles) in your generated documentation, **your table must include a scenario column**, either implicitly as a leading, undeclared column, or explicitly with the `@Scenario` annotation marker.

```java
@TableTest("""
    Scenario | Username | Password | Expected?
    Success  | admin    | secret   | true      ← .passed role applied
    Failure  | guest    | wrong    | false     ← .passed/.failed role applied
    """)
```

Without a scenario column, pass/fail roles cannot be reliably applied due to parameter type conversion. Tables without scenario columns will still generate documentation, but rows won't be marked with pass/fail indicators:

```java
@TableTest("""
    Username | Password | Expected?
    admin    | secret   | true      ← No .passed/.failed roles
    guest    | wrong    | false     ← No .passed/.failed roles
    """)
```

The scenario column can have any name (`Scenario`, `Test Case`, `Description`, etc.) and contain any unique string value to identify each test case.

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

Choose your build tool and run the reporter to generate AsciiDoc or Markdown documentation. The reporter automatically detects where your test framework writes YAML files, so in most standard Maven and Gradle projects you don't need to configure the input directory at all — just run the plugin. See [Input Directory Resolution](#input-directory-resolution) for details on how detection works and when manual configuration is needed.

### Maven Plugin

Add the plugin to your `pom.xml`:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>io.github.nchaugen</groupId>
      <artifactId>tabletest-reporter-maven-plugin</artifactId>
      <version>0.3.2</version>
    </plugin>
  </plugins>
</build>
```

Run the plugin:
```bash
mvn tabletest-reporter:report
```

**Note:** An `<executions>` section can be added if you want the plugin to run automatically during a specific Maven phase.

Documentation is generated to `target/generated-docs/tabletest/`.

**Configuration options:**
```xml
<configuration>
  <format>asciidoc</format>  <!-- or 'markdown' -->
  <inputDirectory>${project.build.directory}/junit-jupiter</inputDirectory>
  <outputDirectory>${project.build.directory}/generated-docs/tabletest</outputDirectory>
  <indexDepth>infinite</indexDepth>  <!-- levels in index (1, 2, ..., or 'infinite') -->
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
  id("io.github.nchaugen.tabletest-reporter") version "0.3.2"
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
  indexDepth.set("infinite")  // levels in index (1, 2, ..., or "infinite")
}
```

### Listing Available Formats

You can list all available output formats (built-in and custom) using the following commands:

**Maven:**
```bash
mvn tabletest-reporter:list-formats
```

**Gradle:**
```bash
./gradlew listTableTestReportFormats
```

**CLI:**
```bash
tabletest-reporter --list-formats
```

The output shows all available formats, sorted alphabetically. By default, you'll see the built-in formats:
```
asciidoc
markdown
```

When using custom templates with additional formats, those will also appear in the list.

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

### Configuration Options

TableTest Reporter can be configured through [JUnit Platform configuration parameters](https://docs.junit.org/current/running-tests/configuration-parameters.html).

**`tabletest.reporter.expectation.pattern`**

Defines a regular expression pattern to identify expectation columns in your test tables. By default, columns ending with `?` are treated as expectations.

Default: `.*\?$` (columns ending with question mark)

Example in `junit-platform.properties`:

```properties
# Prefix convention: "Expected Result", "Expected Value"
tabletest.reporter.expectation.pattern=^Expected.*

# Suffix convention: "resultExpected", "valueExpected"
tabletest.reporter.expectation.pattern=.*[Ee]xpected$

# Parenthetical notation: "value (expected)"
tabletest.reporter.expectation.pattern=.*\\(expected\\)$
```

### Input Directory Resolution

When you run the reporter, it needs to find the YAML files generated during your test run. In most cases, this is handled automatically.

**Resolution order:**

1. **Explicit configuration** — If you specify an input directory (via plugin config or CLI `-i` option), it is used directly
2. **Build tool detection** — The Maven and Gradle plugins read the JUnit output directory from your build configuration:
   - **Maven:** From Surefire's `configurationParameters` (`junit.platform.reporting.output.dir` property)
   - **Gradle:** From the test task's system properties or JVM argument providers
3. **Properties file** — From `junit.platform.reporting.output.dir` in `src/test/resources/junit-platform.properties`
4. **Convention fallback** — `<buildDir>/junit-jupiter` (e.g., `target/junit-jupiter` or `build/junit-jupiter`)

At each step, the reporter checks whether the candidate directory contains `TABLETEST-*.yaml` files. The first directory with matching files is selected.

**When auto-detection works (no configuration needed):**

- Standard Maven projects using Surefire (output goes to `target/junit-jupiter/`)
- Standard Gradle projects (output goes to `build/junit-jupiter/`)
- Projects configuring `junit.platform.reporting.output.dir` via Surefire, Gradle test task properties, or `junit-platform.properties`
- Gradle projects using custom output directories via `jvmArgumentProviders` or system properties (the Gradle plugin detects these automatically)

**When you need to specify the input directory explicitly:**

- **Non-standard engine IDs** — JUnit writes to `<buildDir>/<engine-id>/` by default. If your engine is not `junit-jupiter`, the convention fallback won't match
- **IDE-specific output directories** — When running tests from an IDE, outputs may go to a different location than the build tool expects
- **Custom `OutputDirectoryCreator` implementations** — If you've customised where JUnit writes report files
- **CLI without a build directory** — The CLI falls back to `target/junit-jupiter` or `build/junit-jupiter`; if neither exists, you must specify `-i`

**Configuring the input directory:**

Maven:
```bash
mvn tabletest-reporter:report -Dtabletest.report.inputDirectory=/path/to/yaml/files
```

Gradle:
```kotlin
tableTestReporter {
  inputDir.set(file("/path/to/yaml/files"))
}
```

CLI:
```bash
java -jar tabletest-reporter-cli.jar -i /path/to/yaml/files
```

### Custom Templates

TableTest Reporter uses [Pebble templates](https://pebbletemplates.io/) to generate documentation. You can customise the output by providing your own templates.

**Two approaches:**

1. **Template Extension** - Override specific parts (e.g., add front matter for Jekyll/Hugo)
2. **Template Replacement** - Completely replace the built-in templates

#### Convention-Based Discovery

Custom templates are discovered automatically by naming convention:

- `*-table.adoc.peb` or `*-table.md.peb` - Custom table templates
- `*-index.adoc.peb` or `*-index.md.peb` - Custom index templates

For example, `custom-table.adoc.peb` or `jekyll-table.md.peb` will be used automatically.

**Precedence:**
1. Exact match (e.g., `table.adoc.peb`) - complete replacement
2. Pattern match (e.g., `custom-table.adoc.peb`) - extension template
3. Built-in template - default

#### Configuring Custom Template Directory

**Maven Plugin:**
```xml
<configuration>
  <templateDirectory>${project.basedir}/templates</templateDirectory>
</configuration>
```

**Gradle Plugin:**
```kotlin
tableTestReporter {
  templateDir.set(file("templates"))
}
```

**CLI:**
```bash
java -jar tabletest-reporter-cli.jar \
  --template-dir templates \
  -f markdown \
  -i target/junit-jupiter \
  -o target/generated-docs/tabletest
```

#### Template Extension Example

Extend built-in templates by overriding specific blocks. Create `jekyll-table.md.peb`:

```pebble
{% extends "table.md.peb" %}
{% block frontMatter %}---
layout: default
title: {{ title }}
---

{% endblock %}
```

Available blocks for tables:
- `frontMatter` - Content before the document (e.g., Jekyll/Hugo front matter)
- `title` - Table title
- `description` - Table description
- `table` - Entire table
  - `tableHeaders` - Table header row
  - `tableRows` - Table body rows
- `failures` - Failed row details section
- `footer` - Content after the document

Available blocks for indexes:
- `frontMatter` - Content before the document
- `title` - Index title
- `description` - Index description
- `contents` - List of child pages
- `footer` - Content after the document

#### Template Replacement Example

Completely replace the built-in template. Create `table.adoc.peb`:

```asciidoc
= {{ title }}

Custom header content here.

[cols="{{ '1' | replicate(headers.size) | join(',') }}"]
|===
{% for header in headers %}
|{{ header.value }}
{% endfor %}

{% for row in rows %}
{% for cell in row %}
|{{ cell.value }}
{% endfor %}

{% endfor %}
|===

Generated on {{ "now" | date("yyyy-MM-dd") }}
```

Template context includes:
- `title` - Test display name
- `description` - Test description
- `headers` - List of header cells with `value` and `roles`
- `rows` - List of rows, each containing cells with `value` and `roles`
- `rowResults` - Test results with `displayName`, `passed`, and `errorMessage`

### Custom Output Formats

Beyond the built-in AsciiDoc and Markdown formats, you can define custom output formats (HTML, XML, JSON, etc.) by providing templates in your template directory.

**Requirements:**
- Both `table.{format}.peb` and `index.{format}.peb` must be present
- Format name becomes the file extension (e.g., "html" → ".html")

**Example: HTML Format**

Create `table.html.peb`:
```html
<!DOCTYPE html>
<html>
<head>
    <title>{{ title }}</title>
</head>
<body>
    <h2>{{ title }}</h2>
    {% if description %}<p>{{ description }}</p>{% endif %}
    <table>
        <thead>
            <tr>
            {% for header in headers %}
                <th>{{ header.value }}</th>
            {% endfor %}
            </tr>
        </thead>
        <tbody>
        {% for row in rows %}
            <tr>
            {% for cell in row %}
                <td>{{ cell.value }}</td>
            {% endfor %}
            </tr>
        {% endfor %}
        </tbody>
    </table>
</body>
</html>
```

Create `index.html.peb`:
```html
<!DOCTYPE html>
<html>
<head>
    <title>{{ title ? title : name }}</title>
</head>
<body>
    <h1>{{ title ? title : name }}</h1>
    {% if description %}<p>{{ description }}</p>{% endif %}
    <ul>
    {% for item in contents %}
        <li><a href="{{ item.path }}">{{ item.title }}</a></li>
    {% endfor %}
    </ul>
</body>
</html>
```

**Usage:**

Specify the custom format when running the reporter:

**Maven:**
```xml
<configuration>
  <format>html</format>
  <templateDirectory>${project.basedir}/templates</templateDirectory>
</configuration>
```

**Gradle:**
```kotlin
tableTestReporter {
  format.set("html")
  templateDir.set(file("templates"))
}
```

**CLI:**
```bash
java -jar tabletest-reporter-cli.jar \
  --template-dir templates \
  -f html \
  -i target/junit-jupiter \
  -o target/generated-docs/tabletest
```

If an unknown format is specified, you'll get a helpful error message listing all available formats (both built-in and discovered custom formats).

### Styling HTML Reports

When generating HTML from AsciiDoc reports, you can apply custom CSS styling based on the roles generated by TableTest Reporter.

**Understanding CSS Class Placement**

TableTest Reporter generates AsciiDoc with custom roles (`.scenario`, `.expectation`, `.passed`, `.failed`) that become CSS classes in HTML output. These classes are applied to **inline elements inside table cells** (such as `span`, `ul`, `ol`), not directly to the `th`/`td` elements.

To style cells based on their content's roles, use the CSS `:has()` selector:

```css
/* Scenario column - light yellow background */
:is(th, td):has(.scenario) {
    background-color: #fffacd;
    font-style: italic;
}

/* Expectation columns - light blue background */
:is(th, td):has(.expectation) {
    background-color: #add8e6;
}

/* Passed rows - light green background */
:is(th, td):has(.passed) {
    background-color: #90ee90;
}

/* Failed rows - light red background */
:is(th, td):has(.failed) {
    background-color: #ffcccb;
}

/* Expectation cells in passed rows - bold green */
:is(th, td):has(.expectation.passed) {
    background-color: #32cd32;
    font-weight: bold;
}

/* Expectation cells in failed rows - bold red */
:is(th, td):has(.expectation.failed) {
    background-color: #ff6347;
    font-weight: bold;
}
```

**Note:** Use `:has(.classname)` without specifying element type, since roles may be applied to different elements depending on content.

**Asciidoctor Maven Plugin Configuration**

Configure the [asciidoctor-maven-plugin](https://docs.asciidoctor.org/maven-tools/latest/) to use your custom stylesheet:

```xml
<plugin>
    <groupId>org.asciidoctor</groupId>
    <artifactId>asciidoctor-maven-plugin</artifactId>
    <version>3.2.0</version>
    <configuration>
        <sourceDirectory>${project.build.directory}/generated-docs/tabletest</sourceDirectory>
        <outputDirectory>${project.build.directory}/generated-html/tabletest</outputDirectory>
        <backend>html5</backend>
        <preserveDirectories>true</preserveDirectories>
        <attributes>
            <stylesheet>tabletest.css</stylesheet>
            <stylesdir>${project.basedir}/src/main/resources</stylesdir>
            <copycss>true</copycss>
        </attributes>
    </configuration>
</plugin>
```

Key attributes:
- `stylesheet` - Name of your CSS file
- `stylesdir` - Directory containing your CSS file
- `copycss` - Embeds CSS in each HTML file (set to `false` and use `linkcss` for external stylesheet)

See the [Asciidoctor stylesheet documentation](https://docs.asciidoctor.org/asciidoc/latest/docinfo/stylesheet/) for more options.

**Working Example**

A complete working example is available in the project's compatibility tests: [`compatibility-tests/junit-6-maven/`](compatibility-tests/junit-6-maven/)

### For Plugin Developers

**CLI Usage:**

The CLI can be used standalone if you're building custom tooling:

```bash
java -jar tabletest-reporter-cli.jar \
  -f markdown \
  -i target/junit-jupiter \
  -o target/generated-docs/tabletest \
  --index-depth 2  # levels in index (1, 2, ..., or 'infinite')
```

**Building from Source:**

```bash
# Build core, CLI, and Maven plugin
mvn clean install

# Build Gradle plugin (separate subproject)
cd tabletest-reporter-gradle-plugin
gradle publishToMavenLocal
```

**Setting Up Git Hooks:**

The project includes git hooks for commit message validation and other checks that are handy when working with code agents. After cloning, enable them:

```bash
git config core.hooksPath git-hooks
```

This configures git to use the versioned hooks in the `git-hooks/` directory instead of `.git/hooks/`.

