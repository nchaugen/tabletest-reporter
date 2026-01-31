# TableTest Reporter Changelog

## [Unreleased]

### Added
- Auto-detection of JUnit output directory from Maven Surefire and Gradle test task configurations

### Changed
- CLI, Maven plugin, and Gradle plugin now display file count on successful generation
- Empty input directories now show informational message instead of silent success

### Fixed
- YAML parsing errors now include file path for easier debugging

## [0.3.1]
### Changed
- `.passed`/`.failed` roles now only applied to tables with a scenario column as row and test results correlation is otherwise not possible

### Fixed
- AsciiDoc index-to-index links now generate as proper file paths instead of anchor references in HTML output
- Empty index files no longer generated for test classes without TableTest methods
- Scenario names containing parentheses now match correctly (previously truncated at first opening parenthesis)
- Error messages in failed rows now properly separated from closing delimiter with newline (affects both AsciiDoc and Markdown)

## [0.3.0] - 2025-12-21
### Added
- Custom output format support – define formats like HTML, XML, JSON via templates
- Support for both extension (child templates) and complete template replacement of built-in templates
- Template extension blocks (frontMatter, title, description, table/contents, footer) for customisation
- New runner options to specify custom template directory and to list all available output formats

## [0.2.1] - 2025-12-19
### Added
- Configurable expectation column pattern via `tabletest.reporter.expectation.pattern` configuration parameter
### Fixed
- Parameter types no longer included in the test title generated from the method name
- Passed/failed roles now added correctly when scenario name is null or empty string 

## [0.2.0] - 2025-12-15
### Added
- JUnit extension to collect TableTest report data during test runs (tabletest-reporter-junit)
- Multiple roles supported per cell in published YAML
- Roles added to signal if a row passed or failed
- CamelCase and snake_case aware slugified YAML file name generation
- Human-readable titles for test classes and methods without `@DisplayName` annotation
- Test class and package index pages rendered with proper title of child pages 
### Changed
- YAML files prefixed with `TABLETEST-` to avoid conflicts with other YAML files
- Output file name for TableTest methods either explicit `@DisplayName` or method name (without parameters)

## [0.1.1] - 2025-12-09
### Fixed
- Added project name to tabletest-reporter-core module

## [0.1.0] - 2025-12-09
### Added
- Core TableTest reporting functionality
- Support for AsciiDoc and Markdown output formats
- Template-based rendering using Pebble template engine
- Slugified output directories and filenames


[Unreleased]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-0.3.1...HEAD
[0.3.1]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-0.3.0...tabletest-reporter-0.3.1
[0.3.0]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-0.2.1...tabletest-reporter-0.3.0
[0.2.1]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-0.2.0...tabletest-reporter-0.2.1
[0.2.0]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-0.1.1...tabletest-reporter-0.2.0
[0.1.1]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-0.1.0...tabletest-reporter-0.1.1
[0.1.0]: https://github.com/nchaugen/tabletest-reporter/commits/tabletest-reporter-0.1.0
