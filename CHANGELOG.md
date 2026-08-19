# TableTest Reporter Changelog

## [Unreleased]

### Added
- A table wide enough to scroll sideways now says so: the scroll box keeps a visible slim
  scrollbar, and a shaded edge appears on whichever side has more table beyond it. Previously the
  box scrolled silently — on a platform with overlay scrollbars a reader had no way to tell the
  last column on screen was not the last column.
- A feature in the `tabletest-reporter.yaml` `features:` tree can carry a `description`, rendered
  under the feature's title on its own index page the way a test class's `@Description` is. An
  intermediate index page could previously carry only a title, so anything true of a whole group
  of features had to be repeated on every rule beneath it.

### Fixed
- Declaring a custom format with no name is now refused with `Format name cannot be missing`
  instead of a message reading only `name`. The blank-name and leading-dot refusals are
  unchanged.

## [1.3.0] - 2026-07-23
> [!IMPORTANT]
> **Slug generation changed, and some published page names change with it.** A test or class
> name containing a letter with no ASCII form (`ß æ ø ł þ ð œ đ`), a compatibility character
> (`ﬁ`, fullwidth letters, `x²`, `Ⅻ`, `™`, `½`), or a non-Latin script now produces a different
> filename and URL than earlier versions did — those characters used to be dropped, so `Grüße`
> published as `grue` and now publishes as `grusse`. If you already publish your documentation,
> the affected pages move and existing links to them break; regenerate the whole report rather
> than an incremental subset, and expect to update any links you control. Names made only of
> ASCII, and accented letters that already folded to a base letter (`ü ö ä é å ñ`), are
> unaffected — their slugs are byte-for-byte what they were.

### Fixed
- A test named wholly in a non-Latin script no longer produces an empty filename: `Москва` now
  publishes as `москва` rather than as nothing at all, and likewise for Greek, CJK, Devanagari
  and every other script. Such a name keeps its own characters, which browsers percent-encode
  and GitHub Pages serves as UTF-8; a name whose ASCII form is only a number it contained
  (`Москва основана в 1147`) is treated the same way rather than published as `1147`. A name
  with no letters or digits anywhere gets `unnamed-` plus a stable hash, so two of them still
  get two files. Names that already produced a working slug are unaffected.

### Changed
- Compatibility characters now reduce to the characters they stand for instead of being dropped
  from filenames and URLs: `ﬁle ﬂow` becomes `file-flow`, fullwidth `Ｆｕｌｌｗｉｄｔｈ` becomes
  `fullwidth`, `x² area` becomes `x2-area`, `Chapter Ⅻ` becomes `chapter-xii`. A precomposed
  accented ligature (`Ǽgir`) no longer loses its letter either. Names written in a script of
  their own are composed the same way, so halfwidth and fullwidth katakana spellings of one
  name (`ﾃｽﾄ`, `テスト`) give one slug rather than two.
- Latin letters with no ASCII form now appear in filenames and URLs instead of vanishing from
  them: `Grüße` becomes `grusse` where it used to become `grue`, and `ÆØÅ` becomes `aeoa` where
  it used to become `a`. One rule decides the spelling — ligatures expand to their component
  letters (`ß`→`ss`, `æ`→`ae`, `œ`→`oe`), stroked letters fold to their base letter (`ø`→`o`,
  `ł`→`l`, `đ`→`d`, `ð`→`d`), and `þ`→`th` because thorn has no Latin base letter. Letters that
  already folded (`ü ö ä é å ñ`) are untouched, so a name built only from those keeps the exact
  slug it had; a name containing one of the newly spelled-out letters gets a new one.
- The JUnit extension no longer depends on the Slugify library: filename slug generation is
  now built in. Slugify required Java 21, which forced every project documenting its tests to
  run them on a 21+ runtime; the extension now targets Java 17, so a Java 17 project can use
  it on its own test runtime. Slug output is unchanged — the replacement is pinned by the same
  characterisation table, extended with non-ASCII cases, and reproduces the library exactly.
  This also removes Slugify and its SLF4J transitive from the test classpath, so they can no
  longer conflict with versions a project uses itself. The build still requires Java 21+.

