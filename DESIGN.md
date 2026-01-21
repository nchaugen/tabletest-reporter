# TableTest Reporter - Design Documentation

This document describes the design and architecture of the TableTest Reporter system.

## Overview

TableTest Reporter is a JUnit extension that captures test execution results from `@TableTest` annotated methods and generates documentation (AsciiDoc, Markdown or custom format) from those results. The system operates in two phases:

1. **Collection Phase**: During test execution, capture table definitions and test results as YAML files
2. **Reporting Phase**: Process YAML files to generate formatted documentation with hierarchical indexes

## System Architecture

### Key Modules

- **tabletest-reporter-junit**: JUnit extension for test execution interception and YAML generation
- **tabletest-reporter-core**: Report generation engine (template processing, file writing)
- **tabletest-reporter-cli**: Command-line interface for report generation
- **tabletest-reporter-maven-plugin**: Maven plugin wrapper
- **tabletest-reporter-gradle-plugin**: Gradle plugin wrapper

### Data Flow

```
Test Execution (JUnit)
    ↓
TableTestPublisher Extension (test interception)
    ↓
YAML Files (intermediate format)
    ↓
ReportTree (discover and build hierarchy)
    ↓
TableTestReporter (template rendering)
    ↓
Documentation Files (AsciiDoc/Markdown/Custom)
```

## Collection Phase (tabletest-reporter-junit)

### Extension Discovery

**Auto-detection mechanism**: The `TableTestPublisher` class is discovered automatically by JUnit via the ServiceLoader mechanism:

- Registered in: `META-INF/services/org.junit.jupiter.api.extension.Extension`
- Activated when: `junit.jupiter.extensions.autodetection.enabled=true`
- No explicit `@ExtendWith` annotation needed on test classes

### Test Interception

**TableTestPublisher** implements two JUnit interfaces:

- **TestWatcher**: Intercepts individual test invocation results (pass/fail)
- **AfterAllCallback**: Triggers publication after all tests in a class complete

**Collection workflow**:

1. For each test invocation: `testSuccessful()` or `testFailed()` called
2. Check if method has `@TableTest` annotation
3. On first invocation of a table test method:
   - Parse table content from annotation
   - Store table metadata
   - Mark class as having table tests
   - Add method to publishing queue
4. On subsequent invocations: Store row results only
5. After all tests in class: `afterAll()` triggers publication

### Data Storage

**TableTestStore** uses JUnit's ExtensionContext stores with two scopes:

**Method Store** (per test method):
- `"table"`: Parsed `Table` object from `@TableTest` annotation
- `"rowResults"`: `List<RowResult>` containing pass/fail status for each invocation

**Class Store** (per test class):
- `"hasTableTests"`: Boolean flag indicating class has at least one `@TableTest` method
- `"methodContexts"`: `List<ExtensionContext>` of methods to publish

### YAML Publication

The `afterAll()` callback publishes two types of YAML files:

**Table YAML** (`TABLETEST-<methodName>.yaml`):
- Generated for each `@TableTest` method
- Contains: title, description, headers, rows, row results (pass/fail)
- Filename transformed via `FilenameTransformer`

**Class YAML** (`TABLETEST-<className>.yaml`):
- Generated for each test class (via `publishTestClass()`)
- Contains: title (from `@DisplayName` or class name), description (from `@Description`)
- **Current behavior**: Published for ALL test classes, even if they have no `@TableTest` methods

### Metadata Extraction

**JunitMetadataExtractor** analyzes test methods to identify:

- Column roles: 
   - expectations (recognized with configurable regex, or by default marked with `?` suffix)
   - scenarios (marked with `@Scenario` annotation)
- Test result status: passed/failed per row
- Error messages for failed tests
- Display names for test invocations

## Reporting Phase (tabletest-reporter-core)

### YAML Discovery

**ReportTree.process(inputDir)** workflow:

1. **findTableTestOutputFiles()**: Scan for all `TABLETEST-*.yaml` files recursively
2. **findTargets()**: Parse file paths to build `Target` objects representing the hierarchy
3. **buildTree()**: Construct hierarchical `ReportNode` structure (IndexNode/TableNode)

