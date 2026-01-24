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
echo "Quarkus Latest (3.30.3) with Gradle"
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

# Step 1: Run tests
echo -e "\n${YELLOW}[1/4] Running tests...${NC}"
./gradlew --console=plain clean test
echo -e "${GREEN}✓ Tests completed${NC}"

# Step 2: Validate YAML generation
echo -e "\n${YELLOW}[2/4] Validating YAML generation...${NC}"
validate_yaml_files "build/junit-jupiter"
echo -e "${GREEN}✓ YAML files generated${NC}"

# Step 3: Generate AsciiDoc with Gradle plugin
echo -e "\n${YELLOW}[3/4] Generating AsciiDoc documentation...${NC}"
./gradlew --console=plain reportTableTests
echo -e "${GREEN}✓ AsciiDoc generation completed${NC}"

# Step 4: Validate AsciiDoc generation
echo -e "\n${YELLOW}[4/4] Validating AsciiDoc generation...${NC}"
validate_output_files "build/generated-docs/tabletest" "*.adoc" "AsciiDoc"
echo -e "${GREEN}✓ AsciiDoc files generated${NC}"

echo -e "\n=========================================="
echo -e "${GREEN}SUCCESS: All verification steps passed${NC}"
echo -e "=========================================="