### Added
- Multi-module reports: several directories of TableTest output now merge into a single
  spec, so the modules of a multi-module build publish one set of documentation. Maven gains
  a `tabletest-reporter:aggregate` goal that walks the reactor and finds each module's output
  by itself, plus `<inputDirectories>` on the `report` goal for naming them explicitly;
  Gradle gains `inputDirs`, and the CLI accepts a repeated `-i`/`--input`. The report tree
  comes from the test class names, so modules land in one package hierarchy; where two
  modules published the same class the most recent output wins. A listed directory that does
  not exist is skipped with a warning, so a partial build still publishes what it has.
- Report-time publish selection: a `publish` section in `tabletest-reporter.yaml` decides
  which pages the report holds, with `exclude` paths holding a page (and its subtree) back
  and `include` paths re-admitting one below an excluded page, so a single rule table still
  publishes from an otherwise internal class. Paths name pages as the report's URLs do
  (`converting/convert-with`), with `*` for any part of a page name and `**` for any number
  of levels. Selection happens when the report is generated, so what publishes is no longer
  tied to how the suite was tagged or run, and re-curating a spec needs no new test run. A
  feature page left with nothing published under it drops with its pages; a path matching no
  page is logged and skipped. Without the section every table publishes, as before.
- Spec-level metadata via an optional `tabletest-reporter.yaml` in the project directory:
  give the whole spec a real title and intro paragraph on its root index (instead of the
  leaked lowercase package segment like "junit" or "example"), retitle intermediate index
  pages, and set an explicit feature reading order for the top-level sections and their
  children — declared features lead, undeclared siblings follow alphabetically. The file is
  read at report time and applied on top of the generated tree, so a project without it is
  unaffected. Point elsewhere with Maven `<configFile>` / `-Dtabletest.report.configFile`,
  Gradle `configFile`, or the CLI `--config` / `-c` option.
- Every HTML page footer states when the report was generated ("Generated by
  tabletest-reporter · 20 Jul 2026 14:32 UTC"), so a reader can tell whether published
  documentation still tracks the code it came from. The timestamp is stated in UTC and
  carries a machine-readable `<time datetime>` attribute; every page of a run shares the
  one timestamp.

## [1.2.0] - 2026-07-18
### Added
- HTML format marks whitespace-significant literals with IDE-style per-character markers:
  values with leading/trailing whitespace (on any line), tabs, runs of spaces, or pipes
  (e.g. indent expectations, whitespace-only cells, formatted-row values) render in
  monospace with a CSS-drawn dot per significant space and an arrow per tab, so space
  counts and tab-vs-space composition are readable at a glance. Single spaces between
  words stay unmarked, and the value text itself stays unaltered for copy/paste and
  search.
- Built-in `html` output format: self-contained, single-file-per-page living documentation
  (inline CSS/JS, no external references) with autowidth tables, sticky header/first column,
  nested-collection rendering, pass/fail badges and status colouring, collapsible failure
  details, per-page row filter and "failing only" toggle, roles legend, light/dark toggle,
  and a print stylesheet. Relative links throughout make the output tree GitHub Pages-ready.
- HTML index pages roll pass/fail status up the tree: each nav item shows a status dot and
  every index summarises its scenario pass rate ("N of M scenarios broken"/"All passing").
- Every HTML page shows a breadcrumb trail of its ancestor pages (root package → class →
  table), with relative links so the trail works from any subpath.
- Every HTML page has a menu button opening a navigation drawer with the whole-report tree
  (status dots included), the current page highlighted and all links relative to that page.
  The drawer slides in over the content, so tables always get the full page width.
- Whole-report search: a search box in the navigation drawer searches across every page's
  title, description, headers and cell values, listing matching pages (with status dots) to
  jump to. Backed by a single shared `tabletest-search-index.js` written once to the output
  root and linked from every page by a relative prefix, so search works offline (`file://`)
  and from any subpath without external requests.
- Single-file HTML mode (`--single-file` / `-s` on the CLI): assembles the whole report into
  one self-contained `.html` — every table inlined as an anchored section, sidebar navigation
  and search targeting in-page anchors, search index inlined, no sibling assets. Ideal for
  sharing as a release asset, email or ticket attachment. Multi-file stays the default.
### Fixed
- The Gradle `reportTableTests` task now tracks the TableTest YAML files as task inputs even
  when no explicit `inputDir` is configured (default `build/junit-jupiter`, the JUnit output
  dir override, and the `junit-platform.properties` location). Previously the task could stay
  `UP-TO-DATE` — or restore a stale report from the build cache — after new test runs. The
  task is also ordered to run after `Test` tasks when both are requested.
- A table test whose display-name slug equals its class slug (e.g. the same `@DisplayName`
  on both) no longer silently loses one of the two published YAML files: the table file now
  gets a numeric suffix, keeping the class and table files distinct.
- A row whose scenario value is a prefix of another row's scenario (e.g. "Add" and
  "Add negative") no longer absorbs the other row's pass/fail results; and rows with
  duplicated scenario values now get no pass/fail roles (as documented) instead of the
  OR-ed result of all duplicates.
