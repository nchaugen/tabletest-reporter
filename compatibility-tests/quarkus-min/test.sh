#!/bin/bash

set -e

# Source common test functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../test-common.sh"

echo "Testing: Quarkus minimum with Surefire plugin (workaround)"

# Run tests to generate YAML files
echo "Running tests..."
mvn -B clean test

# Check YAML files were generated
validate_yaml_files "target/junit-jupiter"

# Get Maven plugin version (detect version dynamically)
PLUGIN_VERSION=$(get_maven_plugin_version)

# Use Maven plugin to generate Markdown documentation
# Note: Uses full coordinates including version because plugin is not configured in pom.xml
echo "Generating Markdown documentation with Maven plugin..."
mvn -B org.tabletest:tabletest-reporter-maven-plugin:$PLUGIN_VERSION:report -Dtabletest.report.format=markdown

validate_output_files "target/generated-docs/tabletest" "*.md" "Markdown"
echo "SUCCESS"
