#!/bin/bash

set -e

echo "Testing: JUnit 6.0.1 (latest) with junit-platform.properties"

# Run tests to generate YAML files
echo "Running tests..."
mvn clean test

# Check YAML files were generated
YAML_DIR="target/junit-jupiter"
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

# Get CLI jar path
CLI_JAR="../../tabletest-reporter-cli/target/tabletest-reporter-cli-0.2.1-SNAPSHOT.jar"
if [ ! -f "$CLI_JAR" ]; then
    echo "ERROR: CLI jar not found: $CLI_JAR"
    exit 1
fi

# Test AsciiDoc generation
echo "Generating AsciiDoc documentation..."
OUTPUT_DIR="target/docs/asciidoc"
java -jar "$CLI_JAR" -f asciidoc -i "$YAML_DIR" -o "$OUTPUT_DIR"

if [ ! -d "$OUTPUT_DIR" ]; then
    echo "ERROR: AsciiDoc output directory not created"
    exit 1
fi

ADOC_FILES=$(find "$OUTPUT_DIR" -name "*.adoc" | wc -l | tr -d ' ')
if [ "$ADOC_FILES" -lt 1 ]; then
    echo "ERROR: No AsciiDoc files generated"
    exit 1
fi

echo "Generated $ADOC_FILES AsciiDoc files"
echo "SUCCESS"