- When the input directory accumulates YAML from several test runs (e.g. a
  `junit.platform.reporting.output.dir` with `{uniqueNumber}`), the report now reflects the
  most recently modified files instead of whichever run's files happened to sort first.
- On Windows, index-page links and single-file anchors used backslashes (the platform file
  separator) and were broken in browsers and Markdown/AsciiDoc renderers; generated links now
  use `/` on every platform.

## [1.1.0] - 2026-04-07
### Changed
- Compatible with tabletest-junit 1.2.1 (array parameter support, quoted map keys)
### Fixed
- Gradle `listFormats` task now supports build caching

## [1.0.1] - 2026-02-23
### Fixed
- `FileSystemException: File name too long` when test methods have long fully qualified parameter type signatures (e.g. overloaded methods with complex types)

## [1.0.0] - 2026-02-16
### Changed
- Migrated to org.tabletest coordinates
- Support `io.nchaugen.tabletest.junit.*` annotations for backwards compatibility

## [0.4.0] - 2026-02-15
### Changed
- Index files now show all levels of nested items by default. Set indexDepth = 1 to restore previous behaviour.
### Added
- Configurable index depth to control how many levels of nested items appear in each index file
- Simplified setup for Gradle: plugin automatically adds `tabletest-reporter-junit` dependency and configures JUnit extension autodetection (Maven continues to require manual setup)
- Support `org.tabletest.junit.*` annotations

## [0.3.2] - 2026-02-03
### Added
- Auto-detection of JUnit output directory from Maven Surefire and Gradle test task configurations
### Changed
- Upgraded Pebble template engine to 4.1.0 (security fix)
- Published YAML files now include additional metadata — YAML files from 0.3.x must be regenerated by re-running tests with the updated JUnit extension
- Report output structure now derived from class/package names in YAML metadata instead of input directory layout
- CLI, Maven plugin, and Gradle plugin now display file count on successful generation
- Empty input directories now show informational message instead of silent success
### Fixed
- AsciiDoc description list nesting now cycles colon delimiters to stay within AsciiDoctor's 4-colon limit (issue #11)
- YAML parsing errors now include file path for easier debugging

## [0.3.1] - 2026-01-22
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


[Unreleased]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-1.3.0...HEAD
[1.3.0]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-1.2.0...tabletest-reporter-1.3.0
[1.2.0]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-1.1.0...tabletest-reporter-1.2.0
[1.1.0]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-1.0.1...tabletest-reporter-1.1.0
[1.0.1]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-1.0.0...tabletest-reporter-1.0.1
[1.0.0]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-0.4.0...tabletest-reporter-1.0.0
[0.4.0]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-0.3.2...tabletest-reporter-0.4.0
[0.3.2]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-0.3.1...tabletest-reporter-0.3.2
[0.3.1]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-0.3.0...tabletest-reporter-0.3.1
[0.3.0]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-0.2.1...tabletest-reporter-0.3.0
[0.2.1]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-0.2.0...tabletest-reporter-0.2.1
[0.2.0]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-0.1.1...tabletest-reporter-0.2.0
[0.1.1]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-0.1.0...tabletest-reporter-0.1.1
[0.1.0]: https://github.com/nchaugen/tabletest-reporter/commits/tabletest-reporter-0.1.0
