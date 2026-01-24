# TableTest Reporter Features

This document describes the end-user features provided by TableTest Reporter.

**Key features:**
- **Docs from tests** – Turn TableTest tables into publishable AsciiDoc or Markdown pages.
- **Automatic capture** – JUnit extension records table data and results during test runs.
- **Multiple delivery options** – CLI, Maven plugin, or Gradle plugin.
- **Customizable output** – Extend or replace templates; add new formats beyond the built-ins.
- **Readable structure** – Output mirrors package/class/method structure with index pages.
- **Visual test signals** – Scenario/expectation roles and pass/fail markers for styling.

## Output Formats

**Built-in formats:**
- AsciiDoc (`.adoc`)
- Markdown (`.md`)

**Custom formats:**
Provide `table.{format}.peb` and `index.{format}.peb` templates to add new formats
such as HTML, XML, or JSON. The format name becomes the file extension.

## Test Metadata Included

TableTest Reporter captures and publishes:
- Display names for classes and methods (`@DisplayName`)
- Descriptions for classes and methods (`@Description`)
- Table headers, rows, and column roles
- Per-row pass/fail results and failure messages

## Output Structure

Generated documentation mirrors your test package structure while removing
redundant directory levels. Each package and class gets an index page, and
each test method gets its own page.

## Styling and Visual Indicators

Generated documents include roles for styling:
- `.scenario` - Scenario column cells
- `.expectation` - Expectation column cells
- `.passed` - Rows that passed
- `.failed` - Rows that failed

These roles become CSS classes when AsciiDoc is rendered to HTML.

## Integration Options

Use the reporter where it fits best:
- **CLI** – Run manually or in CI/CD.
- **Maven plugin** – Generate reports in Maven builds.
- **Gradle plugin** – Generate reports in Gradle builds.

## Configuration

Configure expectation column detection with:
- `tabletest.reporter.expectation.pattern` (default: `.*\\?$`)

## Platform Support

Runs anywhere Java 21+ is available on Linux, macOS, and Windows.
