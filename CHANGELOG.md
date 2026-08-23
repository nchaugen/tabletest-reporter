# TableTest Reporter Changelog

## [Unreleased]

### Added
- You can now read an HTML report from the keyboard. Tab reaches every row of the navigation,
  Enter follows one, and Space opens or closes a feature. The browser does all of that, and no
  script runs. These keys are new on top of it:
  - <kbd>↓</kbd> <kbd>↑</kbd> move between the rows you can see
  - <kbd>→</kbd> opens a feature, then steps into it
  - <kbd>←</kbd> closes a feature, or steps out to the one above
  - <kbd>/</kbd> opens the drawer and jumps to the search box
  - <kbd>m</kbd> opens or closes the navigation drawer
  - <kbd>Esc</kbd> leaves the search box, or closes the drawer
  - <kbd>?</kbd> lists the keys on the page

  The arrow keys act only on a row that already holds focus. They still scroll a long page
  everywhere else.
- Your build can now pin the instant a report states it was generated at. Set `generatedAt` on the
  Maven plugin (`-Dtabletest.report.generatedAt`) or the Gradle extension, or pass `--generated-at`
  on the CLI. The value is an ISO-8601 instant, and any other value fails the build.

  Leave it unset and the reporter reads the clock, as before. Two runs of the same tests then write
  two different pages, and a build that compares its own output sees a change every time. A pinned
  instant gives you the same bytes from the same tests. The reporter does not read
  `SOURCE_DATE_EPOCH` itself, because a report that changes with its environment is the fault being
  fixed. Convert that value in your build and pass it in.
- A report can now link back to the site that hosts it. Every other link a report holds stays
  inside the report's own tree, so a reader who arrives from a site has no way back. Declare a
  `site` section with a `label` and a `url` in `tabletest-reporter.yaml`. The link then opens the
  footer of every HTML page: the table page, the index page and the single-file report alike.

  The reporter uses the address exactly as you wrote it, and never resolves it against the
  report's own tree. A root-relative address therefore works for a site that hosts the spec below
  one of its own paths. A `url` without a `label` labels itself with the address. Without a `site`
  section the footer does not change.
- The three HTML page templates now leave a `footer` block, so a template of yours can replace the
  footer without rewriting the page. The site link also has a `siteLink(site)` macro of its own,
  which such a template can call to place the same link elsewhere.
- Front matter is now a config section, and not only a template block. Declare `frontMatter:` in
  `tabletest-reporter.yaml`. The reporter then writes it above every AsciiDoc and Markdown page: a
  fenced YAML block for Markdown, and document attributes for AsciiDoc. An HTML page carries none,
  because it is a finished page and not source for a site generator.

  Keys keep the order you declared them in. The reporter quotes a value only where YAML would
  otherwise read it back as something else.

  Ask for one of three values by writing a token as the **value**, never as the key: `$title`,
  `$position` or `$timestamp`. Site generators do not agree on what to call a page's position.
  Hugo calls it `weight`, Docusaurus `sidebar_position`, a Jekyll theme `nav_order`, and Antora
  `page-weight`. `$position` is the place the `features:` section declares, so a generator that
  orders pages by it lists them as you curated them, and not alphabetically.

  The `frontMatter` template block still works, and still wins, for anyone who wants full control.
- `@NamedLines` marks a column whose cells hold several blocks of text, each under a name. Write
  the cell as a map from name to lines. A file and its contents is the case it was built for, so
  such a cell reads as a small directory. The HTML report draws each name as a caption over its own
  block, indented behind a margin rule. It styles the blocks exactly as a `@Lines` cell, because
  that is what they are.

  There is no converter: the parameter is a plain `Map<String, List<String>>`. Note that TableTest
  never converts a map key, so `Map<Path, List<String>>` compiles and then fails on first read.
  Resolve the name where you write the files.
- `@Numbered` numbers the lines of a block in the HTML report. It is a role of its own, so ask for
  it beside `@Lines` or `@NamedLines`. It is off unless you ask for it: on a block of two or three
  lines the digits are as wide as the text beside them.

### Changed
- The README's manual setup instructions are a section of their own, beside the Gradle and Maven
  ones. They sat inside the Maven section, folded away, although they cover Maven, Gradle and the
  CLI alike — a Gradle reader following the Gradle instructions never met the Gradle option they
  hold.
- Each built-in format has a section of the README to itself. AsciiDoc and Markdown are slim beside
  HTML, which is the honest picture — the format tiering gives presentation to HTML alone — and
  each now says what its own format carries: where a collection goes, how a pipe and significant
  whitespace are written, what a column role becomes, and how front matter reaches the page.
