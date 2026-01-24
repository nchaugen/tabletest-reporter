# TableTest Reporter Compatibility Tests

This directory contains compatibility tests that verify TableTest Reporter works correctly across different frameworks and build tools.

## Test Matrix

| Test Project | Framework | Version | Build Tool | Autodetection Method | Output Format | Reporter |
|--------------|-----------|---------|------------|---------------------|---------------|----------|
| junit-min | JUnit | 5.12 | Gradle | systemProperty | Markdown | Gradle plugin |
| junit-latest | JUnit | latest | Maven | Surefire configurationParameters | AsciiDoc+HTML | Maven + Asciidoctor |
| spring-boot-min | Spring Boot | 3.5.0 | Maven | Surefire plugin | AsciiDoc | CLI |
| spring-boot-latest | Spring Boot | latest | Gradle | junit-platform.properties | Markdown | Gradle plugin |
| quarkus-min | Quarkus | 3.21.2 | Maven | Surefire plugin (workaround) | Markdown | Maven plugin |
| quarkus-latest | Quarkus | latest | Gradle | junit-platform.properties | AsciiDoc | Gradle plugin |

Actual "latest" versions are maintained in the build files and updated weekly.

### Coverage Summary

- **JUnit versions**: 5.12 (minimum supported) and latest
- **Build tools**: 3 Maven, 3 Gradle (balanced coverage)
- **Output formats**: 3 AsciiDoc, 3 Markdown, 1 HTML
- **Reporters**: CLI, Maven plugin, Gradle plugin, Asciidoctor

## Running Tests

### Run All Tests

```bash
./run-tests.sh
```

This script:
1. Builds the main project to get SNAPSHOT artifacts
2. Runs each compatibility test sequentially
3. Reports a summary of passed/failed tests

### Run Individual Test

```bash
cd <test-directory>
./test.sh
```

## Test Structure

Each test project contains:
- **pom.xml** or **build.gradle.kts**: Build configuration with framework dependencies
- **src/test/java**: Test classes using TableTest (including edge cases)
- **test.sh**: Test script that:
  - Runs tests to generate YAML files
  - Verifies YAML files were created
  - Generates documentation (AsciiDoc or Markdown)
  - Verifies documentation files were created

## What Each Test Validates

- **JUnit Integration**: Extension autodetection works correctly
- **YAML Generation**: Test data is captured in YAML files
- **Framework Compatibility**: Works with Spring Boot and Quarkus
- **Build Tool Support**: Maven and Gradle
- **Documentation Generation**: CLI, Maven plugin, and Gradle plugin can process YAML files
- **Output Formats**: Both AsciiDoc and Markdown generation work
- **HTML Generation**: Asciidoctor converts AsciiDoc to HTML with CSS styling

## Edge Cases Tested

The junit-min and junit-latest projects include comprehensive edge case coverage:
- Multiple @TableTest methods per class
- @Disabled tests (should be excluded from reports)
- @Nested test classes
- Classes with no @TableTest methods (should not generate empty files)
- Intentionally failing tests (for CSS class verification)

## Adding New Tests

To add a new compatibility test:

1. Create a new directory: `mkdir -p <test-name>/src/test/{java/com/example,resources}`
2. Add build file (pom.xml or build.gradle.kts)
3. Add a simple test class
4. Create test.sh script following the existing pattern
5. Update run-tests.sh to include the new test
6. Update this README's test matrix

## Notes

- **Quarkus LogManager Warning**: Expected warning about LogManager in Quarkus tests
- **Quarkus Workaround**: Quarkus minimum uses Surefire plugin instead of junit-platform.properties to avoid conflicts
- **Gradle Java Version**: Gradle compatibility tests require Java 21 (detected via JAVA_HOME) because Gradle 8.14 Kotlin DSL doesn't support Java 25
