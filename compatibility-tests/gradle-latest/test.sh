#!/bin/bash

set -e

# Source common test functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../test-common.sh"

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
validate_yaml_files "build/junit-jupiter"

# Generate Markdown documentation with Gradle plugin
echo "Generating Markdown documentation with Gradle plugin..."
./gradlew reportTableTests

validate_output_files "build/generated-docs/tabletest" "*.md" "Markdown"
echo "SUCCESS"
