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
echo "JUnit 6 Maven with Asciidoctor HTML Test"
echo "=========================================="

# ==================================================================
# Pass 1: Convention fallback (target/junit-jupiter)
# ==================================================================
echo -e "\n--- Pass 1: Convention fallback ---"

# Step 1: Run tests (with intentional failures)
echo -e "\n${YELLOW}[1/11] Running tests (convention fallback)...${NC}"
mvn -B clean test || true
echo -e "${GREEN}✓ Tests completed${NC}"

# Step 2: Validate YAML generation
echo -e "\n${YELLOW}[2/11] Validating YAML generation at target/junit-jupiter...${NC}"
validate_yaml_files "target/junit-jupiter" 9
echo -e "${GREEN}✓ YAML files generated${NC}"

# Step 3: Generate AsciiDoc
echo -e "\n${YELLOW}[3/11] Generating AsciiDoc reports...${NC}"
mvn -B tabletest-reporter:report
echo -e "${GREEN}✓ AsciiDoc generation completed${NC}"

# Step 4: Validate AsciiDoc generation
echo -e "\n${YELLOW}[4/11] Validating AsciiDoc generation...${NC}"
validate_output_files "target/generated-docs/tabletest" "*.adoc" "AsciiDoc" 10
echo -e "${GREEN}✓ AsciiDoc files generated${NC}"

# Step 5: Convert to HTML
echo -e "\n${YELLOW}[5/11] Converting AsciiDoc to HTML...${NC}"
mvn -B asciidoctor:process-asciidoc
echo -e "${GREEN}✓ HTML conversion completed${NC}"

# Step 6: Validate HTML generation
echo -e "\n${YELLOW}[6/11] Validating HTML generation...${NC}"
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
echo -e "\n${YELLOW}[7/11] Verifying CSS classes in HTML files...${NC}"

# Get classpath for running HtmlCssVerifier
CLASSPATH="target/test-classes:$(mvn -B dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)"

# Verify failing-with-scenario (2 passed, 1 failed, 4 scenarios: 3 rows + 1 header)
echo -e "\n  Verifying mixed-results-with-scenario-column.html..."
java -cp "$CLASSPATH" com.example.HtmlCssVerifier \
    "$HTML_DIR/failing-test-with-scenario/mixed-results-with-scenario-column.html" 2 1 4

echo -e "\n${GREEN}✓ All CSS class verifications passed${NC}"

# ==================================================================
# Pass 2: Surefire configurationParameters
# ==================================================================
echo -e "\n--- Pass 2: Surefire configurationParameters ---"

# Step 8: Run tests (with intentional failures)
echo -e "\n${YELLOW}[8/11] Running tests (custom output dir)...${NC}"
mvn -B clean test -Pcustom-output || true
echo -e "${GREEN}✓ Tests completed${NC}"

# Step 9: Validate YAML generation
echo -e "\n${YELLOW}[9/11] Validating YAML generation at target/custom-reports...${NC}"
validate_yaml_files "target/custom-reports" 9
echo -e "${GREEN}✓ YAML files generated${NC}"

# Step 10: Generate AsciiDoc
echo -e "\n${YELLOW}[10/11] Generating AsciiDoc reports (custom output dir)...${NC}"
mvn -B tabletest-reporter:report -Pcustom-output
echo -e "${GREEN}✓ AsciiDoc generation completed${NC}"

# Step 11: Validate AsciiDoc generation
echo -e "\n${YELLOW}[11/11] Validating AsciiDoc generation...${NC}"
validate_output_files "target/generated-docs/tabletest" "*.adoc" "AsciiDoc" 10
echo -e "${GREEN}✓ AsciiDoc files generated${NC}"

echo -e "\n=========================================="
echo -e "${GREEN}SUCCESS: All verification steps passed${NC}"
echo -e "=========================================="
