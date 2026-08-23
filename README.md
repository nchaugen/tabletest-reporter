> [!IMPORTANT]
> TableTest Reporter has new Maven coordinates: `org.tabletest:tabletest-reporter-*:1.4.0`
>
> Please update your dependencies to keep receiving updates.

# TableTest Reporter

TableTest Reporter generates documentation from your [TableTest](https://github.com/nchaugen/tabletest) tests. It turns your test tables into readable AsciiDoc or Markdown documentation that you can publish alongside your project docs.

## Quick Start

1. Add the reporter plugin to your build
2. Run your tests (YAML files are generated automatically)
3. Run the reporter plugin to generate documentation

For Gradle users, the plugin handles everything automatically. Maven users need to add the dependency and configure autodetection manually.

## Requirements

- Java 17+ to run your tests (the JUnit extension targets 17)
- Java 21+ to generate the report (CLI, Maven plugin, or Gradle plugin — this is your build
  JVM, which need not match the Java version your project targets)
- JUnit 5.12+ or JUnit 6
- [TableTest](https://github.com/nchaugen/tabletest) 1.0.0+ for your tests
- Gradle 8.14+ if you use the Gradle plugin

Popular frameworks like Spring Boot (3.5.0+) and Quarkus (3.21.2+) include compatible JUnit versions.

These are the versions the compatibility test suite exercises on every change: JUnit 5.12.0 and 6.1.2 on both Maven and Gradle, Spring Boot 3.5.0 and current, Quarkus 3.21.2 and current.

## Step 1: Add the Reporter Plugin

### Gradle

Add the plugin to your `build.gradle.kts`:

```kotlin
plugins {
  id("org.tabletest.reporter") version "1.4.0"
}
```

The plugin automatically:
- Adds `tabletest-reporter-junit` to your `testImplementation` configuration
- Configures `junit.jupiter.extensions.autodetection.enabled=true` on test tasks

That's it! No additional configuration needed for standard projects.

### Maven

Add the dependency and plugin to your `pom.xml`, and enable JUnit extension autodetection:

```xml
<dependencies>
    <dependency>
        <groupId>org.tabletest</groupId>
        <artifactId>tabletest-reporter-junit</artifactId>
        <version>1.4.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>

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
    <plugin>
      <groupId>org.tabletest</groupId>
      <artifactId>tabletest-reporter-maven-plugin</artifactId>
      <version>1.4.0</version>
    </plugin>
  </plugins>
</build>
```

The `report` goal generates documentation from the collected test data.

<details>
<summary><b>Manual Setup (Advanced)</b></summary>

If you need manual control over the JUnit extension setup (e.g., for the CLI runner or custom configurations), you can add the dependency and configure autodetection manually:

**Add Dependency:**

Maven:
```xml
<dependency>
    <groupId>org.tabletest</groupId>
    <artifactId>tabletest-reporter-junit</artifactId>
    <version>1.4.0</version>
    <scope>test</scope>
</dependency>
```

Gradle:
```kotlin
testImplementation("org.tabletest:tabletest-reporter-junit:1.4.0")
```

**Enable Automatic Extension Detection:**

The extension uses JUnit's ServiceLoader mechanism. Enable autodetection using one of these approaches:

| Build Tool | Scenario                          | Recommended Approach                       |
|------------|-----------------------------------|--------------------------------------------|
| **Any**    | Standard projects                 | `junit-platform.properties`                |
| **Maven**  | Simple setup (no Surefire config) | Maven property                             |
| **Maven**  | Already using Surefire plugin     | Surefire config                            |
| **Maven**  | Quarkus projects                  | Maven property (avoids conflict)           |
| **Gradle** | Standard projects                 | Gradle test task config                    |
| **CLI**    | Running tests directly            | Command-line argument                      |

**Option: JUnit Platform Properties**

Create `src/test/resources/junit-platform.properties`:

```properties
junit.jupiter.extensions.autodetection.enabled=true
```

**Option: Maven Property**

```xml
<properties>
    <junit.jupiter.extensions.autodetection.enabled>true</junit.jupiter.extensions.autodetection.enabled>
</properties>
```

**Option: Gradle Test Task**

```kotlin
tasks.test {
    useJUnitPlatform()
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
}
```

</details>

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

### Column Roles

Every published cell carries the roles of its column. The reporter derives four itself — `scenario`,
`expectation`, `passed` and `failed` — and a test can declare more.

**A column of source text: `@Lines`.** A table keeps every row on one line, so a multi-line value is
written as a list of lines. Mark the column with `@Lines` and the parameter receives the lines joined
by newlines, while the HTML report renders the cell as a stacked monospace block instead of a
bulleted list:

```java
@TableTest("""
    Scenario   | Source                             | Table Count?
    One table  | ["a | b", "1 | 2"]                 | 1
    Two tables | ["a | b", "1 | 2", "", "c", "3"]   | 2
    """)
void countsTables(@Lines String source, int tableCount) {
    assertEquals(tableCount, parser.parse(source).size());
}
```

Declare the parameter as a `List<String>` instead to receive the lines themselves. The published
cell is unchanged either way — the reporter publishes the value the row ran with, so the cell is
still the list of lines as it was written.

**A role of your own.** Annotate an annotation with `@ColumnRole` and put it on a parameter:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@ColumnRole
public @interface Ingredient {}
```

The role is published as the annotation's simple name in kebab case — `@SourceLines` publishes
`source-lines` — or as the token `@ColumnRole("...")` names. It reaches the HTML report as a CSS
class on the cell and the AsciiDoc report as an element role, so your own stylesheet can style the
column; Markdown carries no roles. A role must be lower-case words joined by single hyphens, and one
naming a role the reporter derives is published anyway — the column is then styled as that role
without being treated as one.

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

Run the reporter to generate AsciiDoc or Markdown documentation. The reporter automatically detects where your test framework writes YAML files, so in most standard Maven and Gradle projects you don't need to configure the input directory at all — just run the plugin. See [Input Directory Resolution](#input-directory-resolution) for details on how detection works and when manual configuration is needed.

### Maven

If you configured the `report` goal in your plugin executions (see Step 1), documentation is generated automatically during the build. Otherwise, run manually:

```bash
mvn tabletest-reporter:report
```

Documentation is generated to `target/generated-docs/tabletest/`.

**Configuration options:**
```xml
<configuration>
  <format>asciidoc</format>  <!-- or 'markdown', 'html' -->
  <inputDirectory>${project.build.directory}/junit-jupiter</inputDirectory>
  <outputDirectory>${project.build.directory}/generated-docs/tabletest</outputDirectory>
  <indexDepth>infinite</indexDepth>  <!-- levels in index (1, 2, ..., or 'infinite') -->
  <configFile>${project.basedir}/tabletest-reporter.yaml</configFile>  <!-- spec metadata + publish selection, see below -->
  <inputDirectories>  <!-- several modules merged into one spec, see below -->
    <dir>${project.build.directory}/junit-jupiter</dir>
    <dir>${project.basedir}/../other-module/target/junit-jupiter</dir>
  </inputDirectories>
</configuration>
```

Or use command-line properties:
```bash
mvn tabletest-reporter:report -Dtabletest.report.format=markdown
```

### Gradle

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
  configFile.set(layout.projectDirectory.file("tabletest-reporter.yaml"))  // spec metadata + publish selection, see below
  inputDirs.from(layout.buildDirectory.dir("junit-jupiter"))  // several modules merged into one spec, see below
}
```

### Multi-module builds (one spec from several modules)

A single spec can span the modules of a multi-module build. The report tree comes from the
test class names inside the YAML, not from where the files sit, so modules merge into one
package hierarchy. A listed directory that does not exist is skipped with a warning, so a
partial build still publishes what it has.

**Maven** — let the plugin walk the reactor:
```bash
mvn tabletest-reporter:aggregate
```
The `aggregate` goal runs on the aggregator project, finds each module's TableTest output
the way the `report` goal finds its own (the JUnit output directory a module configures,
else its `target/junit-jupiter`), and writes one report to the aggregator's output
directory. To name the directories yourself instead — the option when the goal cannot run
inside the reactor — use `<inputDirectories>` on the `report` goal (shown above), or:
```bash
mvn tabletest-reporter:report -Dtabletest.report.inputDirectories=target/junit-jupiter,../other/target/junit-jupiter
```

**Gradle** — list the subprojects' directories:
```kotlin
tableTestReporter {
  inputDirs.from(
    layout.buildDirectory.dir("junit-jupiter"),
    project(":other-module").layout.buildDirectory.dir("junit-jupiter")
  )
}
```

**CLI** — repeat `-i`:
```bash
tabletest-reporter -i core/target/junit-jupiter -i junit/target/junit-jupiter
```

Where two modules published the same test class, the most recently written output wins —
the same rule that settles repeated runs within one directory.

### Spec metadata (`tabletest-reporter.yaml`)

By default the root index of a spec is titled by the deepest common package segment (e.g.
"junit" or "example") and intermediate index pages show lowercase package names. Drop an
optional `tabletest-reporter.yaml` in the project directory to give the spec a real title
and intro, retitle intermediate pages, and set an explicit feature reading order:

```yaml
title: "TableTest Core — Specification"
intro: >
  Generated from the executable TableTest suite. Every scenario below runs in CI;
  a broken row means the documented behaviour regressed.
features:
  - name: formatter          # matches an index page by its path segment / slug
    title: "Table Formatter"
    description: >           # a paragraph introducing the feature on its own index page
      How a table is laid out, in the order the formatter works in.
    features:                # list order is the reading order (not alphabetical)
      - { name: extraction,   title: "Value Extraction" }
      - { name: displaywidth, title: "Display Width" }
  - name: examples
    title: "Worked Examples"
```

Everything is optional. Declared features render first in the order given; undeclared
siblings follow alphabetically. A `description` is rendered under the feature's title on its
index page, the way a test class's `@Description` is — use it for what the whole group has in
common, so an individual rule need not repeat it. A feature that matches no page is logged and skipped, so a
typo never fails the report. The file is read at report time — a project without it reports
exactly as before. Override its location with the `configFile` option (Maven `<configFile>`,
Gradle `configFile`, CLI `--config`).

### Linking back to your site (`site`)

Every link inside a generated report is relative within its own tree. A reader who reaches
the spec from your site therefore has no way back to it. Add a `site` section to the same
`tabletest-reporter.yaml` to put one link in the footer of every HTML page:

```yaml
site:
  label: "TableTest"                 # the link text; defaults to the address itself
  url: "https://tabletest.org/"      # used exactly as written
```

The address is never resolved against the report's own tree, so a root-relative `/` or
`/docs/` works for a site that hosts the spec below it, and an absolute URL works from
anywhere. Without a `site` section the footer holds the attribution alone, as before.

The footer is a `footer` block in each of the three HTML page templates, and the link has a
`siteLink(site)` macro of its own — a template of yours can override the block, or call the
macro to place the same link elsewhere. See [Custom Templates](#custom-templates).

### Selecting what publishes (`publish`)

By default every table that ran is published. Add a `publish` section to the same
`tabletest-reporter.yaml` to hold pages back — at report time, so changing what publishes
does not mean running the test suite again, and no test-framework tags are involved:

```yaml
publish:
  exclude:
    - parsing                            # this feature page and everything below it
    - converting/convert-with            # one table page
    - "**/kotlin-*"                      # any page whose name starts with "kotlin-"
  include:
    - converting/convert-with/precedence # re-admitted, though its class is excluded
```

Paths name pages the way the report's URLs do: the page names from the root index down,
separated by `/`. Within a name, `*` matches any part of it; a whole segment of `**`
matches any number of levels. Excluding a page takes its whole subtree with it, and a
feature page left with nothing published under it disappears too. `include` wins over
`exclude`, so a single rule table can still publish from an otherwise internal class. A
path matching no page is logged and skipped, like a mistyped feature name.

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
html
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

### How names become filenames

A `@DisplayName`, a Kotlin backtick name, or a camelCase method name is reduced to a lowercase name usable as both a filename and a URL segment. Words are split on spaces, underscores, and camelCase boundaries; every other run of punctuation or symbols becomes a single hyphen.

Names outside plain ASCII are handled as follows:

| Name | Filename | Rule |
|---|---|---|
| `Leap Year Rules` | `leap-year-rules` | words split on spaces |
| `parseHTMLDocument` | `parse-html-document` | camelCase and acronyms split |
| `naïve façade` | `naive-facade` | accents fold to the base letter |
| `Grüße aus München` | `grusse-aus-munchen` | ligatures expand: `ß`→`ss`, `æ`→`ae`, `œ`→`oe` |
| `ÆØÅ` | `aeoa` | stroked letters fold: `ø`→`o`, `ł`→`l`, `đ`→`d`, `ð`→`d` |
| `Þingvellir` | `thingvellir` | `þ`→`th`, having no Latin base letter |
| `ﬁle ﬂow` | `file-flow` | compatibility forms reduce to what they stand for |
| `Москва` | `москва` | a name with no ASCII form keeps its own script |
| `日本語のテスト` | `日本語のテスト` | likewise for CJK, Greek, Devanagari, and the rest |

A name written in its own script is published as-is: those are legal filenames on every supported platform and legal URLs once a browser percent-encodes them, and GitHub Pages serves UTF-8 paths. A name with no letters or digits at all falls back to `unnamed-` plus a stable hash of the name, so two such names still get two pages.

Two tables in the same class that reduce to the same filename are disambiguated with a numeric suffix (`-1`, `-2`).

## Publishing Your Documentation

The `html` format is publishable as it stands: every page is self-contained and every link
relative, so the output directory can be copied to a static host with no build step. The
AsciiDoc and Markdown formats are intermediate sources for a toolchain you already run:

- **HTML:** Copy the output directory to any static host — no conversion step
- **AsciiDoc:** Use Asciidoctor Maven/Gradle plugins to convert to HTML
- **Markdown:** Use your static site generator (Jekyll, Hugo, MkDocs, etc.)

### GitHub Pages via Actions

Living documentation is only worth publishing if it keeps up with the code, which means
generating it in CI rather than committing it. The workflow below runs the tests, generates
the HTML report, and deploys it to Pages. Because the report is generated from a test run,
the `test` phase must run in the same invocation as the `report` goal.

```yaml
name: Publish living documentation

on:
  push:
    branches: [main]
  # Optional: rebuild on a schedule so the docs track the code even without a push
  schedule:
    - cron: '30 6 * * *'

permissions:
  contents: read
  pages: write
  id-token: write

# Let a running deployment finish rather than cancelling it
concurrency:
  group: "pages"
  cancel-in-progress: false

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'   # the reporter's JUnit extension needs 21+
          cache: 'maven'
      - name: Generate living documentation
        run: >
          mvn test org.tabletest:tabletest-reporter-maven-plugin:report
          -Dtabletest.report.format=html
          --no-transfer-progress
      - uses: actions/configure-pages@v4
      - uses: actions/upload-pages-artifact@v3
        with:
          path: target/generated-docs/tabletest

  deploy:
    needs: build
    runs-on: ubuntu-latest
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    steps:
      - id: deployment
        uses: actions/deploy-pages@v4
```

For a multi-module build, swap the generate step for the `aggregate` goal so every module
lands in one spec:

```yaml
      - name: Generate living documentation
        run: >
          mvn test org.tabletest:tabletest-reporter-maven-plugin:aggregate
          -Dtabletest.report.format=html
          --no-transfer-progress
```

Project Pages are served from `/<repo>/`, which the report handles without configuration —
all its links and assets are relative.

### Other hosting options

| Where | How | Good for |
|---|---|---|
| **GitHub Pages** | The workflow above | The default: a browsable URL that tracks `main` |
| **Build artifact** | `actions/upload-artifact` on the output directory | Private repos, or docs not meant to be public |
| **Per-PR preview** | Deploy the output to a preview environment keyed by PR number | Reviewing how a change reads before it merges |
| **Release asset** | Attach the `--single-file` HTML to the release | A frozen spec for a released version |
| **Netlify / Vercel** | Point at the output directory as the publish directory | Preview URLs and custom domains without Pages |

Single-file mode (`--single-file`) is the one to reach for whenever a directory of files is
awkward — release assets, email, ticket attachments.

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

### Built-in HTML Format

The `html` format renders self-contained, living documentation that needs no Asciidoctor
step. Each page is a standalone `.html` file with inline CSS and JavaScript (no external
references), so the output tree works directly on any static host, including GitHub Pages.

```bash
tabletest-reporter -f html -i target/junit-jupiter -o target/generated-docs/tabletest
```

Each table page includes:
- wide, autowidth tables in a horizontal-scroll wrapper with a sticky header row and first column
- nested collections rendered structurally (lists, sets, and maps with distinct markers)
- a pass/fail badge, per-row and per-cell status colouring, and collapsible failure details
- expectation- and scenario-column emphasis, a roles legend, per-page row filter, and a
  "failing only" toggle
- a light/dark theme toggle and a print stylesheet

Each index page shows a link tree of its tables and sub-packages, with a status dot on every
entry and a summary of the scenario pass rate ("N of M scenarios broken", or "All passing")
rolled up from the tables beneath it. Every page also carries a breadcrumb trail of its
ancestors (root package → class → table) and a menu button that opens a navigation drawer
with the whole-report tree — the current page highlighted, status dots throughout — so you
can jump anywhere from any page. The drawer slides in over the content, keeping the full
page width available for wide tables.

The drawer also has a search box that searches the whole report — every page's title,
description, headers, and cell values — and lists the matching pages (with status dots) to
jump to. Search is backed by a single `tabletest-search-index.js`, written once to the output
root and linked from each page by a relative prefix, so it works offline (opened via
`file://`) and under any subpath without any external requests.

Because every link and asset reference is relative, the generated tree deploys unchanged
under a project subpath (e.g. GitHub *project* Pages served from `/<repo>/`). The
`tabletest-search-index.js` asset sits at the output root alongside the root `index.html`.

#### Single-file mode

Add `--single-file` (`-s`) to assemble the whole report into **one** self-contained
`.html` instead of a directory tree:

```bash
tabletest-reporter -f html --single-file -i target/junit-jupiter -o target/generated-docs/tabletest
```

Every table is inlined as an anchored section, the sidebar navigation and search jump to
in-page anchors, and the search index is embedded — so the single `index.html` has no
sibling assets and no external references at all. This is the most portable form: attach it
to a release, email or ticket where a directory of files is awkward. The multi-file tree
remains the default (better for GitHub Pages and per-page linking). Single-file mode
currently applies to the `html` format only.

To customise the markup, drop your own `table.html.peb` / `index.html.peb` into a template
directory — an exact filename match overrides the built-in template (see below).

### Custom Output Formats

Beyond the built-in AsciiDoc, Markdown, and HTML formats, you can define custom output formats (XML, JSON, etc.) by providing templates in your template directory.

**Requirements:**
- Both `table.{format}.peb` and `index.{format}.peb` must be present
- Format name becomes the file extension (e.g., "xml" → ".xml")

**Example: XML Format**

Create `table.xml.peb`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<table title="{{ title }}">
    {% if description %}<description>{{ description }}</description>{% endif %}
    <headers>
    {% for header in headers %}
        <header>{{ header.value }}</header>
    {% endfor %}
    </headers>
    <rows>
    {% for row in rows %}
        <row>
        {% for cell in row %}
            <cell>{{ cell.value }}</cell>
        {% endfor %}
        </row>
    {% endfor %}
    </rows>
</table>
```

Create `index.xml.peb`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<index name="{{ title ? title : name }}">
    {% if description %}<description>{{ description }}</description>{% endif %}
    {% for item in contents %}
    <item path="{{ item.path }}">{{ item.title }}</item>
    {% endfor %}
</index>
```

**Usage:**

Specify the custom format when running the reporter:

**Maven:**
```xml
<configuration>
  <format>xml</format>
  <templateDirectory>${project.basedir}/templates</templateDirectory>
</configuration>
```

**Gradle:**
```kotlin
tableTestReporter {
  format.set("xml")
  templateDir.set(file("templates"))
}
```

**CLI:**
```bash
java -jar tabletest-reporter-cli.jar \
  --template-dir templates \
  -f xml \
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
  --index-depth 2 \  # levels in index (1, 2, ..., or 'infinite')
  --config tabletest-reporter.yaml  # spec metadata + publish selection (default: ./tabletest-reporter.yaml)
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

