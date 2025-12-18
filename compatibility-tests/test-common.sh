#!/bin/bash

# Common functions for compatibility tests

# Validate that YAML files were generated
# Arguments:
#   $1 - YAML directory path
#   $2 - Minimum number of expected files (default: 2)
validate_yaml_files() {
    local yaml_dir=$1
    local min_files=${2:-2}

    if [ ! -d "$yaml_dir" ]; then
        echo "ERROR: YAML directory not found: $yaml_dir"
        exit 1
    fi

    local yaml_count=$(find "$yaml_dir" -name "TABLETEST-*.yaml" | wc -l | tr -d ' ')
    if [ "$yaml_count" -lt "$min_files" ]; then
        echo "ERROR: Expected at least $min_files YAML files, found $yaml_count"
        exit 1
    fi

    echo "Found $yaml_count YAML files"
}

# Validate that output documentation files were generated
# Arguments:
#   $1 - Output directory path
#   $2 - File pattern (e.g., "*.adoc" or "*.md")
#   $3 - File type description (e.g., "AsciiDoc" or "Markdown")
#   $4 - Minimum number of expected files (default: 1)
validate_output_files() {
    local output_dir=$1
    local file_pattern=$2
    local file_type=$3
    local min_files=${4:-1}

    if [ ! -d "$output_dir" ]; then
        echo "ERROR: Output directory not created: $output_dir"
        exit 1
    fi

    local file_count=$(find "$output_dir" -name "$file_pattern" | wc -l | tr -d ' ')
    if [ "$file_count" -lt "$min_files" ]; then
        echo "ERROR: No $file_type files generated"
        exit 1
    fi

    echo "Generated $file_count $file_type files"
}

# Find CLI jar dynamically (version-independent)
# Returns the path to the CLI jar or exits with error
find_cli_jar() {
    local cli_jar=$(find ../../tabletest-reporter-cli/target -name "tabletest-reporter-cli-*-SNAPSHOT.jar" 2>/dev/null | head -n 1)
    if [ -z "$cli_jar" ] || [ ! -f "$cli_jar" ]; then
        echo "ERROR: CLI jar not found in ../../tabletest-reporter-cli/target"
        exit 1
    fi
    echo "$cli_jar"
}
