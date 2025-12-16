#!/bin/bash

set -e

echo "Testing: Spring Boot 3.5.0 (minimum) with Surefire plugin configuration"

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

# Use Maven plugin to generate Markdown documentation
echo "Generating Markdown documentation with Maven plugin..."
mvn io.github.nchaugen:tabletest-reporter-maven-plugin:0.2.1-SNAPSHOT:report -Dtabletest.report.format=markdown

OUTPUT_DIR="target/generated-docs/tabletest"
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