- The README's Formats section is regrouped. Choosing a format and listing the available ones sit
  together, because both answer "which format?". Defining a format of your own moved in with the
  templates, because a format *is* a pair of templates — name the pair after a format that exists
  and you replace it, name it after one that does not and you have defined a format. The HTML
  section is renamed "What the HTML report gives you", so it no longer implies AsciiDoc and
  Markdown siblings that would be stubs: what those two formats carry is already the substance of
  Choosing a format.
- The README section on styling now says which format it serves. It was called "Styling HTML
  Reports", but every word of it is about the `asciidoc` format after Asciidoctor has converted it
  — a reader on the built-in `html` format would have configured a plugin that has no bearing on
  their output. It is now "Styling an AsciiDoc report once it is HTML", sits with the formats, and
  opens by pointing anyone on the built-in format at `extra_stylesheet` instead.
- Column roles are a section of the README in their own right, and no longer a subsection of
  writing tests. They change how the HTML report draws a column, so they sit with the presentation
  material. The section now also shows how to build a rendering of your own: the built-in roles
  have no privileged path — the reporter draws a value from its shape and puts the column's roles
  on the cell as CSS classes, so `@Lines`, `@Tree`, `@NamedLines` and `@Numbered` are each an
  annotation plus a stylesheet rule. A worked example declares an annotation and styles it through
  the `extra_stylesheet` block, which the README mentioned but never showed in use.
- The README is reorganised. The numbered walkthrough used to dissolve at "Step 4", which had
  absorbed ten subsections of configuration reference and no fifth step. Getting started is now
  four steps and stops there. The four sections of `tabletest-reporter.yaml` sit together under
  the file they belong to, with the build's own options beside them rather than among them.
  Choosing a format, the HTML report and custom formats are one section instead of three places.
  "Advanced Topics" is gone, replaced by named sections. Two pairs of headings both called Maven
  and Gradle no longer collide over the same anchors, and the file opens with a table of contents.
- The README no longer opens with the notice about the move to `org.tabletest` coordinates. The
  move shipped in 1.0.0, five releases back, and the relocation POMs on Maven Central still point
  anyone on the old coordinates at the new ones.
- An HTML index page and the sidebar now fold. The page still holds every level below it, but only
  the top level is open. An entry that holds pages carries a chevron, and the reader opens the part
  they want. You no longer choose one depth for every reader.

  The fold is a plain `<details>`, so it needs no script. A folded entry stays in the page: a
  browser search finds it, and a printed copy shows every level open. In the sidebar, the branches
  on the trail to the page you are on arrive open. A reader following a deep link therefore sees
  where they are.

  `indexDepth` does not change. It stays the coarse override for writing fewer levels at all, which
  a Markdown or AsciiDoc index still needs.
- A row of a link tree carries its state on the link itself. The type, the verdict and the current
  page used to sit on the list item around it, and a fold puts that link one level deeper. A
  stylesheet of your own that targeted the navigation tree needs updating; the documented cell-role
  classes — `.scenario`, `.expectation`, `.passed` and `.failed` — do not change.
- A row of a link tree now reads as one thing. The chevron is grey, and the verdict dot beside it
  keeps its colour. You can therefore tell the control you operate from the verdict next to it. The `–`
  before a rule is gone, because the empty chevron column already tells a rule from a feature. The
  dot now sits on a row's first line; a wrapped title used to leave it centred between two.
- The sidebar now marks the whole trail down to the page you are on, and not the page alone. A rule
  deep in a spec highlighted one leaf, and nothing said which feature held it. Every entry above the
  page now reads at full ink, and an accent line runs beside its children.
- An index page states a passing verdict in the same words a table page does: "All 4 scenarios
  hold", where it used to say "passing". A reader moving from an index to a table page met the same
  fact stated two ways.
- A `@Tree` column now draws one connected tree, and not two overlapping ones. Every level used to
  show two competing verticals, and no corner met the line it belonged to. Each entry now owns the
  whole structure. It draws a stem the height of what sits under it, and a tick meeting its own
  text. On the last entry the stem stops at the tick, which makes the corner. The reporter draws the
  lines rather than typing them, so they connect exactly and scale with the font.

### Fixed
- A closed navigation drawer is no longer in the tab order. The drawer sits off-screen, but a
  reader tabbing through a page still walked the whole hidden menu first. That took six stops on
  the reporter's own spec, before anything they could see. The drawer is now `inert` until it opens.
  Opening it moves focus into it, and closing it hands focus back to the menu button. While the
  drawer is open, the page behind it is inert in turn.
