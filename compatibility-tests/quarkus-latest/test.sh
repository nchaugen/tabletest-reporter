#!/bin/bash

set -e

# Source common test functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../test-common.sh"

echo "Testing: Quarkus latest with junit-platform.properties"

# Run tests and generate report (report runs automatically in verify phase)
echo "Running tests and generating report..."
mvn clean verify

# Check YAML files were generated
validate_yaml_files "target/junit-jupiter"

OUTPUT_DIR="target/generated-docs/tabletest"
if [ ! -d "$OUTPUT_DIR" ]; then
    echo "ERROR: Output directory not created"
    exit 1
fi

validate_output_files "target/generated-docs/tabletest" "*.adoc" "AsciiDoc"
echo "SUCCESS"
