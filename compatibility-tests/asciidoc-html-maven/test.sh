#!/bin/bash
set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "AsciiDoc HTML CSS Class Verification Test"
echo "=========================================="

# Step 1: Run tests (with intentional failures)
echo -e "\n${YELLOW}[1/8] Running tests (with intentional failures)...${NC}"
mvn clean test || true
echo -e "${GREEN}✓ Tests completed${NC}"

# Step 2: Validate YAML generation
echo -e "\n${YELLOW}[2/8] Validating YAML generation...${NC}"
YAML_DIR="target/junit-jupiter"
if [ ! -d "$YAML_DIR" ]; then
    echo -e "${RED}✗ YAML directory not found: $YAML_DIR${NC}"
    exit 1
fi

YAML_COUNT=$(find "$YAML_DIR" -name "TABLETEST-*.yaml" | wc -l | tr -d ' ')
if [ "$YAML_COUNT" -ne 4 ]; then
    echo -e "${RED}✗ Expected 4 YAML files (1 for class + 3 for test methods) but found $YAML_COUNT${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Found $YAML_COUNT YAML files${NC}"

# Step 3: Generate AsciiDoc
echo -e "\n${YELLOW}[3/8] Generating AsciiDoc reports...${NC}"
mvn tabletest-reporter:report
echo -e "${GREEN}✓ AsciiDoc generation completed${NC}"

# Step 4: Validate AsciiDoc generation
echo -e "\n${YELLOW}[4/8] Validating AsciiDoc generation...${NC}"
ADOC_DIR="target/generated-docs/tabletest"
if [ ! -d "$ADOC_DIR" ]; then
    echo -e "${RED}✗ AsciiDoc directory not found: $ADOC_DIR${NC}"
    exit 1
fi

ADOC_COUNT=$(find "$ADOC_DIR" -name "*.adoc" | wc -l | tr -d ' ')
if [ "$ADOC_COUNT" -lt 4 ]; then
    echo -e "${RED}✗ Expected at least 4 AsciiDoc files (index + 3 test files) but found $ADOC_COUNT${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Found $ADOC_COUNT AsciiDoc files${NC}"

# Step 5: Convert to HTML
echo -e "\n${YELLOW}[5/8] Converting AsciiDoc to HTML...${NC}"
mvn asciidoctor:process-asciidoc
echo -e "${GREEN}✓ HTML conversion completed${NC}"

# Step 6: Validate HTML generation
echo -e "\n${YELLOW}[6/8] Validating HTML generation...${NC}"
HTML_DIR="target/generated-html/tabletest"
if [ ! -d "$HTML_DIR" ]; then
    echo -e "${RED}✗ HTML directory not found: $HTML_DIR${NC}"
    exit 1
fi

HTML_COUNT=$(find "$HTML_DIR" -name "*.html" | wc -l | tr -d ' ')
if [ "$HTML_COUNT" -lt 4 ]; then
    echo -e "${RED}✗ Expected at least 4 HTML files (index + 3 test files) but found $HTML_COUNT${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Found $HTML_COUNT HTML files${NC}"

# Step 7: Verify CSS is embedded in HTML files
echo -e "\n${YELLOW}[7/8] Verifying CSS is embedded in HTML files...${NC}"
# Check if CSS classes are defined in the HTML files
if grep -q ".scenario" "$HTML_DIR/calculator-tests/addition.html" && grep -q ".expectation" "$HTML_DIR/calculator-tests/addition.html"; then
    echo -e "${GREEN}✓ CSS styles found embedded in HTML files${NC}"
else
    echo -e "${RED}✗ CSS styles not found in HTML files${NC}"
    exit 1
fi

# Step 8: Verify CSS classes in HTML files
echo -e "\n${YELLOW}[8/8] Verifying CSS classes in HTML files...${NC}"

# Get classpath for running HtmlCssVerifier
CLASSPATH="target/test-classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout)"

# Verify addition.html (3 passed, 0 failed, 4 scenarios: 3 rows + 1 header)
echo -e "\n  Verifying addition.html..."
java -cp "$CLASSPATH" com.example.HtmlCssVerifier \
    "$HTML_DIR/calculator-tests/addition.html" 3 0 4

# Verify subtraction.html (2 passed, 2 failed, 5 scenarios: 4 rows + 1 header)
echo -e "\n  Verifying subtraction.html..."
java -cp "$CLASSPATH" com.example.HtmlCssVerifier \
    "$HTML_DIR/calculator-tests/subtraction.html" 2 2 5

# Verify multiplication.html (0 passed, 2 failed, 3 scenarios: 2 rows + 1 header)
echo -e "\n  Verifying multiplication.html..."
java -cp "$CLASSPATH" com.example.HtmlCssVerifier \
    "$HTML_DIR/calculator-tests/multiplication.html" 0 2 3

echo -e "\n${GREEN}✓ All CSS class verifications passed${NC}"

echo -e "\n=========================================="
echo -e "${GREEN}SUCCESS: All verification steps passed${NC}"
echo -e "=========================================="
