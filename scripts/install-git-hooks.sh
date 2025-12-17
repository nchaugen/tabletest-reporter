#!/bin/bash
# Install git hooks for tabletest-reporter

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
HOOKS_DIR="$PROJECT_ROOT/.git/hooks"

# Colours
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "Installing git hooks for tabletest-reporter..."
echo ""

# Backup existing beads hooks if they exist
if [ -f "$HOOKS_DIR/pre-commit" ] && ! [ -f "$HOOKS_DIR/pre-commit.beads" ]; then
    echo "Backing up existing pre-commit hook..."
    cp "$HOOKS_DIR/pre-commit" "$HOOKS_DIR/pre-commit.beads"
fi

if [ -f "$HOOKS_DIR/pre-push" ] && ! [ -f "$HOOKS_DIR/pre-push.beads-original" ]; then
    echo "Backing up existing pre-push hook..."
    cp "$HOOKS_DIR/pre-push" "$HOOKS_DIR/pre-push.beads-original"
fi

# Install pre-commit hook (quick build check)
cat > "$HOOKS_DIR/pre-commit" << 'EOF'
#!/bin/bash
# Pre-commit hook: bd sync + quick build check

set -e

# Colours for output
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

echo "Running pre-commit checks..."

# 1. Run beads sync first
if [ -f .git/hooks/pre-commit.beads ]; then
    bash .git/hooks/pre-commit.beads
fi

# 2. Build and run unit tests
echo ""
echo "Building and running tests..."
if ! mvn clean install -q; then
    echo -e "${RED}✗ Build or tests failed${NC}"
    echo "Fix the errors before committing"
    exit 1
fi
echo -e "${GREEN}✓ Build and tests successful${NC}"

echo -e "${GREEN}✓ Pre-commit checks passed${NC}"
echo ""
echo "Reminder: Compatibility tests will run before push"
exit 0
EOF

chmod +x "$HOOKS_DIR/pre-commit"
echo -e "${GREEN}✓${NC} Installed pre-commit hook (build check)"

# Install pre-push hook (compatibility tests)
cat > "$HOOKS_DIR/pre-push" << 'EOF'
#!/bin/bash
# Pre-push hook: compatibility tests + beads sync check

set -e

# Colours for output
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

echo "Running pre-push checks..."

# 1. Run beads pre-push checks if they exist
if [ -f .git/hooks/pre-push.beads-original ]; then
    bash .git/hooks/pre-push.beads-original "$@"
fi

# 2. Run compatibility tests
echo ""
echo "Running compatibility tests..."
cd compatibility-tests
if ! bash run-tests.sh; then
    echo ""
    echo -e "${RED}✗ Compatibility tests failed${NC}"
    echo "Fix the tests before pushing, or use 'git push --no-verify' to skip"
    exit 1
fi
cd ..

echo ""
echo -e "${GREEN}✓ All pre-push checks passed${NC}"
exit 0
EOF

chmod +x "$HOOKS_DIR/pre-push"
echo -e "${GREEN}✓${NC} Installed pre-push hook (compatibility tests)"

echo ""
echo -e "${GREEN}Git hooks installed successfully!${NC}"
echo ""
echo "What happens now:"
echo "  • Pre-commit: Quick build check (~5 seconds)"
echo "  • Pre-push: Full compatibility tests (~30 seconds)"
echo ""
echo "To skip hooks: Use --no-verify flag"
echo "  git commit --no-verify"
echo "  git push --no-verify"