- A report now prints the same whichever colour scheme you view it in. Printing drops a dark
  background but keeps the text colours, so a reader in dark mode printed pale grey on white. That
  hit every cell, description and breadcrumb, and not only the footer. The print stylesheet now
  replaces the whole palette with one meant for paper.

  This also fixes the page footer. Its screen grey held a 2.2:1 contrast against white paper even
  in light mode, so the attribution and the run timestamp printed as almost nothing.
- A verdict dot now prints. A dot carries its colour as a background, and a browser drops that
  colour when printing unless you ask it to keep it. A printed index tree therefore showed no
  verdicts at all.
- Choosing dark explicitly now sets every colour. The toggle's palette left one colour out. A page
  switched to dark on a light-scheme system therefore kept the light shadow, which is what shows
  that a table can scroll sideways.

## [1.4.0] - 2026-08-20

### Added
- An HTML template of your own can now add to the built-in stylesheet, through a new
  `extra_stylesheet` block, and does not replace it. A report carries its stylesheet inside the
  file. Until now a role declared with `@ColumnRole` had nowhere to take a style from, because the
  only way to reach the CSS was to rewrite the whole sheet. `table.html.peb`, `index.html.peb` and
  `single.html.peb` all leave the block.
- `@Tree` marks a column whose cells hold a tree, written as a nested collection. The built-in HTML
  report then opens each level below its parent, and not beside it, with a guide line down the level
  and a connector on each entry. The default map rendering puts a key beside its value, which walks
  a deep tree sideways across the page. The cell value does not change, so a reader still meets the
  notation they would write.
- A cell whose set expands its row now carries the mark `value-set` in the published report. A
  published table shows no parameters, so `{a, b}` reads the same two ways. It can expand the row
  into one run per value, or it can be a `Set` the test takes whole. The reporter tells the two
  apart the way the runtime does: a set value against a parameter that is not a set expands. The
  built-in HTML stylesheet labels such a cell "any of". Markdown carries no roles, so the two stay
  alike there.
- `@Lines` marks a column whose cells hold the lines of one block of text. The parameter takes the
  lines joined by newlines, or the lines themselves for a `List` parameter. The HTML report draws
  the cell as a stacked monospace block, and not as a bulleted list, so text whose alignment is the
  point reads as you wrote it. AsciiDoc publishes the role and keeps its bulleted list. Markdown
  does not change.
- A space run at the end of a line now carries a `trailing` class beside `sp`. A stylesheet can
  then tell the one run a whitespace-preserving layout cannot show from the runs it can. The
  built-in HTML stylesheet uses it two ways. It drops the markers from alignment padding inside a
  `lines` column, but keeps a trailing run marked. It also leaves a blank line in such a column
  unmarked, because that line already shows as a line of the block. Only the class is new. The
  marked runs, and the characters in them, do not change.
- A test parameter can now declare a role for its column, and the reporter publishes that role on
  every cell of the column. Annotate an annotation of your own with `@ColumnRole` and put it on the
  parameter. The reporter publishes the annotation's simple name in kebab case, or the token
  `@ColumnRole("...")` names. A published role reaches the HTML report as a CSS class, and the
  AsciiDoc report as an element role. A stylesheet of yours can therefore style a column the
  reporter knows nothing about. The reporter still derives `scenario`, `expectation`, `passed` and `failed` itself.
  It publishes a declared role beside them, and never treats one as derived.
- A table wide enough to scroll sideways now says so. The scroll box keeps a visible slim scrollbar,
  and a shaded edge appears on whichever side holds more table. The box used to scroll silently. On
  a platform with overlay scrollbars, a reader had no way to tell that the last column on screen was
  not the last column.
- A feature in the `tabletest-reporter.yaml` `features:` tree can now carry a `description`. The
  reporter draws it under the feature's title on its own index page, the way it draws a test class's
  `@Description`. An intermediate index page could carry only a title before this, so anything true
  of a whole group of features had to be repeated on every rule below it.

### Fixed
- A rule page now shows the description of the page above it, over its own. That class or feature
  description is where you explain the notation a rule's columns use, and it reached the index page
  alone. The sidebar links to rule pages, and search returns rule pages, so a reader met the columns
  without the explanation. All three formats show it.
- A description of more than one paragraph now renders as more than one paragraph in HTML. HTML
  collapses a blank line, so every paragraph of a `@Description` ran together into one block. The
  Markdown and AsciiDoc reports were already right. The reporter still drops the line breaks inside
  a paragraph. The text therefore flows to the width of the page, and not to the width of the text
  block you wrote it in.
