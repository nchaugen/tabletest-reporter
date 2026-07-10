#!/bin/bash

set -e

# Source common test functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../test-common.sh"

echo "Testing: Spring Boot minimum with Surefire plugin configuration"

# Run tests to generate YAML files
echo "Running tests..."
mvn -B clean test

# Check YAML files were generated
validate_yaml_files "target/junit-jupiter"

# Get CLI jar path (detect version dynamically)
CLI_JAR=$(find_cli_jar)

# Test AsciiDoc generation with CLI
echo "Generating AsciiDoc documentation with CLI..."
YAML_DIR="target/junit-jupiter"
OUTPUT_DIR="target/docs/asciidoc"
java -jar "$CLI_JAR" -f asciidoc -i "$YAML_DIR" -o "$OUTPUT_DIR"

validate_output_files "$OUTPUT_DIR" "*.adoc" "AsciiDoc"

# Test single-file HTML assembly with CLI (-s): the whole report collapses into one
# self-contained index.html with the search index inlined and no sibling assets.
echo "Generating single-file HTML documentation with CLI..."
SINGLE_DIR="target/docs/html-single"
java -jar "$CLI_JAR" -f html -s -i "$YAML_DIR" -o "$SINGLE_DIR"

if [ ! -f "$SINGLE_DIR/index.html" ]; then
    echo "ERROR: Single-file mode did not produce $SINGLE_DIR/index.html"
    exit 1
fi
SINGLE_COUNT=$(find "$SINGLE_DIR" -type f | wc -l | tr -d ' ')
if [ "$SINGLE_COUNT" -ne 1 ]; then
    echo "ERROR: Single-file mode wrote $SINGLE_COUNT files, expected exactly 1"
    find "$SINGLE_DIR" -type f
    exit 1
fi
assert_self_contained_html "$SINGLE_DIR"
echo "Verified single-file HTML is one self-contained file"

echo "SUCCESS"
