#!/bin/bash

set -e

# Source common test functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../test-common.sh"

echo "Testing: Spring Boot latest with inputDirectory and outputDirectory configuration"

# Run tests to generate YAML files
echo "Running tests..."
mvn clean test

# Check YAML files were generated
validate_yaml_files "target/junit-jupiter"

# Generate Markdown documentation with Maven plugin (default inputDirectory)
echo "Generating Markdown documentation with Maven plugin..."
mvn tabletest-reporter:report

validate_output_files "target/generated-docs/tabletest" "*.md" "Markdown"

# Test custom inputDirectory and outputDirectory configuration
# Copy YAML files to 'build' directory to simulate non-standard build output location
echo "Testing custom inputDirectory and outputDirectory configuration..."
mkdir -p build/junit-jupiter
cp -r target/junit-jupiter/* build/junit-jupiter/

# Verify YAML files were copied
validate_yaml_files "build/junit-jupiter"

# Generate documentation from 'build' directory using inputDirectory parameter
mvn tabletest-reporter:report \
    -Dtabletest.report.inputDirectory=build/junit-jupiter \
    -Dtabletest.report.outputDirectory=build/generated-docs

echo "Validating custom inputDirectory and outputDirectory..."
validate_output_files "build/generated-docs" "*.md" "Markdown"
echo "SUCCESS"
