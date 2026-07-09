# tabletest-reporter

Generates living documentation (AsciiDoc/Markdown) from TableTest run output, via CLI,
Maven plugin, and Gradle plugin. See DESIGN.md (architecture) and TEST-STRATEGY.md.

Tests use TableTest — invoke the `tabletest` plugin skill before writing or converting them.

## Git hooks

Install once: `cp git-hooks/* .git/hooks/ && chmod +x .git/hooks/*`

- **pre-commit** — Spotless (`mvn spotless:apply` / `./gradlew spotlessApply`), then build
  + unit tests. Formatting is automatic; don't format by hand. CI runs `mvn spotless:check`.
- **commit-msg** — rejects non-conventional-commit messages or a first line ≥ 50 chars.
- **pre-push** — Spotless + `make build`.

Amend freely before pushing (e.g. to fold in hook-generated copyright headers); never
amend or rewrite pushed history.

## Compatibility tests

`compatibility-tests/run-tests.sh` verifies the reporter across JUnit/Spring Boot/Quarkus
versions: extension autodetection, YAML generation, CLI/Maven/Gradle doc generation,
AsciiDoc/Markdown output.

Run before committing changes to any of `tabletest-reporter-{junit,core,cli,maven-plugin,gradle-plugin}`,
the YAML format, the Pebble templates, extension detection/config, public APIs, or the
JUnit/Spring/Quarkus version ranges; also for non-trivial refactoring in those modules.
Skip for docs-only, CI-workflow, and tooling changes.

## Feature docs

Before committing a user-facing feature: add a CHANGELOG.md `[Unreleased]` entry
(`### Added` / `### Changed` / `### Fixed`; skip internal refactors) and README.md
usage/config examples.
