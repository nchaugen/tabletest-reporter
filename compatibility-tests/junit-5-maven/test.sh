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

PROPERTIES_FILE="src/test/resources/junit-platform.properties"

# Ensure properties file does not exist at start (convention fallback pass)
rm -f "$PROPERTIES_FILE"

echo "=========================================="
echo "JUnit 5 Maven with Markdown"
echo "=========================================="

# ==================================================================
# Pass 1: Convention fallback (target/junit-jupiter)
# ==================================================================
echo -e "\n--- Pass 1: Convention fallback ---"

# Step 1: Run tests (with intentional failures)
echo -e "\n${YELLOW}[1/10] Running tests (convention fallback)...${NC}"
mvn -B clean test || true
echo -e "${GREEN}✓ Tests completed${NC}"

# Step 2: Validate YAML generation at convention location
echo -e "\n${YELLOW}[2/10] Validating YAML generation at target/junit-jupiter...${NC}"
validate_yaml_files "target/junit-jupiter" 10
echo -e "${GREEN}✓ YAML files generated${NC}"

# Step 3: Generate Markdown
echo -e "\n${YELLOW}[3/10] Generating Markdown reports...${NC}"
mvn -B tabletest-reporter:report
echo -e "${GREEN}✓ Markdown generation completed${NC}"

# Step 4: Validate Markdown generation
echo -e "\n${YELLOW}[4/10] Validating Markdown generation...${NC}"
validate_output_files "target/generated-docs/tabletest" "*.md" "Markdown" 11
echo -e "${GREEN}✓ Markdown files generated${NC}"

# Step 5: Verify custom expectation pattern in YAML
echo -e "\n${YELLOW}[5/10] Verifying custom expectation pattern...${NC}"
YAML_FILE=$(find target/junit-jupiter -path "*CustomExpectationPatternTest*" -name "*.yaml" -exec grep -l '"headers"' {} \; | head -1)
if [ -z "$YAML_FILE" ]; then
    echo -e "${RED}✗ Custom expectation pattern YAML file not found${NC}"
    exit 1
fi

if grep -q '"expectation"' "$YAML_FILE" && grep -q '"Expected Result"' "$YAML_FILE"; then
    echo -e "${GREEN}✓ Custom expectation pattern captured in YAML${NC}"
else
    echo -e "${RED}✗ Custom expectation pattern not found in YAML${NC}"
    echo "YAML content:"
    cat "$YAML_FILE"
    exit 1
fi

# ==================================================================
# Pass 2: junit-platform.properties with custom output dir
# ==================================================================
echo -e "\n--- Pass 2: junit-platform.properties ---"

# Step 6: Create junit-platform.properties with custom output dir
echo -e "\n${YELLOW}[6/10] Creating junit-platform.properties...${NC}"
mkdir -p "$(dirname "$PROPERTIES_FILE")"
cat > "$PROPERTIES_FILE" << 'EOF'
junit.platform.reporting.output.dir=target/custom-reports
EOF
echo -e "${GREEN}✓ Properties file created${NC}"

# Step 7: Re-run tests (clean to remove convention output)
echo -e "\n${YELLOW}[7/10] Running tests (properties file)...${NC}"
mvn -B clean test || true
echo -e "${GREEN}✓ Tests completed${NC}"

# Step 8: Validate YAML at custom location
echo -e "\n${YELLOW}[8/10] Validating YAML generation at target/custom-reports...${NC}"
validate_yaml_files "target/custom-reports" 10
echo -e "${GREEN}✓ YAML files generated at custom location${NC}"

# Step 9: Generate Markdown (discovers input via properties file)
echo -e "\n${YELLOW}[9/10] Generating Markdown reports (properties file discovery)...${NC}"
mvn -B tabletest-reporter:report
echo -e "${GREEN}✓ Markdown generation completed${NC}"

# Step 10: Validate Markdown generation
echo -e "\n${YELLOW}[10/10] Validating Markdown generation...${NC}"
validate_output_files "target/generated-docs/tabletest" "*.md" "Markdown" 11
echo -e "${GREEN}✓ Markdown files generated${NC}"

# Clean up properties file
rm -f "$PROPERTIES_FILE"

echo -e "\n=========================================="
echo -e "${GREEN}SUCCESS: All verification steps passed${NC}"
echo -e "=========================================="
