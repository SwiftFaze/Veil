# Testing

Three test layers, run at different points, kept in separate file-naming
conventions so Maven can tell them apart automatically:

## Unit tests

- Location: `src/test/java/**/*Test.java`
- Runner: Surefire, bound to `mvn test`
- Fast, no real I/O — the norm for new production code.
- Run a single test: `mvn test -Dtest=PlayerTest#movingRightIncreasesX`

## Acceptance tests (Cucumber)

- `.feature` files (Gherkin) live in `/specs/features/`, one per feature,
  each generated from the matching `/specs/intent/<slug>.md`. They are
  copied onto the test classpath at build time (`testResources` config in
  `pom.xml`) so Cucumber can discover them via `@SelectPackages("features")`.
- Step definitions live in `src/test/java/com/swiftfaze/veil/steps/`.
- `RunCucumberTest` (`src/test/java/com/swiftfaze/veil/RunCucumberTest.java`)
  is a JUnit 5 `@Suite` that includes the Cucumber engine — because its name
  matches Surefire's `*Test.java` pattern, the whole Cucumber suite runs
  under plain `mvn test` alongside the unit tests.
- Run a single scenario: `mvn test -Dcucumber.filter.name="A newly created player starts as a Warrior"`
- `specs/features/default-player-class.feature` +
  `steps/DefaultPlayerClassSteps.java` are a worked example proving this
  wiring end-to-end — copy their shape for the next real feature.
- A `.feature` file generated ahead of its implementation (e.g. an intent
  covering several areas, spec-drafted all at once but implemented one
  area at a time) should be tagged `@pending` at the top, and excluded via
  `RunCucumberTest`'s tag filter — otherwise `mvn test` fails on undefined
  steps for scenarios that don't have an implementation yet. Remove the
  tag from a file only once its step definitions exist.
- A `.feature` file with no Java code path to exercise at all — a
  build-pipeline or OS-installer concern rather than application behavior
  (e.g. `installer-mods-bundling.feature`) — is tagged `@manual-verification`
  instead, and excluded by the same tag filter. Unlike `@pending`, this
  exclusion is permanent: the file is the human-reviewed spec of record
  (per root `CLAUDE.md`'s review table), but it's never meant to gain step
  definitions, so the tag is never removed. Verification happens by
  actually exercising the described behavior manually (e.g. building and
  running an installer), not via `mvn test`/`mvn verify`.
- A step definition that drives a Swing component's keyboard interaction
  by calling `component.getActionMap().get(actionName).actionPerformed(...)`
  directly is faster to write but bypasses Swing's real focus-routing
  chain (`WHEN_FOCUSED` vs. `WHEN_ANCESTOR_OF_FOCUSED_COMPONENT`, and
  which component the OS/window manager actually has focused) — a panel
  can pass every such scenario while a real key press does nothing, or
  gets swallowed by the wrong ancestor. For any feature where keyboard
  focus crosses a window boundary or a component hierarchy the step
  definitions don't already exercise via real input, add at least one
  scenario driven through genuine key events (a `java.awt.Robot`-based
  diagnostic, or `component.dispatchEvent(new KeyEvent(...))` against the
  actual focus owner) rather than relying only on the `ActionMap`
  shortcut for every scenario.
  - **This only works with a real, OS-focused window.** Confirmed
    empirically (issue #175): a bare `dispatchEvent` against a component
    with no real, visible, focused window does nothing —
    `WHEN_IN_FOCUSED_WINDOW`/`WHEN_FOCUSED` bindings never fire. Getting
    real focus requires `new JFrame()` + `setVisible(true)` on an actual
    display, and `new JFrame()` itself throws `HeadlessException` when
    `GraphicsEnvironment.isHeadless()` — which is unconditionally true on
    this repo's actual CI (`ci.yml` runs `ubuntu-latest` with no `DISPLAY`
    and no Xvfb step). So a scenario built this way must guard itself with
    `Assumptions.assumeFalse(GraphicsEnvironment.isHeadless())` — skipped
    (not failed) in CI, but real coverage on any machine with a display
    (local dev, a future CI with Xvfb added). Worked example:
    `GamePanelRealKeyEventTest`. Adding Xvfb to CI so this runs
    unconditionally is a deliberately separate decision (cost/flakiness
    tradeoffs), not bundled into whatever change adds the guarded test.

### Troubleshooting: cascading/flaky Cucumber failures

If `UndefinedStepException` starts appearing across scenarios in
feature files unrelated to whatever you just changed — not real
assertion failures, but steps Cucumber claims don't exist — **check for
duplicate or ambiguous step definitions first**, before environment
theories (JDK/tooling version mismatches, JaCoCo instrumentation,
parallel execution). Cucumber matches step text regardless of the
Given/When/Then keyword, so two methods annotated with the same literal
step text under different keywords is always a duplicate. A duplicate
poisons the *entire* glue registry for that test run, not just the two
colliding methods — which is exactly what produces this
misleading-looks-unrelated, run-to-run-varying failure pattern (which
scenario happens to trigger the collision first, and how far the
resulting registry corruption cascades, depends on execution order).
Find it with:
```
grep -ohE '@(Given|When|Then)\("[^"]*"\)' src/test/java/com/swiftfaze/veil/steps/*.java | sed -E 's/@(Given|When|Then)\("(.*)"\)/\2/' | sort | uniq -d
```
See `.claude/workflow.md`'s Step 5 guidance for the full rule (this check
is mandatory before reporting Step 5 done, not just a debugging tip).
If the same literal step text genuinely needs different behavior
depending on whether it's a setup precondition or a later assertion,
that's a sign the text needs rewording into two distinct steps, not that
duplicate annotations on the same text are safe — see
`UiComponentFrameworkSteps.theConfirmationPopupIsShown()` for the
correct single-method pattern (guard with `if (x == null) { build it }
else { just assert }`) when the same text is genuinely reused as both a
fresh-build precondition and a later assertion.

