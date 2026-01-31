#!/bin/bash

set -e

# Source common test functions
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../test-common.sh"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "JUnit 6 Gradle with AsciiDoc"
echo "=========================================="

# Ensure JAVA_HOME points to Java 21 for Gradle daemon
if [ -z "$JAVA_HOME" ] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -q "version \"21"; then
    if command -v /usr/libexec/java_home &> /dev/null; then
        # macOS
        export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home -v 21.0 2>/dev/null)
    elif [ -d "/usr/lib/jvm/temurin-21-jdk-amd64" ]; then
        # Linux (Temurin on CI)
        export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
    elif [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
        # Linux (OpenJDK)
        export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
    fi
fi

# ==================================================================
# Pass 1: Convention fallback (build/junit-jupiter)
# ==================================================================
echo -e "\n--- Pass 1: Convention fallback ---"

# Step 1: Run tests (with intentional failures)
echo -e "\n${YELLOW}[1/8] Running tests (convention fallback)...${NC}"
./gradlew --console=plain clean test
echo -e "${GREEN}✓ Tests completed${NC}"

# Step 2: Validate YAML generation
echo -e "\n${YELLOW}[2/8] Validating YAML generation at build/junit-jupiter...${NC}"
validate_yaml_files "build/junit-jupiter" 10
echo -e "${GREEN}✓ YAML files generated${NC}"

# Step 3: Generate AsciiDoc
echo -e "\n${YELLOW}[3/8] Generating AsciiDoc reports...${NC}"
./gradlew --console=plain reportTableTests
echo -e "${GREEN}✓ AsciiDoc generation completed${NC}"

# Step 4: Validate AsciiDoc generation
echo -e "\n${YELLOW}[4/8] Validating AsciiDoc generation...${NC}"
validate_output_files "build/generated-docs/tabletest" "*.adoc" "AsciiDoc" 11
echo -e "${GREEN}✓ AsciiDoc files generated${NC}"

# ==================================================================
# Pass 2: Explicit reporter inputDir
# ==================================================================
echo -e "\n--- Pass 2: Explicit reporter inputDir ---"

# Step 5: Re-run tests
echo -e "\n${YELLOW}[5/8] Running tests (explicit inputDir)...${NC}"
./gradlew --console=plain clean test
echo -e "${GREEN}✓ Tests completed${NC}"

# Step 6: Validate YAML generation
echo -e "\n${YELLOW}[6/8] Validating YAML generation at build/junit-jupiter...${NC}"
validate_yaml_files "build/junit-jupiter" 10
echo -e "${GREEN}✓ YAML files generated${NC}"

# Step 7: Generate AsciiDoc with explicit inputDir
echo -e "\n${YELLOW}[7/8] Generating AsciiDoc reports with explicit inputDir...${NC}"
./gradlew --console=plain reportTableTests -PtabletestReporterInputDir=build/junit-jupiter
echo -e "${GREEN}✓ AsciiDoc generation completed${NC}"

# Step 8: Validate AsciiDoc generation
echo -e "\n${YELLOW}[8/8] Validating AsciiDoc generation...${NC}"
validate_output_files "build/generated-docs/tabletest" "*.adoc" "AsciiDoc" 11
echo -e "${GREEN}✓ AsciiDoc files generated${NC}"

echo -e "\n=========================================="
echo -e "${GREEN}SUCCESS: All verification steps passed${NC}"
echo -e "=========================================="
