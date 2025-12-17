#!/bin/bash

set -e

echo "Testing: Quarkus latest with junit-platform.properties"

# Run tests and generate report (report runs automatically in verify phase)
echo "Running tests and generating report..."
mvn clean verify

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

OUTPUT_DIR="target/generated-docs/tabletest"
if [ ! -d "$OUTPUT_DIR" ]; then
    echo "ERROR: Output directory not created"
    exit 1
fi

ADOC_FILES=$(find "$OUTPUT_DIR" -name "*.adoc" | wc -l | tr -d ' ')
if [ "$ADOC_FILES" -lt 1 ]; then
    echo "ERROR: No AsciiDoc files generated"
    exit 1
fi

echo "Generated $ADOC_FILES AsciiDoc files"
echo "SUCCESS"