## Approval tests

Approval tests compare rendered output (the ASCII glyph grid) against committed
baseline fixtures. They catch rendering regressions — camera viewport edges,
building footprints, entity layering, glyph grid content — that humans tend to
miss during playtesting but that can be checked automatically.

### How approval tests work

- **Fixture location:** `src/test/resources/approved/<scenario-name>.approved.txt`
- **Mismatch file:** `src/test/resources/approved/<scenario-name>.received.txt`
  (created only if the actual output differs from the approved fixture)
- **Behavior:** `ApprovalCheck.verify()` renders a scene to a `char[][]` grid,
  converts it to text (rows joined by `\n`), and compares against the committed
  fixture. On match, any stale `.received.txt` is cleaned up and the test passes.
  On mismatch, `.received.txt` is written, an assertion failure shows both grids
  for side-by-side inspection, and the test fails.
- **Re-approval:** After validating that a `.received.txt` is correct (e.g.
  because a fixture was deliberately updated), promote it to `.approved.txt`:
  ```
  mvn compile exec:java -Dexec.mainClass=com.swiftfaze.veil.testing.approval.ApprovalReapprove
  ```
  This command scans for all `.received.txt` files in the approved fixtures
  directory, replaces their `.approved.txt` siblings with the received content,
  and deletes the `.received.txt` files. It is never run automatically — re-approval
  is always explicit. After running it, `mvn test` will pass if the new fixtures
  are correct.
- **Line endings:** Approved fixtures must use LF (not CRLF) for byte-exact
  comparison across platforms. This is enforced in `.gitattributes`:
  ```
  src/test/resources/approved/*.txt text eol=lf
  ```
  When you commit an approved fixture, Git normalizes its line endings to LF
  regardless of the OS's default.

### Writing approval test scenarios

Use the patterns from `specs/features/approval-tests-glyph-grid.feature`:
- Build a small `WorldScene` with hand-built test-double tiles (not real mod
  content — see `WorldSceneTest` for the pattern).
- Create a `Camera` with the viewport size you want to test.
- Call `scene.renderToGrid(new Viewport(camera.getX(), camera.getY(),
  camera.getViewportWidth(), camera.getViewportHeight()), entities)` to produce
  the glyph grid. `renderToGrid` takes a `Viewport` (a small record in
  `com.swiftfaze.veil.world`) and a `List<? extends PositionedGlyph>` rather
  than `Camera`/`DrawableAsciiEntity` directly, so the seam has no dependency
  back on the root `com.swiftfaze.veil` package — see the Module dependency
  gate section below.
- Convert the `char[][]` to a text fixture: join each row into a line, rows
  into a single string with `\n` between them.
- Call `ApprovalCheck.verify(scenarioName, gridAsText)` to compare and
  record the result.

## Integration tests