- Declaring a custom format with no name now fails with `Format name cannot be missing`. The message
  used to read only `name`. The blank-name and leading-dot refusals do not change.

## [1.3.0] - 2026-07-23
> [!IMPORTANT]
> **Slug generation changed, and some published page names change with it.** Three kinds of name
> now produce a different filename and URL: a test or class name holding a letter with no ASCII
> form (`ß æ ø ł þ ð œ đ`), one holding a compatibility character (`ﬁ`, fullwidth letters, `x²`,
> `Ⅻ`, `™`, `½`), and one written in a non-Latin script. Those characters used to be dropped, so
> `Grüße` published as `grue` and now publishes as `grusse`.
>
> If you already publish your documentation, the affected pages move, and existing links to them
> break. Regenerate the whole report rather than an incremental subset, and expect to update any
> links you control. Two kinds of name are unaffected: a name made only of ASCII, and a name whose
> accented letters already folded to a base letter (`ü ö ä é å ñ`). Their slugs are byte-for-byte
> what they were.

### Fixed
- A test named wholly in a non-Latin script no longer produces an empty filename. `Москва` now
  publishes as `москва`, and not as nothing at all. Greek, CJK, Devanagari and every other script
  behave the same way. Such a name keeps its own characters. Browsers percent-encode them, and
  GitHub Pages serves them as UTF-8.

  A name whose ASCII form is only a number it held (`Москва основана в 1147`) takes the same route,
  and no longer publishes as `1147`. A name with no letter and no digit anywhere gets `unnamed-`
  plus a stable hash, so two such names still get two files. A name that already produced a working
  slug does not change.

### Changed
- A compatibility character now reduces to the characters it stands for, instead of dropping out of
  the filename and the URL. `ﬁle ﬂow` becomes `file-flow`, fullwidth `Ｆｕｌｌｗｉｄｔｈ` becomes
  `fullwidth`, `x² area` becomes `x2-area`, and `Chapter Ⅻ` becomes `chapter-xii`. A precomposed
  accented ligature (`Ǽgir`) no longer loses its letter either. A name written in a script of its
  own composes the same way. The halfwidth and fullwidth katakana spellings of one name (`ﾃｽﾄ`,
  `テスト`) therefore give one slug, and not two.
- A Latin letter with no ASCII form now appears in the filename and the URL, instead of vanishing
  from it. `Grüße` becomes `grusse`, where it used to become `grue`. `ÆØÅ` becomes `aeoa`, where it
  used to become `a`. One rule decides the spelling. A ligature expands to its component letters
  (`ß`→`ss`, `æ`→`ae`, `œ`→`oe`). A stroked letter folds to its base letter (`ø`→`o`, `ł`→`l`,
  `đ`→`d`, `ð`→`d`). `þ`→`th`, because thorn has no Latin base letter.

  A letter that already folded (`ü ö ä é å ñ`) does not change. A name built only from those keeps
  the exact slug it had. A name holding one of the newly spelled-out letters gets a new one.
- The JUnit extension no longer depends on the Slugify library, and generates a filename slug
  itself. Slugify required Java 21, which forced every project documenting its tests onto a 21+
  runtime. The extension now targets Java 17, so a Java 17 project can use it on its own test
  runtime. The build still requires Java 21+.

  Slug output does not change. The same characterisation table pins the replacement, extended with
  non-ASCII cases, and it reproduces the library exactly. This also takes Slugify and its SLF4J
  transitive off the test classpath, where they could conflict with the versions a project uses
  itself.

### Added
- Several directories of TableTest output now merge into a single spec, so the modules of a
  multi-module build publish one set of documentation. Maven gains a `tabletest-reporter:aggregate`
  goal, which walks the reactor and finds each module's output by itself, and `<inputDirectories>`
  on the `report` goal for naming them explicitly. Gradle gains `inputDirs`. The CLI takes a
  repeated `-i` / `--input`.

  The report tree comes from the test class names, so modules land in one package hierarchy. Where
  two modules published the same class, the most recent output wins. The reporter skips a listed
  directory that does not exist, and warns, so a partial build still publishes what it has.
- A `publish` section in `tabletest-reporter.yaml` now decides which pages the report holds. An
  `exclude` path holds a page back, and its subtree with it. An `include` path re-admits one page
  below an excluded page, so a single rule table still publishes from an otherwise internal class.
  Paths name pages the way the report's URLs do (`converting/convert-with`), with `*` for any part
  of a page name and `**` for any number of levels.

  Selection happens when the report is generated. What publishes is therefore no longer tied to how
  you tagged or ran the suite, and re-curating a spec needs no new test run. A feature page left
  with nothing published under it drops with its pages. The reporter logs a path that matches no
  page, and skips it. Without the section every table publishes, as before.
