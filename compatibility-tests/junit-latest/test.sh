#!/bin/bash

set -e

# Source common test functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../test-common.sh"

echo "Testing: JUnit latest with Surefire plugin configuration"

# Run tests to generate YAML files
echo "Running tests..."
mvn clean test

# Check YAML files were generated
validate_yaml_files "target/junit-jupiter"

# Generate AsciiDoc documentation with Maven plugin
echo "Generating AsciiDoc documentation with Maven plugin..."
mvn tabletest-reporter:report

validate_output_files "target/generated-docs/tabletest" "*.adoc" "AsciiDoc"
echo "SUCCESS"
