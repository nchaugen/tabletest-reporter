#!/bin/bash

set -e

# Source common test functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../test-common.sh"

echo "Testing: Spring Boot minimum with Surefire plugin configuration"

# Run tests to generate YAML files
echo "Running tests..."
mvn clean test

# Check YAML files were generated
validate_yaml_files "target/junit-jupiter"

# Get CLI jar path (detect version dynamically)
CLI_JAR=$(find_cli_jar)

# Test AsciiDoc generation with CLI
echo "Generating AsciiDoc documentation with CLI..."
OUTPUT_DIR="target/docs/asciidoc"
java -jar "$CLI_JAR" -f asciidoc -i "$YAML_DIR" -o "$OUTPUT_DIR"

validate_output_files "$OUTPUT_DIR" "*.adoc" "AsciiDoc"
echo "SUCCESS"