- An optional `tabletest-reporter.yaml` in the project directory now carries spec-level metadata.
  Give the whole spec a real title and an intro paragraph on its root index, in place of the
  lowercase package segment that used to leak through ("junit", "example"). Retitle an intermediate
  index page. Set an explicit reading order for the top-level features and their children:
  declared features lead, and undeclared siblings follow alphabetically.

  The reporter reads the file at report time and applies it on top of the generated tree, so a
  project without one is unaffected. Point elsewhere with Maven `<configFile>` or
  `-Dtabletest.report.configFile`, with Gradle `configFile`, or with the CLI `--config` / `-c`.
- Every HTML page footer now states when the report was generated: "Generated by tabletest-reporter
  · 20 Jul 2026 14:32 UTC". A reader can then tell whether the published documentation still tracks
  the code it came from. The footer states the timestamp in UTC and carries a machine-readable
  `<time datetime>` attribute. Every page of one run shares the one timestamp.

## [1.2.0] - 2026-07-18

### Added
- A new built-in `html` output format writes living documentation that needs no Asciidoctor step.
  Each page is a standalone file with its CSS and JavaScript inside it, and no external reference.
  A page carries an autowidth table, with a sticky header row and first column. It draws a nested
  collection as structure, and states a pass/fail badge with status colouring. A broken row opens
  its failure details below the table. It
  also carries a row filter, a "failing only" toggle, a roles legend, a light and dark toggle, and
  a print stylesheet. Every link is relative, so the output tree is ready for GitHub Pages.
- The HTML report marks whitespace you would otherwise have to count. Four things make a value
  significant: whitespace at the start or end of any line, a tab, a run of spaces, and a pipe. Such
  a value renders in monospace, with a dot drawn per significant space and an arrow per tab. An
  indent expectation, a whitespace-only cell and a formatted row all read at a glance. A single
  space between words stays unmarked. The value text itself does not change, so it still copies and
  searches as the value the row ran with.
- An HTML index page rolls pass and fail up the tree. Each entry of the tree carries a status dot,
  and each index states its own scenario pass rate: "N of M scenarios broken", or "All passing".
- Every HTML page opens with a breadcrumb trail of the pages above it: root package, then class,
  then table. The links are relative, so the trail works from any subpath.
- Every HTML page carries a menu button that opens a navigation drawer, holding the whole-report
  tree with its status dots. The drawer marks the page you are on, and every link in it is relative
  to that page. It slides in over the content, so a table always gets the full page width.
- A search box in the navigation drawer searches the whole report: every page's title, description,
  headers and cell values. It lists the matching pages, with their status dots, to jump to. One
  shared `tabletest-search-index.js` backs it, written once to the output root and linked from each
  page by a relative prefix. Search therefore works offline, over `file://`, and from any subpath,
  and makes no external request.
- Single-file HTML mode assembles the whole report into one self-contained `.html`. Pass
  `--single-file` or `-s` on the CLI. The file inlines every table as an anchored section, and inlines the
  search index too. The sidebar and the search target those anchors in the page. The reporter writes
  no sibling asset. Reach for it to share a report as a release asset, or to attach one to an email or a
  ticket. The multi-file report stays the default.

### Fixed
- The Gradle `reportTableTests` task now tracks the TableTest YAML files as task inputs, even where
  you configure no explicit `inputDir`. It tracks all three places they come from: the default
  `build/junit-jupiter`, the JUnit output directory override, and the location
  `junit-platform.properties` names. The task could otherwise stay `UP-TO-DATE` after a new test
  run, or restore a stale report from the build cache. The task is also ordered to run after a
  `Test` task where you ask for both.
- A table test whose display-name slug matches its class slug no longer loses one of the two
  published YAML files. The same `@DisplayName` on both is what produces the clash. The table file
  now takes a numeric suffix, which keeps the class file and the table file apart.
- A row whose scenario value is a prefix of another row's no longer absorbs that other row's
  results. "Add" and "Add negative" is such a pair. Rows that share one scenario value now carry no
  pass or fail role at all, as documented, in place of the OR of every duplicate.
- Where the input directory holds YAML from several test runs, the report now reflects the files
  modified most recently. A `junit.platform.reporting.output.dir` holding `{uniqueNumber}` produces
  such a directory. The report used to take whichever run's files sorted first.
- On Windows, an index-page link and a single-file anchor used a backslash, which is the platform's
  file separator. Browsers, and Markdown and AsciiDoc renderers, all broke on them. A generated link
  now uses `/` on every platform.

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
