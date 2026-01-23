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
echo "JUnit Latest with Asciidoctor HTML Test"
echo "=========================================="

# Step 1: Run tests (with intentional failures)
echo -e "\n${YELLOW}[1/7] Running tests (with intentional failures)...${NC}"
mvn clean test || true
echo -e "${GREEN}✓ Tests completed${NC}"

# Step 2: Validate YAML generation
echo -e "\n${YELLOW}[2/7] Validating YAML generation...${NC}"
validate_yaml_files "target/junit-jupiter" 9
echo -e "${GREEN}✓ YAML files generated${NC}"

# Step 3: Generate AsciiDoc
echo -e "\n${YELLOW}[3/7] Generating AsciiDoc reports...${NC}"
mvn tabletest-reporter:report
echo -e "${GREEN}✓ AsciiDoc generation completed${NC}"

# Step 4: Validate AsciiDoc generation
echo -e "\n${YELLOW}[4/7] Validating AsciiDoc generation...${NC}"
validate_output_files "target/generated-docs/tabletest" "*.adoc" "AsciiDoc" 10
echo -e "${GREEN}✓ AsciiDoc files generated${NC}"

# Step 5: Convert to HTML
echo -e "\n${YELLOW}[5/7] Converting AsciiDoc to HTML...${NC}"
mvn asciidoctor:process-asciidoc
echo -e "${GREEN}✓ HTML conversion completed${NC}"

# Step 6: Validate HTML generation
echo -e "\n${YELLOW}[6/7] Validating HTML generation...${NC}"
HTML_DIR="target/generated-html/tabletest"
if [ ! -d "$HTML_DIR" ]; then
    echo -e "${RED}✗ HTML directory not found: $HTML_DIR${NC}"
    exit 1
fi

HTML_COUNT=$(find "$HTML_DIR" -name "*.html" | wc -l | tr -d ' ')
if [ "$HTML_COUNT" -lt 10 ]; then
    echo -e "${RED}✗ Expected at least 10 HTML files but found $HTML_COUNT${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Found $HTML_COUNT HTML files${NC}"

# Verify CSS is embedded in HTML files
if grep -q ".scenario" "$HTML_DIR/failing-test-with-scenario/mixed-results-with-scenario-column.html" && \
   grep -q ".expectation" "$HTML_DIR/failing-test-with-scenario/mixed-results-with-scenario-column.html"; then
    echo -e "${GREEN}✓ CSS styles found embedded in HTML files${NC}"
else
    echo -e "${RED}✗ CSS styles not found in HTML files${NC}"
    exit 1
fi

# Step 7: Verify CSS classes in HTML files
echo -e "\n${YELLOW}[7/7] Verifying CSS classes in HTML files...${NC}"

# Get classpath for running HtmlCssVerifier
CLASSPATH="target/test-classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)"

# Verify failing-with-scenario (2 passed, 1 failed, 4 scenarios: 3 rows + 1 header)
echo -e "\n  Verifying mixed-results-with-scenario-column.html..."
java -cp "$CLASSPATH" com.example.HtmlCssVerifier \
    "$HTML_DIR/failing-test-with-scenario/mixed-results-with-scenario-column.html" 2 1 4

echo -e "\n${GREEN}✓ All CSS class verifications passed${NC}"

echo -e "\n=========================================="
echo -e "${GREEN}SUCCESS: All verification steps passed${NC}"
echo -e "=========================================="
