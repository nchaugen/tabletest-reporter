# TableTest Reporter Changelog

## [Unreleased]
### Added
- Custom output format support - define formats like HTML, XML, JSON via templates
- Format discovery automatically finds custom formats from template directory
- Helpful error messages listing all available formats (built-in and custom)
- Custom template support with convention-based discovery
- Template extension blocks (frontMatter, title, description, table/contents, footer) for customisation
- Support for both template extension (child templates) and complete template replacement
- CLI `--template-dir` option to specify custom template directory
- CLI `--list-formats` option to list all available output formats
- Maven `list-formats` goal to list all available output formats
- Gradle `listTableTestReportFormats` task to list all available output formats
### Changed
- Renamed `ReportFormat` to `BuiltInFormat` for clarity between built-in and custom formats
- Extracted duplicate format resolution logic into `FormatResolver` utility class

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
