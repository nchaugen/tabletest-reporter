# TableTest Reporter Compatibility Tests

This directory contains compatibility tests that verify TableTest Reporter works correctly across different frameworks, build tools, and input directory resolution mechanisms.

## Compatibility Concerns

The test suite is designed around four distinct compatibility concerns:

| Concern | What We're Testing |
|---------|-------------------|
| **(a) Output format × doc tools** | Does generated AsciiDoc/Markdown work with Asciidoctor and other publishing tools? |
| **(b) Input dir × JUnit versions** | Do default output dir and `junit.platform.reporting.output.dir` behave consistently across JUnit 5.x and 6.x? |
| **(c) Input dir × build tools + runners** | Do Surefire, Gradle, and the reporter runners (Maven plugin, Gradle plugin, CLI) correctly discover YAML across different default dirs and config mechanisms? |
| **(d) Extension × frameworks** | Does the JUnit extension autodetect and work with JUnit, Spring Boot, and Quarkus test runners? |

Sub-concerns of (c) and (d):

- **(c1) Build tool versions**: Different Surefire and Gradle versions affect how JUnit Platform is launched and how properties are forwarded to the test JVM. Each project pins specific versions to cover a range.
- **(c2) Reporter plugin × build tool versions**: The Maven and Gradle reporter plugins must work with the range of Maven/Gradle versions users have.
- **(d1) Framework test execution modes**: Quarkus (`@QuarkusTest`) and Spring Boot (`@SpringBootTest`) wrap JUnit's lifecycle. Autodetection, extension ordering, and event reporting may behave differently than plain JUnit.

Output format × reporter runner combinations are **not** a compatibility concern — they are well covered by unit tests. The format and runner assigned to each project below are incidental; what matters is full coverage of concerns (a)–(d).

## Test Matrix

### JUnit projects (concerns b + c)

Test input directory discovery across JUnit versions and build tools. Each project validates convention fallback as the primary mechanism and tests one additional input dir mechanism as a second pass.

| Project | JUnit | Build | Primary Input Dir | Additional Test | Reporter |
|---------|-------|-------|-------------------|-----------------|----------|
| junit-5-maven | 5.12 | Maven | Convention (`target/junit-jupiter`) | `junit-platform.properties` | Maven plugin |
| junit-5-gradle | 5.12 | Gradle | Convention (`build/junit-jupiter`) | Gradle `jvmArgumentProviders` | Gradle plugin |
| junit-6-maven | 6.0 | Maven | Convention (`target/junit-jupiter`) | Surefire `configurationParameters` | Maven plugin |
| junit-6-gradle | 6.0 | Gradle | Convention (`build/junit-jupiter`) | Explicit reporter `inputDir` | Gradle plugin |

### Framework projects (concern d)

Test extension autodetection and API compatibility with framework test runners. Use convention fallback only — input dir discovery is the JUnit projects' responsibility.

| Project | Framework | Version | Build | Reporter |
|---------|-----------|---------|-------|----------|
| spring-boot-min | Spring Boot | 3.5.0 | Maven | CLI |
| spring-boot-latest | Spring Boot | latest | Gradle | Gradle plugin |
| quarkus-min | Quarkus | 3.21.2 | Maven | Maven plugin |
| quarkus-latest | Quarkus | latest | Gradle | Gradle plugin |

Actual "latest" versions are maintained in the build files and updated weekly.

### Doc tool testing (concern a)

Asciidoctor HTML conversion is tested as an extra step in **junit-6-maven**. Future publishing tools are added as steps in other projects.

## Coverage Analysis

### (a) Doc tools

- Asciidoctor HTML conversion: junit-6-maven

### (b) Input dir × JUnit versions

| | Maven | Gradle |
|---|---|---|
| JUnit 5.12 | junit-5-maven | junit-5-gradle |
| JUnit 6.0 | junit-6-maven | junit-6-gradle |

### (c) Input dir mechanisms

| Mechanism | Maven | Gradle |
|-----------|-------|--------|
| Convention fallback | all Maven projects | all Gradle projects |
| Surefire `configurationParameters` | junit-6-maven | N/A |
| Gradle `jvmArgumentProviders` | N/A | junit-5-gradle |
| `junit-platform.properties` | junit-5-maven | — |
| Explicit reporter `inputDir` | — | junit-6-gradle |

### (c) Reporter runners

| Runner | Projects |
|--------|----------|
| Maven plugin | junit-5-maven, junit-6-maven, quarkus-min |
| Gradle plugin | junit-5-gradle, junit-6-gradle, spring-boot-latest, quarkus-latest |
| CLI | spring-boot-min |

### (d) Extension × frameworks

| Framework | Version | Build | JUnit (transitive) |
|-----------|---------|-------|---------------------|
| JUnit | 5.12 | Maven + Gradle | 5.12 |
| JUnit | 6.0 | Maven + Gradle | 6.0 |
| Spring Boot | 3.5 | Maven | 5.x |
| Spring Boot | 4.0 | Gradle | 6.x |
| Quarkus | 3.21 | Maven | 5.x |
| Quarkus | 3.30 | Gradle | 5.x or 6.x |

Framework projects use their native test annotations (`@SpringBootTest`, `@QuarkusTest`) to exercise framework test runner integration, not plain `@Test`.

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
  - (JUnit projects) Runs a second pass with a non-convention input dir mechanism

## Edge Cases Tested

The JUnit projects include comprehensive edge case coverage:
- Multiple @TableTest methods per class
- @Disabled tests (should be excluded from reports)
- @Nested test classes
- Classes with no @TableTest methods (should not generate empty files)
- Intentionally failing tests (for CSS class verification)
- Custom expectation patterns

## Adding New Tests

To add a new compatibility test:

1. Create a new directory: `mkdir -p <test-name>/src/test/{java/com/example,resources}`
2. Add build file (pom.xml or build.gradle.kts)
3. Add test classes (use framework-native test annotations for framework projects)
4. Create test.sh script following the existing pattern
5. Update run-tests.sh to include the new test
6. Update this README's test matrix and coverage analysis

## Notes

- **Quarkus LogManager Warning**: Expected warning about LogManager in Quarkus tests
- **Quarkus Workaround**: Quarkus minimum uses Surefire plugin instead of junit-platform.properties to avoid conflicts
- **Gradle Java Version**: Gradle compatibility tests require Java 21 (detected via JAVA_HOME) because Gradle 8.14 Kotlin DSL doesn't support Java 25
