# TableTest Reporter Test Strategy

## Overview

The project uses a multi-layered testing approach combining unit tests, integration tests, and compatibility tests to ensure the reporter works correctly across different frameworks, build tools, and JUnit versions.

## Test Layers

### 1. Unit Tests

Located within each module's `src/test/java` directory. These test individual components in isolation.

#### Rendering tests: a blind spot to guard against

The template rendering tests (`rendering/TableWith*Test`) golden-file the generated
**AsciiDoc/Markdown source**, and `AsciiDocValidator.assertValidAsciiDoc` only checks
that the source *parses*. Neither asserts anything about how the output *renders* — so a
template change can keep every golden test green while producing a visually broken
document. This has bitten us: switching the table to `[%header%autowidth]` (dropping the
explicit `cols`) parsed fine and matched updated goldens, but Asciidoctor then inferred a
single column because each cell sits on its own line.

Guard against it with automated structural assertions, not by eyeballing. The mechanism
already exists: `AsciiDocValidator` loads the rendered output into the AsciidoctorJ AST and
asserts against the render context, and the `assertValidAsciiDoc(rendered, context)`
overload asserts that the rendered column count matches the number of headers. Table
rendering tests call that overload — the single-argument `assertValidAsciiDoc(rendered)` only
proves the source parses.

**When adding or changing rendered structure, assert it structurally — do not rely on the
source-level golden alone.** If a template change makes a new structural property matter
(row count, header emphasis, nesting depth, cell spans, …), add an AST-level check in
`AsciiDocValidator` the same way `assertColumnsMatchHeaders` does (`document.findBy(...)`
over the parsed model), and drive the assertion from the render context, which is the
independent source of truth for what the reporter intended. This is exactly how the
single-column autowidth regression is now caught automatically.

Eyeballing a generated doc in a real AsciiDoc/Markdown viewer remains a useful final check
after `table.*.peb` / `macros.*.peb` changes, but it is the backup, not the guard — anything
worth catching is worth asserting.

### 2. Integration Tests

Test the interaction between modules (e.g., JUnit extension generating YAML that the core module processes).

### 3. Compatibility Tests

Located in `compatibility-tests/`. These are end-to-end tests that verify the complete pipeline works across different environments.

## Compatibility Test Infrastructure

### Directory Structure

```
compatibility-tests/
├── run-tests.sh           # Main orchestrator script
├── test-common.sh         # Shared utility functions
├── README.md              # Test matrix documentation
├── junit-5-gradle/        # JUnit 5.12 + Gradle + Markdown
├── junit-6-maven/         # JUnit latest + Maven + AsciiDoc + HTML
├── spring-boot-min/       # Spring Boot 3.5.0 + Maven + CLI
├── spring-boot-latest/    # Spring Boot latest + Gradle + Markdown
├── quarkus-min/           # Quarkus 3.21.2 + Maven + Markdown
└── quarkus-latest/        # Quarkus latest + Gradle + AsciiDoc
```

### Test Execution Flow

1. `run-tests.sh` builds the main project to create SNAPSHOT artifacts
2. Each test project is executed sequentially
3. Results are tracked and summarised (pass/fail counts)
4. Exit code reflects overall success/failure

### Individual Test Structure

Each compatibility test project contains:
- **Build file**: `pom.xml` or `build.gradle.kts`
- **Test classes**: TableTest examples including edge cases in `src/test/java`
- **Test script**: `test.sh` that orchestrates the test

### Test Script Pattern

Each `test.sh` typically:
1. Runs tests to generate YAML files
2. Validates YAML files were created (count and location)
3. Generates documentation (AsciiDoc or Markdown)
4. Validates output files were created

### Shared Utilities (`test-common.sh`)

- `validate_yaml_files()` - Verifies YAML files exist
- `validate_output_files()` - Verifies documentation files exist
- `find_cli_jar()` - Locates CLI jar dynamically
- `get_maven_plugin_version()` - Extracts plugin version

## Test Matrix

