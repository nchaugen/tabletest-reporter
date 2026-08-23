# TableTest Reporter

TableTest Reporter generates documentation from your [TableTest](https://github.com/nchaugen/tabletest) tests. It turns your test tables into a readable specification you can publish beside your project docs, as a self-contained HTML site or as AsciiDoc or Markdown for a site generator you already run.

## Contents

- [Quick Start](#quick-start)
- [Requirements](#requirements)
- [Getting Started](#getting-started)
  - [Add the reporter plugin](#add-the-reporter-plugin)
  - [Write your tests](#write-your-tests)
  - [Run your tests](#run-your-tests)
  - [Generate the documentation](#generate-the-documentation)
- [Configuring the Report](#configuring-the-report)
  - [The sidecar file (`tabletest-reporter.yaml`)](#the-sidecar-file-tabletest-reporteryaml)
  - [Build options](#build-options)
  - [Test-side options (JUnit configuration parameters)](#test-side-options-junit-configuration-parameters)
- [Formats](#formats)
  - [Choosing a format](#choosing-a-format)
  - [The built-in HTML format](#the-built-in-html-format)
  - [Listing available formats](#listing-available-formats)
  - [Custom output formats](#custom-output-formats)
- [Output Structure](#output-structure)
  - [How names become filenames](#how-names-become-filenames)
- [Publishing Your Documentation](#publishing-your-documentation)
  - [GitHub Pages via Actions](#github-pages-via-actions)
  - [Other hosting options](#other-hosting-options)
  - [Publishing into an existing site](#publishing-into-an-existing-site)
- [Custom Templates](#custom-templates)
  - [Convention-based discovery](#convention-based-discovery)
  - [Configuring a custom template directory](#configuring-a-custom-template-directory)
  - [Template extension example](#template-extension-example)
  - [Recording when the report was generated](#recording-when-the-report-was-generated)
  - [Template replacement example](#template-replacement-example)
  - [The template context](#the-template-context)
  - [What an extension template can reach](#what-an-extension-template-can-reach)
- [Styling HTML Reports](#styling-html-reports)
- [For Plugin Developers](#for-plugin-developers)

## Quick Start

1. [Add the reporter plugin](#add-the-reporter-plugin) to your build
2. [Write your tests](#write-your-tests) as TableTest tables
3. [Run your tests](#run-your-tests), which write the YAML files
4. [Generate the documentation](#generate-the-documentation) from that output

The Gradle plugin handles every part of this. A Maven build declares the reporter dependency and
turns on JUnit extension autodetection itself, which the Maven setup below shows.

## Requirements

- Java 17+ to run your tests (the JUnit extension targets 17)
- Java 21+ to generate the report (CLI, Maven plugin, or Gradle plugin — this is your build
  JVM, which need not match the Java version your project targets)
- JUnit 5.12+ or JUnit 6
- [TableTest](https://github.com/nchaugen/tabletest) 1.0.0+ for your tests
- Gradle 8.14+ if you use the Gradle plugin

Popular frameworks like Spring Boot (3.5.0+) and Quarkus (3.21.2+) include compatible JUnit versions.

The compatibility test suite exercises these versions on every change. It covers JUnit 5.12.0 and
6.1.2, on Maven and on Gradle. It also covers Spring Boot 3.5.0 and current, and Quarkus 3.21.2
and current.

## Getting Started

### Add the reporter plugin

#### Gradle setup

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

#### Maven setup

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

#### Manual setup (advanced)

You may want manual control over the JUnit extension setup, for the CLI runner or for a
configuration of your own. This applies to Maven, to Gradle and to the CLI alike.

<details>
<summary><b>Add the dependency and configure autodetection yourself</b></summary>

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

### Write your tests

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

**Your table needs a scenario column** for the pass and fail indicators, which are the `.passed`
and `.failed` CSS roles. Give it one of two ways: as a leading column the test does not declare,
or with the `@Scenario` annotation on the parameter you choose.

```java
@TableTest("""
    Scenario | Username | Password | Expected?
    Success  | admin    | secret   | true      ← .passed role applied
    Failure  | guest    | wrong    | false     ← .passed/.failed role applied
    """)
```

Without a scenario column, the reporter cannot apply the pass and fail roles reliably, because
parameter type conversion gets in the way. A table with no scenario column still generates
documentation. Its rows carry no pass or fail indicator:

```java
@TableTest("""
    Username | Password | Expected?
    admin    | secret   | true      ← No .passed/.failed roles
    guest    | wrong    | false     ← No .passed/.failed roles
    """)
```

The scenario column can have any name (`Scenario`, `Test Case`, `Description`, etc.) and contain any unique string value to identify each test case.

#### Column roles

Every published cell carries the roles of its column. The reporter derives four itself — `scenario`,
`expectation`, `passed` and `failed` — and a test can declare more.

**A column of source text: `@Lines`.** A table keeps every row on one line, so you write a
multi-line value as a list of lines. Mark the column with `@Lines`. The parameter then takes the
lines joined by newlines. The HTML report draws the cell as a stacked
monospace block, and not as a bulleted list:

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

Declare the parameter as a `List<String>` instead, and it takes the lines themselves. The published
cell does not change either way. The reporter publishes the value the row ran with, so the cell is
still the list of lines you wrote.

**Several named blocks: `@NamedLines`.** One cell can hold more than one block of text, each under
a name. Write it as a map from name to lines. A file and its contents is the case it serves, so the cell reads as a small directory. The HTML report draws each name as a caption over its own
block:

```java
@TableTest("""
    Scenario                   | Your template directory                                       | Table page?
    The name of the table page | [table.md.peb: ['# {{ title }} of note', 'Written by hand.']] | ['# Leap years of note', 'Written by hand.']
    Two names that both match  | [b-table.md.peb: ['# From B'], a-table.md.peb: ['# From A']]  | ['# From A']
    """)
void rendersWithYourTemplate(
        @NamedLines Map<String, List<String>> yourTemplateDirectory, @Lines List<String> tablePage) { ... }
```

The parameter is a plain `Map<String, List<String>>`, and there is no converter. Note that **a map
key is never converted**. Declaring `Map<Path, …>` therefore compiles, and then fails at the first
read. Resolve the name yourself where you write the files out.

**Numbered lines: `@Numbered`.** Numbering is a role of its own, and not part of the two above. Ask
for it beside either: `@Lines @Numbered` or `@NamedLines @Numbered`. It earns its place on a block
long enough that a reader needs to point at a line. On two or three lines the digits are as wide as
the text beside them, which is why it is off unless you ask.

**A role of your own.** Annotate an annotation with `@ColumnRole` and put it on a parameter:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@ColumnRole
public @interface Ingredient {}
```

The reporter publishes the role as the annotation's simple name in kebab case, so `@SourceLines`
publishes `source-lines`. Name the token yourself with `@ColumnRole("...")` instead.

The role reaches the HTML report as a CSS class on the cell, and the AsciiDoc report as an element
role. A stylesheet of your own can therefore style the column. Markdown carries no roles.

A role must be lower-case words joined by single hyphens. A role that names one the reporter derives
publishes anyway: the column takes that role's styling, and the reporter never treats it as one.

### Run your tests

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

### Generate the documentation

Run the reporter to generate the documentation. It finds where your test framework writes its YAML
files. Most standard Maven and Gradle
projects therefore need no input directory at all. Run the plugin. See [Input Directory Resolution](#input-directory-resolution) for details on how detection works and when manual configuration is needed.

#### Generating with Maven

Where you configured the `report` goal in your plugin executions, as [Maven setup](#maven-setup)
shows, the build
generates the documentation itself. Otherwise, run it by hand:

```bash
mvn tabletest-reporter:report
```

The plugin writes the documentation to `target/generated-docs/tabletest/`.

**Configuration options:**
```xml
<configuration>
  <format>asciidoc</format>  <!-- or 'markdown', 'html' -->
  <inputDirectory>${project.build.directory}/junit-jupiter</inputDirectory>
  <outputDirectory>${project.build.directory}/generated-docs/tabletest</outputDirectory>
  <indexDepth>infinite</indexDepth>  <!-- levels in index (1, 2, ..., or 'infinite') -->
  <generatedAt>2026-07-20T14:32:09Z</generatedAt>  <!-- pin the report's timestamp, see below -->
  <configFile>${project.basedir}/tabletest-reporter.yaml</configFile>  <!-- the sidecar file, see Configuring the Report -->
  <inputDirectories>  <!-- several modules merged into one spec, see Build options -->
    <dir>${project.build.directory}/junit-jupiter</dir>
    <dir>${project.basedir}/../other-module/target/junit-jupiter</dir>
  </inputDirectories>
</configuration>
```

Or use command-line properties:
```bash
mvn tabletest-reporter:report -Dtabletest.report.format=markdown
```

#### Generating with Gradle

Run the task:
```bash
./gradlew reportTableTests
```

The task writes the documentation to `build/generated-docs/tabletest/`.

**Configuration options:**
```kotlin
tableTestReporter {
  format.set("markdown")  // default: "asciidoc"
  inputDir.set(layout.buildDirectory.dir("junit-jupiter"))
  outputDir.set(layout.buildDirectory.dir("generated-docs/tabletest"))
  indexDepth.set("infinite")  // levels in index (1, 2, ..., or "infinite")
  generatedAt.set("2026-07-20T14:32:09Z")  // pin the report's timestamp, see below
  configFile.set(layout.projectDirectory.file("tabletest-reporter.yaml"))  // the sidecar file, see Configuring the Report
  inputDirs.from(layout.buildDirectory.dir("junit-jupiter"))  // several modules merged into one spec, see below
}
```

## Configuring the Report

Four of the report's settings live together in one optional sidecar file. The rest are options of
the build that runs the reporter.

### The sidecar file (`tabletest-reporter.yaml`)

Put `tabletest-reporter.yaml` in the project directory, or point at it with Maven `<configFile>`,
Gradle `configFile`, or the CLI `--config`. It holds four independent sections, and every one of
them is optional. The reporter reads the file when it generates the report, so changing any of
them needs no new test run. A project with no such file reports exactly as it did before.

#### Spec metadata

Without a sidecar file, the root index of a spec takes its title from the deepest common package
segment, such as "junit" or "example". An intermediate index page shows a lowercase package name.

Drop an optional `tabletest-reporter.yaml` in the project directory. It gives the spec a real
title and intro, retitles an intermediate page, and sets an explicit reading order for the
features:

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

Everything is optional. A declared feature renders first, in the order you gave it. An undeclared
sibling follows, alphabetically.

The reporter draws a `description` under the feature's title on its index page, the way it draws a
test class's `@Description`. Use it for what the whole group has in common. An individual rule
then does not repeat it. A feature that matches no page is logged and skipped, so a
typo never fails the report.

#### Front matter for a site generator (`frontMatter`)

A site generator reads an AsciiDoc or Markdown report. It decides how each page looks, and where
that page sits in the site. Front matter is what you tell the generator. Declare it once:

```yaml
frontMatter:
  layout: report          # any key you like, written as declared
  type: docs
  title: $title           # filled by the reporter: the page's own title
  weight: $position       # filled by the reporter: its place in the reading order
  generated: $timestamp   # filled by the reporter: when the run happened
```

Markdown pages open with a fenced block and AsciiDoc pages with document attributes:

```markdown
---
layout: report
title: Leap years
weight: 2
generated: "2026-08-23T09:19:33Z"
---
```

```asciidoc
:layout: report
:title: Leap years
:weight: 2
:generated: 2026-08-23T09:19:33Z
```

**An HTML page carries none.** It is a finished page, and not source for a generator.

**Write the token as the value, never as the key.** Generators do not agree on what to call a
page's position. Hugo calls it `weight`, Docusaurus `sidebar_position`, a Jekyll theme `nav_order`,
and Antora `page-weight`. Antora exposes a custom attribute under no other prefix. Name the key
whatever your generator reads:

```yaml
frontMatter:
  sidebar_position: $position    # Docusaurus
  page-weight: $position         # Antora
  nav_order: $position           # Jekyll
```

There are three tokens: `$title`, `$position` and `$timestamp`. The reporter writes everything else as you declared it. A value that only looks like a token stays as it stands, and the reporter warns. A typo therefore never fails a report. Write `$$` for a literal value that begins with a dollar sign.

A derived value that does not apply to a page leaves its key out, rather than writing it empty. A
position on the root index is such a value, because the root has no siblings. Keys keep the order
you declared them in. A value is quoted only where YAML would otherwise read it back as something
else.

**`$position` is the one worth knowing about.** The `features:` section above declares the reading
order of your spec. A site generator sorts the pages alphabetically without it, and loses your curation at the boundary. With it, the published site reads in the order you chose.

The `frontMatter` template block still works and takes precedence — see
[Custom Templates](#custom-templates) for when you need more than keys and values.

#### Linking back to your site (`site`)

Every link inside a generated report is relative within its own tree. A reader who reaches
the spec from your site therefore has no way back to it. Add a `site` section to the same
`tabletest-reporter.yaml` to put one link in the footer of every HTML page:

```yaml
site:
  label: "TableTest"                 # the link text; defaults to the address itself
  url: "https://tabletest.org/"      # used exactly as written
```

The reporter never resolves the address against the report's own tree. A root-relative `/` or
`/docs/` therefore works for a site that hosts the spec below it, and an absolute URL works from
anywhere. Without a `site` section the footer holds the attribution alone, as before.

Each of the three HTML page templates leaves the footer as a `footer` block, and the link has a
`siteLink(site)` macro of its own. A template of yours can override the block, or call the macro
to place the same link elsewhere. See [Custom Templates](#custom-templates).

#### Selecting what publishes (`publish`)

By default every table that ran publishes. Add a `publish` section to the same
`tabletest-reporter.yaml` to hold pages back. It applies at
report time, so changing what publishes needs no new test run. No test-framework tag is involved:

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

### Build options

These are options of the build that runs the reporter, and not of the sidecar file.

#### Pinning the report's timestamp (`generatedAt`)

Every page states when the report was generated. The reporter reads the clock, so two runs of
the same tests write two different pages. A build that compares its own output then sees a change
every time. Pin the instant, and the same tests give you the same bytes:

```bash
mvn tabletest-reporter:report -Dtabletest.report.generatedAt=2026-07-20T14:32:09Z
./gradlew reportTableTests                      # generatedAt.set("2026-07-20T14:32:09Z")
tabletest-reporter --generated-at 2026-07-20T14:32:09Z -i target/junit-jupiter
```

The value is an ISO-8601 instant. Any other value fails the build, and never falls back to the
clock. The reporter does not read `SOURCE_DATE_EPOCH` itself, because a report that changes with
its environment is the fault being fixed. A reproducible build passes its own value in:

```bash
mvn tabletest-reporter:report \
  -Dtabletest.report.generatedAt="$(date -u -d "@$SOURCE_DATE_EPOCH" +%Y-%m-%dT%H:%M:%SZ)"
```

A report states the timestamp in UTC, whatever zone the build ran in. The label a reader sees
drops the sub-second precision.

#### Multi-module builds (one spec from several modules)

A single spec can span the modules of a multi-module build. The report tree comes from the
test class names inside the YAML, not from where the files sit, so modules merge into one
package hierarchy. The reporter skips a listed directory that does not exist, and warns. A partial build therefore still publishes what it has.

**Maven** — let the plugin walk the reactor:
```bash
mvn tabletest-reporter:aggregate
```
The `aggregate` goal runs on the aggregator project and writes one report to its output directory.
It finds each module's TableTest output the way the `report` goal finds its own: the JUnit output
directory a module configures, or that module's `target/junit-jupiter`.

Name the directories yourself instead, which is the route to take where the goal cannot run inside
the reactor. Use `<inputDirectories>` on the `report` goal, as the
[Maven configuration options](#generating-with-maven) show, or:
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

#### Input directory resolution

When you run the reporter, it needs to find the YAML files generated during your test run. In most cases, this is handled automatically.

**Resolution order:**

1. **Explicit configuration** — name an input directory, through plugin config or the CLI `-i`
option, and the reporter uses it directly
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

### Test-side options (JUnit configuration parameters)

Configure TableTest Reporter through [JUnit Platform configuration
parameters](https://docs.junit.org/current/running-tests/configuration-parameters.html).

**`tabletest.reporter.expectation.pattern`**

Defines a regular expression pattern to identify expectation columns in your test tables. By default, the reporter treats a column ending with `?` as an expectation.

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

## Formats

### Choosing a format

The three built-in formats are not interchangeable, and picking one is picking a publishing
route. One rule decides which features a format gets. A feature that changes the **data** a page carries
reaches every format that can express it. A feature that changes how a page **looks** reaches HTML
alone.

| Format | What it is for | What it carries |
|---|---|---|
| `html` | the publishable destination — put it on a web server as it stands | self-contained pages, inline CSS and JavaScript, sidebar, search, breadcrumbs, footer, whitespace markers, every column role |
| `asciidoc` | source for an Asciidoctor or Antora pipeline | the table, the titles, the descriptions, and roles (`[.lines]`, `[.value-set]`) for the downstream renderer to style |
| `markdown` | interchange that renders anywhere — GitHub, Docusaurus, MkDocs | the table, the titles, the descriptions; plain, with no roles |

**Neither text format carries breadcrumbs, navigation or a footer.** That is deliberate rather
than missing. A site generator builds those from its own page tree, and it decides where your spec is nested. A
trail the reporter wrote would therefore name different ancestors than the site's, and both would
render. You may want a fact the generator cannot know, such as when the report was generated. Put it in
the `frontMatter` block of an extension template — see [Custom Templates](#custom-templates).

**The default is `asciidoc`** where you name no format, in every entry point. If you intend to
publish the result directly, set `html` explicitly.

### The built-in HTML format

The `html` format renders self-contained living documentation, and needs no Asciidoctor step.
Each page is a standalone `.html` file, with its CSS and JavaScript inside it and no external
reference. The output tree therefore works directly on any static host, GitHub Pages included.

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

Every entry carries a status dot. The page also states its scenario pass rate, rolled up from the
tables beneath it: "N of M scenarios broken", or "All N scenarios hold".

The tree writes out every page below it, and folds all but the top level away. A spec of forty
rules therefore opens on a list of its features, and not on every rule at once. A folded entry
stays in the page, so a browser search finds it, and a printed copy shows every level open.

Every page also carries a breadcrumb trail of the pages above it: root package, then class, then
table. A menu button opens a navigation drawer, holding the whole-report tree with status dots
throughout. The branch that holds the current page arrives open and marked. You can jump
anywhere from any page. The drawer slides in over the content, so a wide table keeps the full
page width.

The drawer also holds a search box, and that box searches the whole report: every page's title,
description, headers and cell values. It lists the matching pages, with their status dots, to
jump to. One `tabletest-search-index.js` backs the search, written once to the output root and
linked from each page by a relative prefix. Search therefore works offline, over `file://`, and
under any subpath, and makes no external request.

Every link and asset reference is relative, so the generated tree deploys unchanged under a
project subpath. GitHub *project* Pages, served from `/<repo>/`, is such a subpath. The
`tabletest-search-index.js` asset sits at the output root, beside the root `index.html`.

#### Reading a report from the keyboard

Tab reaches every row of both trees, Enter follows one, and Space opens or closes a feature.
The browser does all of that, and no script runs. These keys are new on top of it:

| Key | Does |
|---|---|
| <kbd>↓</kbd> <kbd>↑</kbd> | move between the rows you can see, in the drawer or an index page's own tree |
| <kbd>→</kbd> | open a feature, then step into it |
| <kbd>←</kbd> | close a feature, or step out to the one above |
| <kbd>Home</kbd> <kbd>End</kbd> | the first or last row you can see |
| <kbd>/</kbd> | open the drawer and jump to the search box |
| <kbd>m</kbd> | open or close the drawer |
| <kbd>Esc</kbd> | leave the search box, or close the drawer |
| <kbd>?</kbd> | list these keys on the page |

The arrow keys act only on a row that already holds focus. They still scroll a long rule page
everywhere else. A closed drawer is `inert`, so it stays out of the tab order until you open it.
An open drawer holds focus until you leave it.

Note for Safari readers. Safari does not move focus to a link with Tab. Turn that on in
Settings → Advanced → "Press Tab to highlight each item on a webpage". Hold <kbd>Option</kbd>
while you press Tab to reach a link without changing the setting. The arrow keys above work
either way.


#### Single-file mode

Add `--single-file` (`-s`) to assemble the whole report into **one** self-contained
`.html` instead of a directory tree:

```bash
tabletest-reporter -f html --single-file -i target/junit-jupiter -o target/generated-docs/tabletest
```

The file inlines every table as an anchored section, and embeds the search index. The sidebar and
the search jump to anchors in the page. The one `index.html` therefore has no sibling asset, and
no external reference at all. This is the most portable form: attach it
to a release, email or ticket where a directory of files is awkward. The multi-file tree
remains the default (better for GitHub Pages and per-page linking). Single-file mode
currently applies to the `html` format only.

To customise the markup, drop your own `table.html.peb` / `index.html.peb` into a template
directory — an exact filename match overrides the built-in template, as
[Custom Templates](#custom-templates) describes.

### Listing available formats

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

### Custom output formats

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

The reporter reduces a `@DisplayName`, a Kotlin backtick name, or a camelCase method name to one
lowercase name. That name serves as a filename and as a URL segment. The reporter splits words on spaces, on underscores, and on camelCase boundaries. Every other run of punctuation or symbols becomes one hyphen.

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

A name written in its own script publishes as it stands. Such names are legal filenames on every
supported platform, and legal URLs once a browser percent-encodes them. GitHub Pages serves UTF-8
paths. A name with no letter and no digit falls back to `unnamed-` plus a stable hash of the name. Two
such names therefore still get two pages.

Two tables in the same class that reduce to the same filename are disambiguated with a numeric suffix (`-1`, `-2`).

## Publishing Your Documentation

The `html` format publishes as it stands. Every page is self-contained, and every link is
relative. Copy the output directory to a static host, with no build step. The
AsciiDoc and Markdown formats are intermediate sources for a toolchain you already run:

- **HTML:** Copy the output directory to any static host — no conversion step
- **AsciiDoc:** Use Asciidoctor Maven/Gradle plugins to convert to HTML
- **Markdown:** Use your static site generator (Jekyll, Hugo, MkDocs, etc.)

### GitHub Pages via Actions

Living documentation is only worth publishing if it keeps up with the code, which means
generating it in CI rather than committing it. The workflow below runs the tests, generates
the HTML report, and deploys it to Pages. The reporter builds the report from a test run, so the `test` phase must run in the same invocation as the `report` goal.

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

GitHub serves Project Pages from `/<repo>/`, which the report handles with no configuration —
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

### Publishing into an existing site

Everything above hosts the report as a site of its own. The other route is to generate a text
format straight into a site you already run, and let its generator lay the pages out. Which route
you are on decides which options you need:

| Route | Format | What you configure |
|---|---|---|
| The report **is** the site, or sits beside one | `html` | `site:` — the one link back out of an otherwise self-contained tree |
| The report is **content for** your site | `markdown` or `asciidoc` | `frontMatter:` — what the generator needs to place and title each page |

For the second route, point the output at your content directory and declare the front matter your
generator reads:

```xml
<configuration>
  <format>markdown</format>
  <outputDirectory>${project.basedir}/../site/content/spec</outputDirectory>
  <configFile>${project.basedir}/tabletest-reporter.yaml</configFile>
</configuration>
```

```yaml
# Hugo
frontMatter:
  title: $title
  weight: $position
  generated: $timestamp

# Docusaurus
frontMatter:
  title: $title
  sidebar_position: $position

# Antora — a custom attribute reaches the UI model only under the page- prefix
frontMatter:
  page-title: $title
  page-weight: $position
```

`$position` is what carries the reading order declared in `features:` across the boundary. Without it a generator sorts your pages alphabetically, and loses your curation.

**Do not use `site:` on this route.** Your generator already draws the navigation. A second trail
beside its own would disagree about where the page sits, and the site decides that, not the
report. For the same reason neither text format carries breadcrumbs or a footer of its own; see
[Choosing a Format](#choosing-a-format).

---

## Custom Templates

TableTest Reporter uses [Pebble templates](https://pebbletemplates.io/) to generate documentation. You can customise the output by providing your own templates.

**Two approaches:**

1. **Template Extension** - Override specific parts (e.g., add front matter for Jekyll/Hugo)
2. **Template Replacement** - Completely replace the built-in templates

### Convention-based discovery

Custom templates are discovered automatically by naming convention:

- `*-table.adoc.peb` or `*-table.md.peb` - Custom table templates
- `*-index.adoc.peb` or `*-index.md.peb` - Custom index templates

The reporter picks up `custom-table.adoc.peb` or `jekyll-table.md.peb` by itself, for example.

**Precedence:**
1. Exact match (e.g., `table.adoc.peb`) - complete replacement
2. Pattern match (e.g., `custom-table.adoc.peb`) - extension template
3. Built-in template - default

### Configuring a custom template directory

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

### Template extension example

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

### Recording when the report was generated

Most projects want [the `frontMatter` config section](#front-matter-for-a-site-generator-frontmatter)
instead — it needs no template at all. Write the block yourself when you need more than keys and
values, such as a value composed from several context keys:

```pebble
{% extends "table.md.peb" %}
{% block frontMatter %}---
title: "{{ title }}"
generated: "{{ generatedAt.datetime }}"
---

{% endblock %}
```

**Quote the value.** Pebble trims the newline straight after a `}}` expression, so a line ending
in one joins the line below it. An unquoted `generated: {{ generatedAt.datetime }}` therefore puts
the closing `---` on the value line, and the front matter stops parsing. Any character after the braces
prevents it, and quotes are valid YAML for a timestamp.

For AsciiDoc the same block carries document attributes instead:

```pebble
{% extends "table.adoc.peb" %}
{% block frontMatter %}:generated: {{ generatedAt.datetime }}
{% endblock %}
```

### Template replacement example

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

Generated on {{ generatedAt.label }}
```

### The template context

Every key below is available to a replacement template, and to a block you override in an
extension template.

**On every page, in every format:**

| Key | Holds |
|---|---|
| `title` | the page's display name |
| `description` | the page's description, where it has one |
| `name` | the page's own name, as it appears in the URL |
| `breadcrumbs` | the ancestor trail, each entry `label`, `href`, `current`; the page itself is last and has no `href` |
| `nav` | the whole report: `nav.home` (`label`, `href`, `current`) and `nav.tree` of nested entries |
| `assetRoot` | the relative prefix from this page back to the report root, e.g. `../../` |
| `generatedAt` | `datetime` (ISO 8601) and `label` (readable) for the run, or null |
| `site` | `label` and `url` of the hosting site, or null when no `site` is declared |

**A table page adds:**

| Key | Holds |
|---|---|
| `headers` | header cells, each with `value` and `roles` |
| `rows` | rows of cells, each with `value` and `roles` |
| `rowResults` | one entry per scenario, with `displayName`, `passed` and `errorMessage` |
| `featureDescription` | the description of the page this rule sits under, where it has one |

**An index page adds:**

| Key | Holds |
|---|---|
| `contents` | child pages, nested: `name`, `title`, `path`, `type` (`index` or `table`), `status`, and `contents` for its own children |
| `status` | the rollup below this page: `state` (`passed`, `failed` or `neutral`), `total`, `passed`, `broken` |

**A single-file report** has `title`, `description`, `nav`, `assetRoot`, `generatedAt`, `site`,
`searchData`, and `sections` — one entry per page, with `anchor`, `title`, `type`, `status`,
`level`, `description`, and `headers` / `rows` / `rowResults` for a table. It has **no**
`breadcrumbs`.

### What an extension template can reach

A template that extends a built-in one reads every context key above, inside the block it
overrides. It can also import the built-in macros and call them:

```pebble
{% extends "index.html.peb" %}
{% import "macros.html.peb" %}
{% block footer %}
  <p>Reviewed quarterly.</p>
  {{ docFooter(generatedAt, site) }}
{% endblock %}
```

Two things to know about the blocks themselves:

- **The three text-format blocks are not in the HTML templates.** The AsciiDoc and Markdown
  templates leave `frontMatter`, `title`, `description`, `table`, `failures` and `contents`. The
  HTML templates leave `extra_stylesheet` and `footer`.
- **A block must sit at the top level of the page template to be overridable.** A block written
inside a macro renders its default and ignores the override, without a word. A macro reaches the
page through `{% import %}`, which is not inheritance.

## Styling HTML Reports

When generating HTML from AsciiDoc reports, you can apply custom CSS styling based on the roles generated by TableTest Reporter.

**Understanding CSS Class Placement**

TableTest Reporter generates AsciiDoc with custom roles (`.scenario`, `.expectation`, `.passed`, `.failed`) that become CSS classes in HTML output. The reporter puts these classes on **an inline element inside a table cell**, such as a `span`, a `ul` or an `ol`. It does not put them on the `th` or the `td`.

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

**Note:** use `:has(.classname)` and name no element type. A role can land on a different element,
depending on what the cell holds.

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

## For Plugin Developers

**CLI Usage:**

Use the CLI on its own where you are building tooling of your own:

```bash
java -jar tabletest-reporter-cli.jar \
  -f markdown \
  -i target/junit-jupiter \
  -o target/generated-docs/tabletest \
  --index-depth 2 \  # levels in index (1, 2, ..., or 'infinite')
  --generated-at 2026-07-20T14:32:09Z \  # pin the report's timestamp (default: the clock)
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