- Location: `src/test/java/**/*IT.java`
- Runner: Failsafe, bound to `integration-test`/`verify` — **not** run by
  plain `mvn test**. Run them with `mvn verify`.
- Reserved for tests that need real I/O or cross-class wiring that unit
  tests shouldn't pay for on every run (e.g. `ModLoaderIT`, which loads
  actual mod content off disk instead of mocking the file read).
- Run a single integration test: `mvn verify -Dit.test=ModLoaderIT`

## Everything together

`mvn verify` runs all three layers: unit tests and the Cucumber suite via
Surefire, then integration tests via Failsafe.

## Mutation testing (workflow Step 6)

- Runner: PIT (`org.pitest:pitest-maven`, with `pitest-junit5-plugin` so it
  runs through the JUnit Platform and picks up both plain unit tests and
  Cucumber scenarios).
- Run it: `mvn org.pitest:pitest-maven:mutationCoverage` — HTML report at
  `target/pit-reports/index.html` (per-package/per-class breakdowns,
  including a *line* coverage figure distinct from mutation score).
- `<targetClasses>`/`<targetTests>` in `pom.xml` scope this to classes with
  real unit tests — pure Swing view/wiring classes with no meaningful unit
  coverage (`Main`, layout-only panels, `ClassSandbox`'s UI entry point)
  are excluded rather than left to report a wall of untested mutants.
- This is the check on the unit tests themselves (CLAUDE.md's changed-file
  coverage constraint is easy to satisfy with weak assertions; mutation
  score catches that) — not a substitute for acceptance tests or the
  Step 4.5 manual playtest.
- `<mutationThreshold>` in `pom.xml`: 51% (measured baseline 1088/2115
  mutations killed). Ratcheted by the same `check-quality-gates.sh` as the
  JaCoCo floors below. Stays a manual command, not bound to `mvn verify`.

## Code quality gates (PMD and JaCoCo)

- Both gates are bound to `mvn verify` and fail the build if violated.
- **PMD (maven-pmd-plugin)** enforces design rules via `.pmd-minimal.xml`
  (complexity, method length, parameter count — see `.claude/workflow.md`'s
  "Constraints" section for the exact current numbers, the single source of
  truth for these) plus CPD flagging duplicate code at 100+ tokens. Pure
  Swing layout/wiring classes with no branching logic are excluded (`pom.xml`'s
  PMD excludes list has the full list and rationale).
- The one narrow, already-practiced suppression exception (general rule in
  `.claude/workflow.md`'s Constraints section): a method overriding a JDK/
  library interface whose signature mandates more parameters than the
  enforced ceiling — e.g. `Border.paintBorder(...)` (6 params, see
  `RadioGroupWidget.RadioOptionBorder`/`TableWidget.AccentableCellBorder`) or
  `DocumentFilter.replace(...)` (5 params, see
  `PatternFieldWidget.AllowedCharacterFilter`) — may carry
  `@SuppressWarnings("PMD.ExcessiveParameterList")` plus a comment naming the
  interface. Parameter count only; never complexity, length, or coverage.
- **JaCoCo (jacoco-maven-plugin)** enforces coverage floors at two scopes, both
  measured from a real baseline and ratcheting up only — enforcement is
  `.claude/tools/check-quality-gates.sh`, run as `repo-hygiene.yml`'s
  `quality-gate-ratchet` job on every PR; this list is not the source of
  truth, that check is:

  | Scope | Counter | Floor | Measured baseline |
  |---|---|---|---|
  | `BUNDLE` (repo-wide) | `LINE` | 85% | 90.34% |
  | `BUNDLE` (repo-wide) | `BRANCH` | 78% | 78.98% |
  | `SOURCEFILE` (per file) | `LINE` | 35% | 35.7% (`FillLayout.java`) |
  | `SOURCEFILE` (per file) | `BRANCH` | 50% | 50.0% (`FillLayout.java`) |

  The per-file scope is `SOURCEFILE`, not `CLASS`: JaCoCo's `CLASS` element
  counts every compiled `.class` file, so a nested/anonymous inner class is
  judged as its own unit — measured on `develop`, the worst such rows are
  incidental helpers of already-tested widgets, which forces any green
  `CLASS` floor to ~0% (see `specs/intent/coverage-gate-floors-class-scope.md`
  for the full derivation). `SOURCEFILE` judges the `.java` file someone
  actually adds, needing no per-inner-class exclusion list.
  Excludes are shared with PMD's list above, plus a `$*` sibling glob per
  entry (a bare glob only matches the outer class, not its nested/anonymous
  ones) — see `pom.xml`'s `jacoco-check` execution for the full list.
- **Mutation testing** (below) also carries a ratcheted `mutationThreshold`,
  enforced by the same `check-quality-gates.sh`.

## Compile-time gates (Error Prone + NullAway)

- Both are wired into `maven-compiler-plugin` (`pom.xml`) and bound directly
  to `mvn compile` - not `verify`, and not a profile like `clean-code`. This
  is deliberate: Error Prone is a `javac` compiler plugin, so the entire
  point is that a violation fails the compile an agent already has to run,
  the same feedback loop as a syntax error. Diff-scoping it through
  `check-clean.sh` (the PMD/SpotBugs pattern) would let it be skipped;
  binding it to `compile` means it can't be.
- **JDK 17 compiler-plugin flags**: `-XDcompilePolicy=simple`,
  `--should-stop=ifError=FLOW`, plus a set of `-J--add-exports`/
  `-J--add-opens` flags reaching into `jdk.compiler`'s internal packages
  (stable since JDK 9, so unaffected by this machine running Maven on a
  newer JDK than the project's 17 compile target - see `pom.xml`'s plugin
  comment). These require `<fork>true</fork>`, and on Windows, forking
  generates a batch file that silently rewrites backslash line-continuations
  inside a multi-line `<arg>` into forward slashes, corrupting the flag list
  (confirmed upstream bug: `google/error-prone#4256`). The fix is structural,
  not optional: every Error Prone/NullAway flag lives in **one single-line**
  `<arg>`, never split across XML lines with backslash continuations.