| Test Project        | Framework   | Version | Build Tool | Autodetection                    | Output        | Reporter       |
|---------------------|-------------|---------|------------|----------------------------------|---------------|----------------|
| junit-5-gradle      | JUnit       | 5.12    | Gradle     | systemProperty                   | Markdown      | Gradle plugin  |
| junit-6-maven       | JUnit       | latest  | Maven      | Surefire configurationParameters | AsciiDoc+HTML | Maven + Asciidoctor |
| spring-boot-min     | Spring Boot | 3.5.0   | Maven      | Surefire plugin                  | AsciiDoc      | CLI            |
| spring-boot-latest  | Spring Boot | latest  | Gradle     | junit-platform.properties        | Markdown      | Gradle plugin  |
| quarkus-min         | Quarkus     | 3.21.2  | Maven      | Surefire plugin (workaround)     | Markdown      | Maven plugin   |
| quarkus-latest      | Quarkus     | latest  | Gradle     | junit-platform.properties        | AsciiDoc      | Gradle plugin  |

Actual "latest" versions are maintained in the build files (`pom.xml`, `build.gradle.kts`) and updated weekly.

### Coverage Summary

- **JUnit versions**: Both 5.12 (minimum supported) and latest
- **Build tools**: 3 Maven, 3 Gradle projects
- **Output formats**: AsciiDoc (3 projects), Markdown (3 projects), HTML (1 project)
- **Reporters**: CLI (1), Maven plugin (2), Gradle plugin (3), Asciidoctor (1)
- **Edge cases**: Full test suites with multiple @TableTest, @Disabled, @Nested, failing tests

## What Compatibility Tests Verify

### JUnit Extension
- Autodetection works via different configuration methods
- YAML files are generated correctly
- Works with both JUnit 5.12 and 6.0.3

### Framework Integration
- Spring Boot test context doesn't interfere with extension
- Quarkus test lifecycle is compatible
- No conflicts with framework-specific test runners

### Build Tool Support
- Maven Surefire plugin configuration patterns
- Gradle test task configuration
- Plugin execution in both ecosystems

### Reporter Tools
- CLI processes YAML and generates documentation
- Maven plugin integrates with build lifecycle
- Gradle plugin provides correct tasks

### Output Formats
- AsciiDoc generation produces valid files
- Markdown generation produces valid files
- HTML generation via Asciidoctor with CSS styling
- File structure mirrors package hierarchy

## HTML CSS Verification (junit-6-maven)

The junit-6-maven project includes comprehensive HTML verification:

1. **Intentional test failures**: Tests include deliberately failing assertions
2. **8-step verification process**:
   - Run tests (with failures)
   - Validate YAML generation
   - Generate AsciiDoc with Maven plugin
   - Validate AsciiDoc generation
   - Convert to HTML via Asciidoctor
   - Validate HTML generation
   - Verify CSS is embedded
   - Run HtmlCssVerifier for detailed CSS class checks

3. **CSS class verification**: Custom Java utility (`HtmlCssVerifier`) validates:
   - `.scenario` class presence and count
   - `.expectation` class presence and count
   - `.passed` and `.failed` class counts match expected results

## Running Compatibility Tests

### All Tests
```bash
./compatibility-tests/run-tests.sh
```

### Individual Test
```bash
cd compatibility-tests/<test-name>
./test.sh
```

## When to Run Compatibility Tests

Run before committing changes to:
- `tabletest-reporter-junit` (JUnit extension)
- `tabletest-reporter-core` (report generation)
- `tabletest-reporter-cli` (CLI tool)
- `tabletest-reporter-maven-plugin` (Maven plugin)
- `tabletest-reporter-gradle-plugin` (Gradle plugin)
- YAML format changes
- Template changes
- Public API changes

Skip for:
- Documentation-only changes
- CI/CD workflow changes
- Development tooling changes

## Test Data

Compatibility tests include comprehensive test examples:
- Calculator tests (addition, subtraction, multiplication)
- Edge case tests (multiple @TableTest per class, @Disabled, @Nested)
- Failing tests for CSS class verification
- Various table formats and scenarios

This ensures tests exercise the full pipeline while verifying edge case handling.