### Tree Structure

**Target representation**: Intermediate format during tree building
- Represents a position in the package/class hierarchy
- Links to resource file (YAML) if it exists
- May have no resource file (e.g., package directories, classes with no methods)

**ReportNode hierarchy**:
- **IndexNode**: Represents package or class directory with children (generates index file)
- **TableNode**: Represents individual table test method (leaf node, generates table file)

### Filtering Logic

**Current filtering behavior** (in `ReportTree.buildTree()`):

```java
if (targets.isEmpty() || targets.size() == 1 && targets.getFirst().hasNoResource())
    return null;
```

This filters out:
- Completely empty directories (no YAML files)
- Single target with no resource file

**IndexNode vs TableNode decision**:
```java
List<Target> children = targets.stream().filter(node::isParentOf).toList();
if (children.isEmpty()) {
    return new TableNode(...);  // Leaf node, no children
} else {
    return new IndexNode(...);  // Has children, generate index
}
```

**Important**: IndexNodes can be created even when the class YAML file doesn't exist (`resource = null`), as long as there are child table methods (used to create index per package).

### Template Rendering

**TemplateEngine** uses Pebble templates:

- **Index templates**: `index.adoc.peb`, `index.md.peb`
- **Table templates**: `table.adoc.peb`, `table.md.peb`
- Supports custom templates via template directory parameter

**Index context structure**:
```java
{
    "title": "Title from YAML or derived from name",
    "description": "Description from YAML (optional)",
    "name": "Derived from class/package name",
    "contents": [
        {
            "name": "child name",
            "title": "child title (optional)",
            "type": "index" or "table",
            "path": "relative/path/to/child"
        }
    ]
}
```

**File generation**:
- Index files: `{path}/index.adoc` or `{path}/index.md`
- Table files: `{path}/{table-name}.adoc` or `{path}/{table-name}.md`
- Recursive: Process IndexNode, then recursively process all children

## Key Classes and Responsibilities

| Class                      | Module | Responsibility                                                      |
|----------------------------|--------|---------------------------------------------------------------------|
| **TableTestPublisher**     | junit  | JUnit extension implementing test interception and YAML publication |
| **TableTestStore**         | junit  | ExtensionContext-based storage of table data and publishing queue   |
| **JunitMetadataExtractor** | junit  | Extract column roles, scenario indices from annotations             |
| **TableTestData**          | junit  | Data structure for table YAML serialization                         |
| **TestClassData**          | junit  | Data structure for class YAML serialization                         |
| **YamlRenderer**           | junit  | Renders data objects to YAML format                                 |
| **FilenameTransformer**    | junit  | Transforms display names to filesystem-safe names                   |
| **ReportTree**             | core   | Discovers YAML files and builds hierarchical report structure       |
| **ReportNode**             | core   | Base interface for tree nodes (IndexNode/TableNode)                 |
| **TableTestReporter**      | core   | Orchestrates template rendering and file writing                    |
| **TemplateEngine**         | core   | Loads and renders Pebble templates                                  |

## File Formats

### YAML Format

**Table YAML** (per test method):
```yaml
"title": "Test method title"
"description": "Optional description"
"headers":
- "value": "columnName"
  "roles":
  - "expectation"  # or "scenario"
"rows":
- - "value": "cellValue"
    "roles":
    - "passed"  # or "failed"
"rowResults":
- "rowIndex": !!int "1"
  "passed": !!bool "true"
  "displayName": "[1] scenario name"
  "errorMessage": "optional error message"
```

**Class YAML** (per test class):
```yaml
"title": "Test class title"
"description": "Optional description"
```

### Generated Documentation

**Directory structure**:
```
output/
├── index.adoc (or .md)
├── package-name/
│   ├── index.adoc
│   ├── TestClassName/
│   │   ├── index.adoc
│   │   ├── test-method-1.adoc
│   │   └── test-method-2.adoc
```
