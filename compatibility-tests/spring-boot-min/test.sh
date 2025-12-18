#!/bin/bash

set -e

echo "Testing: Spring Boot minimum with Surefire plugin configuration"

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

# Get CLI jar path (detect version dynamically)
CLI_JAR=$(find ../../tabletest-reporter-cli/target -name "tabletest-reporter-cli-*-SNAPSHOT.jar" 2>/dev/null | head -n 1)
if [ -z "$CLI_JAR" ] || [ ! -f "$CLI_JAR" ]; then
    echo "ERROR: CLI jar not found in ../../tabletest-reporter-cli/target"
    exit 1
fi

# Test AsciiDoc generation with CLI
echo "Generating AsciiDoc documentation with CLI..."
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
