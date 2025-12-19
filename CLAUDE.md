**Note**: This project uses [bd (beads)](https://github.com/steveyegge/beads)
for issue tracking. Use `bd` commands instead of markdown TODOs.

See [AGENTS.md](AGENTS.md) for:
- How to use bd properly
- TableTest patterns

See [README.md](README.md) for project context

# Code Quality Expectations

This section highlights code quality patterns specific to this project. See ~/.claude/CLAUDE.md for general coding principles (Single Responsibility, TDD, immutability, etc.).

## Resource Management
- Always use try-with-resources for `AutoCloseable` resources (streams, files, etc.)
- Example: `Files.list()`, `Files.walk()`, database connections
```java
// Good
try (var files = Files.list(outputDir)) {
    return files.filter(...).findFirst().orElseThrow();
}

// Bad - resource leak
return Files.list(outputDir).filter(...).findFirst().orElseThrow();
```

## Method Extraction
- Each method should do ONE thing with a clear, singular purpose
- No combined operations (avoid names like `setupAndRun`, `validateAndProcess`)
- Extract setup/utility logic into focused helper methods
```java
// Good
Path inputDir = setupInputDirectory(tempDir);
Path templateDir = setupCustomTemplateDirectory(tempDir);
int exitCode = runCli(args);

// Bad - combined operations
Path dirs = setupDirectoriesAndRunCli(tempDir, args);
```

## Test Structure
- Keep assertions visible in test methods (don't hide them in helpers)
- Extract repetitive setup into helper methods
- Helper methods should return values for test methods to assert on
```java
// Good
Path generatedFile = findGeneratedFile(outputDir);
String content = Files.readString(generatedFile);
assertThat(content).contains("expected value");

// Bad - assertion hidden in helper
verifyGeneratedFileContains(outputDir, "expected value");
```

## Project-Specific Patterns
- Prefer TableTest for parameterised tests (see global CLAUDE.md)
- Use meaningful test names describing behaviour (e.g., `uses_custom_template_when_template_dir_provided`)

# Commit and Push Workflow

**CRITICAL**: You must NEVER commit or push changes without explicit user approval.

## Git Safety Rules - NEVER VIOLATE THESE

🚨 **ABSOLUTE PROHIBITIONS** 🚨
- **NEVER** use `git commit --amend` on commits already pushed to remote
- **NEVER** use `git push --force` or `git push --force-with-lease` to main/master
- **NEVER** rewrite history on main/master branch
- **NEVER** commit or push without explicit user approval

If you need to fix a commit message after pushing:
1. Create a NEW commit with the fix (e.g., "docs: fix commit message format")
2. Push normally with `git push`

## Git Commit Message Format

**CRITICAL: The commit-msg hook will reject commits that violate these rules**

- **Use conventional commits** (feat:, fix:, docs:, refactor:, test:, chore:, etc.)
- **Keep first line under 50 characters**
- **NEVER add Claude Code attribution footer** - omit all AI attribution
- First line should be imperative mood (e.g., "fix bug" not "fixed bug")

Good examples:
```
feat: add template directory option to CLI
fix: resolve resource leak in file stream
docs: update compatibility testing guide
refactor: extract helper methods in CLI tests
```

Bad examples:
```
feat: add --template-dir option to CLI with validation  (TOO LONG - 59 chars)
Added new feature  (wrong: not conventional commits format)
fix: resolve bug

🤖 Generated with Claude Code  (NEVER DO THIS - hook will reject)
```

## Workflow Steps

1. **Make changes** as requested by the user
2. **Run compatibility tests if required**: Check the "Compatibility Testing Requirements" section below to determine if `./compatibility-tests/run-tests.sh` must be run before committing
3. **Show what changed**: Use `git diff` to show the user what you've changed
4. **Explain the changes**: Briefly explain what you did and why
5. **Wait for approval**: Ask "Should I commit and push these changes?"
6. **Only after user says yes**:
   - Run `git add` and `git commit` (the commit-msg hook will validate format)
   - Run `git push` (NOT force push)
   - If commit is rejected by hook, fix and create NEW commit (don't amend if already pushed)

## Example Sessions

**Example 1: Change requiring compatibility tests**
```
User: Fix the YAML generation bug in tabletest-reporter-junit
Assistant: [makes changes]
Assistant: I've fixed the bug. Running compatibility tests since this affects core YAML generation...
[runs ./compatibility-tests/run-tests.sh]
Assistant: All compatibility tests passed. Here are the changes:
[shows git diff output]
Should I commit and push these changes?

User: yes
Assistant: [commits and pushes]
```

**Example 2: Documentation change (no compatibility tests needed)**
```
User: Update the README with better examples
Assistant: [makes changes]
Assistant: I've updated the README. Here are the changes:
[shows git diff output]
(No compatibility tests needed for documentation-only changes)
Should I commit and push these changes?

User: yes
Assistant: [commits and pushes]
```

## Never Do This

- Do NOT commit immediately after making changes
- Do NOT push without showing the user what changed
- Do NOT assume the user wants changes committed
- Do NOT skip the approval step

# Compatibility Testing Requirements

The project has compatibility tests in `compatibility-tests/` that verify TableTest Reporter works across different frameworks and versions (JUnit, Spring Boot, Quarkus). These tests validate JUnit extension autodetection, YAML generation, documentation generation (CLI and Maven plugin), and output formats (AsciiDoc/Markdown).

## When Compatibility Tests MUST Be Run

Run `./compatibility-tests/run-tests.sh` before committing when making:

- **Changes to core modules:**
  - `tabletest-reporter-junit` (JUnit extension, YAML generation, autodetection)
  - `tabletest-reporter-core` (report generation, template processing, Pebble filters)
  - `tabletest-reporter-cli` (CLI functionality)
  - `tabletest-reporter-maven-plugin` (Maven plugin functionality)

- **API or format changes:**
  - YAML file format changes
  - Template changes affecting AsciiDoc or Markdown output
  - Extension configuration or detection mechanism changes
  - Public API changes in any module

- **Dependency version updates:**
  - JUnit version changes
  - Spring Boot version range changes
  - Quarkus version range changes
  - Major dependency updates in core modules

- **Build configuration affecting tests:**
  - Parent POM changes affecting test execution
  - Surefire plugin configuration patterns
  - Framework integration changes

## When Compatibility Tests Are Recommended

Consider running compatibility tests for:

- Non-trivial refactoring within core modules
- Changes that might affect runtime behaviour
- Changes to error handling or edge cases

## When Compatibility Tests Are NOT Needed

Skip compatibility tests for:

- Documentation-only changes (README, CHANGELOG, CLAUDE.md, AGENTS.md)
- CI/CD workflow changes (`.github/workflows/`)
- Development tooling (`.gitignore`, `.idea/`, `scripts/`)
- Beads issue tracking changes (`.beads/`)
- Gradle plugin changes (not yet covered by compatibility tests)
