#!/bin/bash

set -e

echo "Testing: Gradle latest with tabletest-reporter-gradle-plugin"

# Ensure JAVA_HOME points to Java 21 for Gradle daemon
# (Gradle 8.14 Kotlin DSL doesn't support Java 25)
if [ -z "$JAVA_HOME" ] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -q "version \"21"; then
    # Try to detect Java 21 if JAVA_HOME not set or not pointing to Java 21
    if command -v /usr/libexec/java_home &> /dev/null; then
        # macOS
        export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home -v 21.0 2>/dev/null)
    elif [ -d "/usr/lib/jvm/temurin-21-jdk-amd64" ]; then
        # Linux (Temurin on CI)
        export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
    elif [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
        # Linux (OpenJDK)
        export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
    fi
fi

# Run tests to generate YAML files
echo "Running tests..."
./gradlew clean test

# Check YAML files were generated
YAML_DIR="build/junit-jupiter"
if [ ! -d "$YAML_DIR" ]; then
    echo "ERROR: YAML directory not found: $YAML_DIR"
    exit 1
fi

YAML_FILES=$(find "$YAML_DIR" -name "TABLETEST-*.yaml" | wc -l | tr -d ' ')
if [ "$YAML_FILES" -lt 2 ]; then
    echo "ERROR: Expected at least 2 YAML files, found $YAML_FILES"
    exit 1
fi

echo "Found $YAML_FILES YAML files"

# Generate Markdown documentation with Gradle plugin
echo "Generating Markdown documentation with Gradle plugin..."
./gradlew reportTableTests

OUTPUT_DIR="build/generated-docs/tabletest"
if [ ! -d "$OUTPUT_DIR" ]; then
    echo "ERROR: Output directory not created"
    exit 1
fi

MD_FILES=$(find "$OUTPUT_DIR" -name "*.md" | wc -l | tr -d ' ')
if [ "$MD_FILES" -lt 1 ]; then
    echo "ERROR: No Markdown files generated"
    exit 1
fi

echo "Generated $MD_FILES Markdown files"
echo "SUCCESS"