- **Error Prone runs repo-wide** at the default check set (it is not
  package-scoped) - only NullAway is limited to specific packages. Four
  checks are demoted from the default set, each with a one-line reason next
  to the `<arg>` in `pom.xml`:

  | Check | Demoted to | Why |
  |---|---|---|
  | `EnumOrdinal` | WARN | fixed tab indices in this codebase intentionally rely on ordinal position |
  | `StringCaseLocaleUsage` | WARN | the flagged strings are ASCII-invariant identifiers (content types), not locale-sensitive text |
  | `MissingSummary` | WARN | Javadoc-summary style, already covered by IDE formatting conventions |
  | `ImmutableEnumChecker` | WARN | flags record fields that hold function references, which this check can't distinguish from genuine mutable state |

  Any further check that proves noisy gets the same treatment: demote it in
  the same `<arg>`, with its own one-line reason next to it. Never demote
  silently.
- **NullAway's annotated-packages list** starts at `com.swiftfaze.veil.world`
  and `com.swiftfaze.veil.entities` (which covers `entities.player`,
  `entities.items`, `entities.buildings`, and `entities.quests` too, since
  NullAway matches by package prefix) - chosen for value, not ease: a null
  tile or a null player/entity field is where a null reference actually
  crashes the game. **This list only grows, never shrinks** - the same
  ratchet posture the ArchUnit frozen violation store below already uses.
  To add the next package:
  1. Add it to the comma-separated `AnnotatedPackages` value inside the
     single-line `-Xplugin:ErrorProne ...` `<arg>` in `pom.xml`.
  2. Run `mvn compile` and fix every finding it reports in that package -
     annotate a genuinely-optional reference `@Nullable`
     (`com.swiftfaze.veil.annotations.Nullable`, see below), or fix the real
     gap NullAway found (an uninitialized field, a missing null-check).
     Don't paper over a finding with a blanket
     `@SuppressWarnings("NullAway")` - that turns the checker off for the
     whole method instead of making the one nullable reference explicit,
     which defeats the point of adding the package at all.
- **`@Nullable` annotation**: `com.swiftfaze.veil.annotations.Nullable`, a
  hand-rolled marker (`@Target` on method/field/parameter/local variable,
  `CLASS` retention) rather than a dependency on JSR-305/checker-framework -
  NullAway matches `@Nullable` by simple class name regardless of package, so
  no dependency is needed for this half of the feature. Use this one
  annotation consistently for every `@Nullable` in `world`/`entities` and
  any future annotated package, rather than each file picking its own.
- **Measured cost**: a clean `mvn compile` (`mvn clean compile`, cache warm)
  took ~11.7s before this change and ~24.2s after, on the machine this was
  measured on - roughly +12.5s. This is the agent's inner-loop cost on every
  `mvn compile`/`mvn verify`/`mvn test` run, not just CI; if it grows
  further as more packages join the NullAway list, re-measure and reconsider
  whether Error Prone's default-check pass in particular needs to move
  behind a profile the way `clean-code`'s SpotBugs pass already does.

## Module dependency gate (ArchUnit)

