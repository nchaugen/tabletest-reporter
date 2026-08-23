# TableTest Reporter Changelog

## [Unreleased]

### Added
- A report can link back to the site that hosts it. Every other link a report holds is relative
  within its own tree, so a reader who arrives from a site has no way back once inside. Declare a
  `site` section with a `label` and a `url` in `tabletest-reporter.yaml` and the link renders at the
  start of the footer of every HTML page — the table page, the index page and the single-file report
  alike. The address is used exactly as written and is never resolved against the report's own tree,
  so a root-relative address works for a site that hosts the spec below it. A `url` declared without
  a `label` labels itself with the address. Without a `site` section the footer is unchanged.
- The three HTML page templates now leave a `footer` block, so a template of yours can replace the
  footer without rewriting the page. The site link also has a `siteLink(site)` macro of its own,
  which such a template can call to place the same link elsewhere.

- Front matter is now a config section, not only a template block. Declare `frontMatter:` in
  `tabletest-reporter.yaml` and every AsciiDoc and Markdown page is written with it above the page —
  a fenced YAML block for Markdown, document attributes for AsciiDoc, and nothing for HTML, which is
  a finished page rather than source for a site generator. Keys keep their declared order, and a
  value is quoted only where YAML would otherwise misread it. Three values the reporter knows are
  asked for by a token in the value rather than by the key's name — `$title`, `$position` and
  `$timestamp` — because site generators do not agree on what to call them: a page's position is
  `weight` to Hugo, `sidebar_position` to Docusaurus, `nav_order` to a Jekyll theme and
  `page-weight` to Antora, which exposes a custom attribute under no other prefix. `$position` is
  the place the `features:` section declares, so a generator ordering pages by it lists them as the
  project curated them instead of alphabetically. The `frontMatter` template block still works and
  still wins, for anyone who wants full control.

### Fixed
- A report now prints the same whichever colour scheme it is viewed in. Printing drops a dark
  background but keeps the text colours, so a reader in dark mode printed pale grey on white — every
  cell, description and breadcrumb, not only the footer. The print stylesheet now replaces the whole
  palette with one meant for paper. This also fixes the page footer, whose screen grey was a 2.2:1
  contrast against white paper even in light mode, so the attribution and the run timestamp read as
  nothing on a printed page.
- A verdict dot now prints. It carries its colour as a background, which a browser drops when
  printing unless asked to keep it, so a printed index tree showed no verdicts at all.
- Choosing dark explicitly now sets every colour. The theme toggle's palette left out `--shade`, so
  a page switched to dark on a light-scheme system kept the light shadow that cues a table can be
  scrolled sideways. The four palettes in the built-in stylesheet are now held to the same set of
  colours by a test, because CSS cannot share one set of values between a media query and a
  selector.

## [1.4.0] - 2026-08-20

### Added
- An HTML template of your own can add to the built-in stylesheet through a new `extra_stylesheet`
  block, without replacing it. A report carries its stylesheet inside the file, so until now a role
  declared with `@ColumnRole` had nowhere to be styled from: the only way to reach the CSS was to
  rewrite the whole sheet. The block is left by `table.html.peb`, `index.html.peb` and
  `single.html.peb` alike.
- `@Tree` marks a column whose cells hold a tree, written as a nested collection. The built-in HTML
  report then opens each level below its parent rather than beside it, with a guide line down the
  level and a connector on each entry. The default map rendering puts a key beside its value, which
  walks a deep tree sideways across the page. The cell value is unchanged, so a reader still meets
  the notation they would write.
- A cell whose set expands its row is now marked `value-set` in the published report. A published
  table shows no parameters, so `{a, b}` reads the same whether it expands the row into one run per
  value or is a `Set` the test receives whole. The reporter tells them apart the way the runtime
  does — a set value against a parameter that is not a set expands — and the built-in HTML
  stylesheet labels the cell "any of". Markdown carries no roles, so the two stay alike there.
- `@Lines` marks a column whose cells hold the lines of one block of text. The parameter receives
  the lines joined by newlines (or the lines themselves, for a `List` parameter), and the HTML
  report renders the cell as a stacked monospace block rather than a bulleted list, so text whose
  alignment is the point reads as it was written. AsciiDoc publishes the role and keeps its bulleted
  list; Markdown is unchanged.
- A space run at the end of a line now carries a `trailing` class alongside `sp`, so a stylesheet can
  tell the one run a whitespace-preserving layout cannot show from the ones it can. The built-in HTML
  stylesheet uses it to drop the markers from alignment padding inside a `lines` column while keeping
  a trailing run marked, and to leave a blank line in such a column unmarked — it is already visible
  as a line of the block. Only the class is new — the marked runs and the characters in them are
  unchanged.
- A test parameter can now declare a role for its column, and the reporter publishes it on every
  cell of that column. Annotate an annotation of your own with `@ColumnRole` and put it on the
  parameter; the role is published as the annotation's simple name in kebab case, or as the token
  `@ColumnRole("...")` names. Published roles reach the HTML report as CSS classes and the AsciiDoc
  report as element roles, so a stylesheet of yours can style a column the reporter knows nothing
  about. `scenario`, `expectation`, `passed` and `failed` are still derived by the reporter itself;
  a declared role is published alongside them without being treated as one.
- A table wide enough to scroll sideways now says so: the scroll box keeps a visible slim
  scrollbar, and a shaded edge appears on whichever side has more table beyond it. Previously the
  box scrolled silently — on a platform with overlay scrollbars a reader had no way to tell the
  last column on screen was not the last column.
- A feature in the `tabletest-reporter.yaml` `features:` tree can carry a `description`, rendered
  under the feature's title on its own index page the way a test class's `@Description` is. An
  intermediate index page could previously carry only a title, so anything true of a whole group
  of features had to be repeated on every rule beneath it.

### Fixed
- A rule page now shows the description of the page it sits under, above its own. The class or
  feature description is where the notation a rule's columns use is explained, and it rendered only
  on the index page — but the sidebar links to rule pages and search returns rule pages, so a reader
  met the columns without the explanation. All three formats show it.
- A description with more than one paragraph now renders as more than one paragraph in HTML. HTML
  collapses a blank line, so every paragraph of a `@Description` ran together into one block. The
  Markdown and AsciiDoc reports were already correct. Line breaks inside a paragraph are still
  dropped, so the text flows to the width of the page rather than to the width of the text block it
  was written in.
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


[Unreleased]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-1.4.0...HEAD
[1.4.0]: https://github.com/nchaugen/tabletest-reporter/compare/tabletest-reporter-1.3.0...tabletest-reporter-1.4.0
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
