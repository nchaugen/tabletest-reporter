# TableTest Reporter Features

This document describes the end-user features provided by TableTest Reporter.

**Key features:**
- **Docs from tests** – Turn TableTest tables into publishable HTML, AsciiDoc, or Markdown pages.
- **Automatic capture** – JUnit extension records table data and results during test runs.
- **Ready to publish** – The built-in HTML format is self-contained and deploys as-is to any
  static host, with no Asciidoctor or site-generator step.
- **Multiple delivery options** – CLI, Maven plugin, or Gradle plugin.
- **One spec from many modules** – Several modules of a build merge into a single report.
- **Curated by configuration** – An optional `tabletest-reporter.yaml` sets the spec's title,
  intro, reading order, and which pages publish — without changing how tests are tagged or run.
- **Customizable output** – Extend or replace templates; add new formats beyond the built-ins.
- **Readable structure** – Output mirrors package/class/method structure with index pages.
- **Visual test signals** – Scenario/expectation roles and pass/fail markers for styling.

## Output Formats

**Built-in formats:**
- HTML (`.html`) – self-contained living documentation, ready to publish
- AsciiDoc (`.adoc`)
- Markdown (`.md`)

**Custom formats:**
Provide `table.{format}.peb` and `index.{format}.peb` templates to add new formats
such as XML or JSON. The format name becomes the file extension. Dropping in a
`table.html.peb` / `index.html.peb` overrides the built-in HTML templates.

## The HTML Report

The `html` format produces standalone pages with inline CSS and JavaScript and no external
references, so the output tree works offline (`file://`) and on any static host. Every link
and asset reference is relative, so the tree also deploys unchanged under a project subpath.

**On each table page:**
- autowidth tables in a horizontal-scroll wrapper, with a sticky header row and first column
- nested collections rendered structurally (lists, sets, and maps with distinct markers)
- a pass/fail badge, per-row and per-cell status colouring, and collapsible failure details
- scenario- and expectation-column emphasis, a roles legend, a row filter, and a
  "failing only" toggle
- IDE-style markers for whitespace-significant values — a dot per significant space, an arrow
  per tab — so indentation and space counts are readable without altering the value itself

**Across the report:**
- index pages showing a link tree with a status dot on every entry, and a scenario pass-rate
  summary rolled up from the tables beneath ("N of M scenarios broken" / "All passing")
- a breadcrumb trail on every page, and a navigation drawer with the whole-report tree
- whole-report search over every page's title, description, headers, and cell values
- a light/dark theme toggle and a print stylesheet
- a footer stating when the report was generated, in UTC, so readers can tell whether the
  documentation still tracks the code it came from

**Single-file mode** (`--single-file`) assembles the whole report into one self-contained
`.html` file with navigation and search embedded — the most portable form, for attaching to
a release or a ticket. HTML format only.

## Test Metadata Included

TableTest Reporter captures and publishes:
- Display names for classes and methods (`@DisplayName`)
- Descriptions for classes and methods (`@Description`)
- Table headers, rows, and column roles
- Per-row pass/fail results and failure messages

Published cells hold the values as the test run saw them, so a report reflects one
particular run of the suite.

## Output Structure

Generated documentation mirrors your test package structure while removing
redundant directory levels. Each package and class gets an index page, and
each test method gets its own page.

`indexDepth` controls how many levels of the tree each index page lists (default: all of
them). Note that it counts tree levels, so introducing a grouping package shifts everything
below it one level deeper.

## Multi-Module Reports

Several directories of TableTest output merge into a single spec, so the modules of a
multi-module build publish one set of documentation:

- **Maven** – an `aggregate` goal walks the reactor and finds each module's output by itself,
  plus `<inputDirectories>` on the `report` goal to name them explicitly
- **Gradle** – `inputDirs`
- **CLI** – a repeated `-i`/`--input`

The report tree comes from the test class names, so modules land in one package hierarchy. A
listed directory that does not exist is skipped with a warning, so a partial build still
publishes what it has.

## Curating the Spec

An optional `tabletest-reporter.yaml` in the project directory shapes the report at
generation time. A project without one is unaffected.

- **Spec metadata** – give the whole spec a title and intro paragraph on its root index,
  retitle intermediate index pages, and set an explicit reading order for features (declared
  features lead, undeclared siblings follow alphabetically).
- **Publish selection** – a `publish` section decides which pages the report holds: `exclude`
  paths hold a page and its subtree back, `include` paths re-admit one below an excluded page.
  Paths name pages as the report's URLs do (`converting/convert-with`), with `*` for any part
  of a page name and `**` for any number of levels.

Because selection happens when the report is generated, what publishes is decoupled from how
the suite was tagged or run — re-curating a spec needs no new test run.

Point elsewhere with Maven `<configFile>` / `-Dtabletest.report.configFile`, Gradle
`configFile`, or the CLI `--config` / `-c`.

## Styling and Visual Indicators

Generated documents include roles for styling:
- `.scenario` - Scenario column cells
- `.expectation` - Expectation column cells
- `.passed` - Rows that passed
- `.failed` - Rows that failed

These roles become CSS classes when AsciiDoc is rendered to HTML. The built-in HTML format
applies them itself.

## Integration Options

Use the reporter where it fits best:
- **CLI** – Run manually or in CI/CD.
- **Maven plugin** – Generate reports in Maven builds.
- **Gradle plugin** – Generate reports in Gradle builds.

## Configuration

Configure expectation column detection with:
- `tabletest.reporter.expectation.pattern` (default: `.*\\?$`)

## Platform Support

Runs on Linux, macOS, and Windows. The JUnit extension that captures table data runs
inside your test JVM and targets **Java 17**, so a Java 17 project can document its tests on
its own runtime. Generating the report (CLI, Maven plugin, Gradle plugin) needs **Java 21+** —
that is your build JVM, which need not match the version your project targets.