- `ModuleDependencyTest` (`src/test/java/com/swiftfaze/veil/ModuleDependencyTest.java`) is a plain JUnit 5 test using ArchUnit (`archunit-junit5`), run by Surefire via `mvn test`/`mvn verify` like any other unit test — no separate command needed.
- **Direction rules** (hand-picked, not frozen):
  - "Engine" code — everything outside `com.swiftfaze.veil.ui`, excluding the `Main` composition root and the `sandbox` package — must not depend on `com.swiftfaze.veil.ui` at all.
  - Classes in `com.swiftfaze.veil.ui.widget` must not depend on screen classes that sit directly in `com.swiftfaze.veil.ui` (screens may depend on widgets, not the reverse).
  - `Main` is excluded because it's the composition root that assembles the `JFrame` from UI panels — that wiring role requires depending on `ui` by definition. `sandbox` is excluded because `ClassSandboxPanel` and `DevConsolePanel` are themselves UI panels (plain JPanels that reuse `ui/widget/ListWidget`) that just happen to live outside the `ui` package as dev-only tools — they are UI code, not engine code that should be isolated from UI.
- **Structural rules** (frozen at today's baseline in a shared violation store):
  - **Package cycle freedom**: the dependency graph between packages must be acyclic. The rule uses full-depth slicing via the pattern `com.swiftfaze.(**)` so every package at every level is its own slice — this catches cycles involving the root package and intra-package cycles that a top-level-only pattern would miss. At this granularity the codebase's actual cycle count is **104** elementary cycles (not the 6 bidirectional package *pairs* the originating issue counted by hand — full-depth slicing surfaces every distinct cycle through those pairs, including ones through the root package and within `entities`), all frozen as today's baseline.
    - **`cycles.maxNumberToDetect` must be raised above ArchUnit's default of 100** (set to `10000` in `archunit.properties`). ArchUnit's cycle detector stops early once it hits this cap and reports only "cycles found so far" — with 104 real cycles and the default 100-cycle cap, which ~100 got reported varied non-deterministically between JVM runs with zero code changes, making the frozen store spuriously mismatch and fail on a clean re-run. Confirmed by running `mvn test` five times in a row with no changes: unstable at the default cap, stable at `10000`. If this rule ever starts flaking again, suspect this cap before anything else.
  - **No public mutable static fields**: all `public static` fields that are not `final` are forbidden in the `com.swiftfaze.veil..` package tree. `public static final` constants are fine; `WidgetTheme`'s 13 existing `public static` non-final color fields (repopulated by `applyTheme()` from mod-loaded themes) are frozen as accepted debt rather than refactored now.
  - **Instantiation confined to composition roots**: classes in `com.swiftfaze.veil.game..`, `com.swiftfaze.veil.world..`, `com.swiftfaze.veil.mods..`, and `com.swiftfaze.veil.entities..` may only be *constructed* by `Main`, the single composition root — the rule matches constructor calls specifically (ArchUnit's `callConstructorWhere`), not general method/field access, so passing an already-built engine object around isn't a violation, only `new`-ing one from outside the composition root is. A class residing *inside* one of those four packages is naturally exempt from its own rule (engine subsystems building each other isn't a composition-root violation, only external code reaching in is) — this is why `GamePanel` (itself in `game`) constructing `Player`/`TileTestScene2`/`Camera` isn't flagged at all, rather than being frozen debt. The two violations actually frozen today are `sandbox.ClassSandboxModel` and `sandbox.PlayerFieldMutator` each constructing `entities.player.Stats` directly. `com.swiftfaze.veil.ui..` is explicitly excluded from the target set so the rule stays about engine construction, not a general no-`new` rule firing on every `new JPanel()`.
- **Freeze mechanism**: all three structural rules share one violation store at `src/test/resources/archunit_store/`, configured via `src/test/resources/archunit.properties`. The store is committed to the repo; on first run with an empty store, ArchUnit records all violations (the "freezing" step). On subsequent runs, the same violations are accepted as baseline, and NEW violations fail the build (the "ratchet" step). Violations that no longer reproduce drop out of the store automatically on the next run — no hand-editing needed to fix a cycle, for example. To update the store after fixing a genuine violation, just re-run `mvn test` or `mvn verify` and commit the smaller store diff. **Do not re-freeze violations by hand** — that mechanism is exactly what could turn the ratchet into a rubber stamp if abused; changes to the store must come from changes to the code.
- This mirrors the engine/widgets/screens layering `docs/architecture.md`, `docs/ui-widgets.md`, and `docs/screens.md` already describe conceptually — it's the first mechanical check of that layering at the package level (previously only enforced by the function-level SLAP guidance in `.claude/workflow.md`).
- A violation fails the build with ArchUnit's own violation report naming the offending class and rule, the same way a PMD violation does.
