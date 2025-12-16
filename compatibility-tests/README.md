# TableTest Reporter Compatibility Tests

This directory contains compatibility tests that verify TableTest Reporter works correctly across different frameworks and build tools.

## Test Matrix

| Test Project | Framework | Version | Build Tool | Autodetection Method | Output Format | Reporter |
|--------------|-----------|---------|------------|---------------------|---------------|----------|
| junit-latest | JUnit | 6.0.1 | Maven | junit-platform.properties | AsciiDoc | Maven plugin |
| spring-boot-min | Spring Boot | 3.5.0 | Maven | Surefire plugin | AsciiDoc | CLI |
| spring-boot-latest | Spring Boot | 4.0.0 | Maven | junit-platform.properties | Markdown | Maven plugin |
| quarkus-min | Quarkus | 3.21.2 | Maven | Surefire plugin (workaround) | Markdown | CLI |
| quarkus-latest | Quarkus | 3.30.3 | Maven | junit-platform.properties | AsciiDoc | Maven plugin |

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
- **src/test/java**: Simple test class using TableTest
- **test.sh**: Test script that:
  - Runs tests to generate YAML files
  - Verifies YAML files were created
  - Generates documentation (AsciiDoc or Markdown)
  - Verifies documentation files were created

## What Each Test Validates

- **JUnit Integration**: Extension autodetection works correctly
- **YAML Generation**: Test data is captured in YAML files
- **Framework Compatibility**: Works with Spring Boot and Quarkus
- **Build Tool Support**: Maven and Gradle (via Spring Boot)
- **Documentation Generation**: CLI and Maven plugin can process YAML files
- **Output Formats**: Both AsciiDoc and Markdown generation work

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
- **Gradle Plugin**: Not yet tested due to JUnit Platform compatibility issues with Gradle test runners (will be addressed in future)
