**Note**: This project uses [bd (beads)](https://github.com/steveyegge/beads)
for issue tracking. Use `bd` commands instead of markdown TODOs.

See [AGENTS.md](AGENTS.md) for:
- How to use bd properly

See [README.md](README.md) for project context

# Developer Setup

## Git Hooks Installation

This project uses git hooks for automated formatting and quality checks. Install them with:

```bash
cp git-hooks/* .git/hooks/
chmod +x .git/hooks/*
```

The pre-commit hook automatically:
1. Runs `bd sync` (if beads is configured)
2. **Formats Java code with Spotless** (Maven and Gradle modules)
3. Builds project and runs unit tests

**Code formatting is automatic** - the hook runs `mvn spotless:apply` before each commit, so you don't need to manually format code.

## Code Formatting

This project uses **Spotless** with **Palantir Java Format** for consistent code style:

- **Maven modules**: Formatted automatically by pre-commit hook via `mvn spotless:apply`
- **Gradle plugin**: Formatted automatically by pre-commit hook via `./gradlew spotlessApply`
- **Manual formatting**: Run `mvn spotless:apply` or `./gradlew spotlessApply` in respective directories
- **Check formatting**: Run `mvn spotless:check` (used in CI)

The pre-commit hook ensures all committed code is properly formatted.

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

## Table-based Tests
Prefer using TableTest-style JUnit tests where possible. You should have a skill `tabletest` you can leverage.

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

## TableTest Consolidation Patterns
When multiple test methods follow the same structure but vary only in inputs/outputs, consolidate them into a TableTest:

**Good candidates for consolidation:**
- Multiple tests with identical setup and assertion logic
- Tests varying only in input data and expected outcomes
- Validation tests checking different scenarios

**Keep as separate @Test methods:**
- Edge cases with null/empty inputs
- Tests requiring complex setup (e.g., creating subdirectories)
- Tests with fundamentally different assertion logic

**TableTest best practices:**
- Use `List<String>` for file lists: `[file1.txt, file2.txt]`
- Use path notation for subdirectories: `[subdir/file.txt]`
- Include `@Scenario String _scenario` when using `@TempDir` (parameter shift issue)
- Create parent directories: `Files.createDirectories(filePath.getParent())`

Example:
```java
@TableTest("""
    Scenario          | Files                    | Expected
    Single file       | [file.txt]               | [file]
    Multiple files    | [a.txt, b.txt]           | [a, b]
    Subdirectory      | [dir/file.txt]           | []
    """)
void discovers_files(@Scenario String _scenario, List<String> files, List<String> expected,
                    @TempDir Path tempDir) throws IOException {
    for (String file : files) {
        Path path = tempDir.resolve(file);
        Files.createDirectories(path.getParent());
        Files.writeString(path, "content");
    }
    // assertions...
}
```

# Commit and Push Workflow

**CRITICAL**: You must NEVER commit or push changes without explicit user approval.

## Git Safety Rules - NEVER VIOLATE THESE

🚨 **ABSOLUTE PROHIBITIONS** 🚨
- **NEVER** use `git commit --amend` on commits already pushed to remote
- **NEVER** use `git push --force` or `git push --force-with-lease` to main/master
- **NEVER** rewrite history on main/master branch
- **NEVER** commit or push without explicit user approval

**Amending commits is SAFE and ENCOURAGED before pushing** (e.g., to include hook-generated changes like copyright headers).

If you need to fix a commit message after pushing:
1. Create a NEW commit with the fix (e.g., "docs: fix commit message format")
2. Push normally with `git push`

## Git Commit Message Format

**CRITICAL: The commit-msg hook will reject commits that violate these rules**

- **Use conventional commits** (feat:, fix:, docs:, refactor:, test:, chore:, etc.)
- **Keep first line under 50 characters**
- **Keep messages concise** - single line preferred, no multi-line explanations
- **Focus on what, not why** - the code diff shows the why
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
3. **Update documentation for features**: See "Documentation Updates" section below
4. **Show what changed**: Use `git diff` to show the user what you've changed
5. **Explain the changes**: Briefly explain what you did and why
6. **Wait for approval**: Ask "Should I commit and push these changes?"
7. **Only after user says yes**:
   - Run `git add` and `git commit` (the commit-msg hook will validate format)
   - Run `git push` (NOT force push)
   - If commit is rejected by hook, fix and create NEW commit (don't amend if already pushed)

## Documentation Updates for Features

When adding new features, update documentation BEFORE asking for commit approval:

1. **CHANGELOG.md** - Add user-facing changes to `[Unreleased]` section:
   - `### Added` for new features visible to users
   - `### Changed` for breaking changes or significant behaviour changes
   - `### Fixed` for bug fixes
   - Skip internal refactorings unless they affect users

2. **README.md** - Add examples and usage instructions:
   - Add new sections for major features
   - Update configuration examples
   - Include code examples showing how users will use the feature

**Documentation-only changes** do not require running compatibility tests.

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

The project has compatibility tests in `compatibility-tests/` that verify TableTest Reporter works across different frameworks and versions (JUnit, Spring Boot, Quarkus). These tests validate JUnit extension autodetection, YAML generation, documentation generation (CLI, Maven plugin, and Gradle plugin), and output formats (AsciiDoc/Markdown).

## When Compatibility Tests MUST Be Run

Run `./compatibility-tests/run-tests.sh` before committing when making:

- **Changes to core modules:**
  - `tabletest-reporter-junit` (JUnit extension, YAML generation, autodetection)
  - `tabletest-reporter-core` (report generation, template processing, Pebble filters)
  - `tabletest-reporter-cli` (CLI functionality)
  - `tabletest-reporter-maven-plugin` (Maven plugin functionality)
  - `tabletest-reporter-gradle-plugin` (Gradle plugin functionality)

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
