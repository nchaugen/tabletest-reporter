#!/bin/bash

set -e

echo "Testing: Spring Boot latest with inputDirectory and outputDirectory configuration"

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

# Generate Markdown documentation with Maven plugin (default inputDirectory)
echo "Generating Markdown documentation with Maven plugin..."
mvn tabletest-reporter:report

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

# Test custom inputDirectory and outputDirectory configuration
# Copy YAML files to 'build' directory to simulate non-standard build output location
echo "Testing custom inputDirectory and outputDirectory configuration..."
mkdir -p build/junit-jupiter
cp -r target/junit-jupiter/* build/junit-jupiter/

# Verify YAML files were copied
BUILD_YAML_FILES=$(find build/junit-jupiter -name "TABLETEST-*.yaml" | wc -l | tr -d ' ')
if [ "$BUILD_YAML_FILES" -lt 2 ]; then
    echo "ERROR: Failed to copy YAML files to build directory"
    exit 1
fi

echo "Copied $BUILD_YAML_FILES YAML files to build/junit-jupiter"

# Generate documentation from 'build' directory using inputDirectory parameter
mvn tabletest-reporter:report \
    -Dtabletest.report.inputDirectory=build/junit-jupiter \
    -Dtabletest.report.outputDirectory=build/generated-docs

BUILD_OUTPUT_DIR="build/generated-docs"
if [ ! -d "$BUILD_OUTPUT_DIR" ]; then
    echo "ERROR: Custom input/output directory test failed - output not created"
    exit 1
fi

BUILD_MD_FILES=$(find "$BUILD_OUTPUT_DIR" -name "*.md" | wc -l | tr -d ' ')
if [ "$BUILD_MD_FILES" -lt 1 ]; then
    echo "ERROR: No Markdown files generated from custom inputDirectory"
    exit 1
fi

echo "Generated $BUILD_MD_FILES Markdown files from custom inputDirectory and outputDirectory"
echo "SUCCESS"
