#!/bin/bash

set -e

# Colours for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Colour

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Track results
declare -a PASSED_TESTS
declare -a FAILED_TESTS

echo "=========================================="
echo "TableTest Reporter Compatibility Tests"
echo "=========================================="
echo ""

# Function to run a single test project
run_test() {
    local test_name=$1
    local test_dir="$SCRIPT_DIR/$test_name"

    echo "----------------------------------------"
    echo "Testing: $test_name"
    echo "----------------------------------------"

    if [ ! -d "$test_dir" ]; then
        echo -e "${RED}✗ Test directory not found: $test_dir${NC}"
        FAILED_TESTS+=("$test_name (directory not found)")
        return 1
    fi

    cd "$test_dir"

    # Run the test script if it exists
    if [ -f "test.sh" ]; then
        if bash test.sh; then
            echo -e "${GREEN}✓ $test_name passed${NC}"
            PASSED_TESTS+=("$test_name")
            return 0
        else
            echo -e "${RED}✗ $test_name failed${NC}"
            FAILED_TESTS+=("$test_name")
            return 1
        fi
    else
        echo -e "${RED}✗ test.sh not found in $test_dir${NC}"
        FAILED_TESTS+=("$test_name (test.sh not found)")
        return 1
    fi
}

# Build the main project first to get SNAPSHOT artifacts
echo "Building tabletest-reporter (SNAPSHOT)..."
cd "$PROJECT_ROOT"
if mvn -B clean install -DskipTests; then
    echo -e "${GREEN}✓ Main project built successfully${NC}"
    echo ""
else
    echo -e "${RED}✗ Failed to build main project${NC}"
    exit 1
fi

# Run all compatibility tests
run_test "junit-5-maven"
run_test "junit-6-maven"
run_test "spring-boot-min"
run_test "spring-boot-latest"
run_test "quarkus-min"
run_test "quarkus-latest"
run_test "junit-5-gradle"

# Summary
echo ""
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo ""
echo -e "Passed: ${GREEN}${#PASSED_TESTS[@]}${NC}"
for test in "${PASSED_TESTS[@]}"; do
    echo -e "  ${GREEN}✓${NC} $test"
done

echo ""
echo -e "Failed: ${RED}${#FAILED_TESTS[@]}${NC}"
for test in "${FAILED_TESTS[@]}"; do
    echo -e "  ${RED}✗${NC} $test"
done

echo ""

# Exit with failure if any tests failed
if [ ${#FAILED_TESTS[@]} -gt 0 ]; then
    exit 1
fi

echo -e "${GREEN}All tests passed!${NC}"
exit 0
