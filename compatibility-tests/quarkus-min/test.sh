#!/bin/bash

set -e

# Source common test functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../test-common.sh"

echo "Testing: Quarkus minimum with Surefire plugin (workaround)"

# Run tests to generate YAML files
echo "Running tests..."
mvn clean test

# Check YAML files were generated
validate_yaml_files "target/junit-jupiter"

# Use Maven plugin to generate Markdown documentation
# Note: Uses full coordinates including version because plugin is not configured in pom.xml
echo "Generating Markdown documentation with Maven plugin..."
mvn io.github.nchaugen:tabletest-reporter-maven-plugin:0.2.1-SNAPSHOT:report -Dtabletest.report.format=markdown

validate_output_files "target/generated-docs/tabletest" "*.md" "Markdown"
echo "SUCCESS"
