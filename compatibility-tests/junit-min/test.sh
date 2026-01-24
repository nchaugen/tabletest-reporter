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
echo "JUnit Min (5.12) with Gradle + Markdown"
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

# Step 1: Run tests (with intentional failures)
echo -e "\n${YELLOW}[1/5] Running tests (with intentional failures)...${NC}"
./gradlew --console=plain clean test
echo -e "${GREEN}✓ Tests completed${NC}"

# Step 2: Validate YAML generation
echo -e "\n${YELLOW}[2/5] Validating YAML generation...${NC}"
validate_yaml_files "build/junit-jupiter" 10
echo -e "${GREEN}✓ YAML files generated${NC}"

# Step 3: Generate Markdown
echo -e "\n${YELLOW}[3/5] Generating Markdown reports...${NC}"
./gradlew --console=plain reportTableTests
echo -e "${GREEN}✓ Markdown generation completed${NC}"

# Step 4: Validate Markdown generation
echo -e "\n${YELLOW}[4/5] Validating Markdown generation...${NC}"
validate_output_files "build/generated-docs/tabletest" "*.md" "Markdown" 11
echo -e "${GREEN}✓ Markdown files generated${NC}"

# Step 5: Verify custom expectation pattern in YAML
echo -e "\n${YELLOW}[5/5] Verifying custom expectation pattern...${NC}"
# Find the method-level YAML file for CustomExpectationPatternTest (contains "headers")
YAML_FILE=$(find build/junit-jupiter -path "*CustomExpectationPatternTest*" -name "*.yaml" -exec grep -l '"headers"' {} \; | head -1)
if [ -z "$YAML_FILE" ]; then
    echo -e "${RED}✗ Custom expectation pattern YAML file not found${NC}"
    exit 1
fi

# Check that "Expected Result" column has expectation role in YAML
if grep -q '"expectation"' "$YAML_FILE" && grep -q '"Expected Result"' "$YAML_FILE"; then
    echo -e "${GREEN}✓ Custom expectation pattern captured in YAML${NC}"
else
    echo -e "${RED}✗ Custom expectation pattern not found in YAML${NC}"
    echo "YAML content:"
    cat "$YAML_FILE"
    exit 1
fi

echo -e "\n=========================================="
echo -e "${GREEN}SUCCESS: All verification steps passed${NC}"
echo -e "=========================================="
