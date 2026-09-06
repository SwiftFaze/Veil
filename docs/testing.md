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

## Code quality gates (PMD and JaCoCo)

- Both gates are bound to `mvn verify` and fail the build if violated.
- **PMD (maven-pmd-plugin)** enforces design rules via `.pmd-minimal.xml`
  (complexity, method length, parameter count — see `.claude/workflow.md`'s
  "Constraints" section for the exact current numbers, which is the single
  source of truth for these; don't restate them here, they're a deliberate,
  adjustable dial per that doc) plus CPD (Copy-Paste Detector) flagging
  duplicate code blocks at 100+ tokens.
  Pure Swing layout/wiring classes with no branching logic are excluded from these checks (see `pom.xml`'s PMD excludes list).
- **JaCoCo (jacoco-maven-plugin)** enforces a minimum of 85% line coverage across all non-excluded classes (repo-wide, not per-changed-file). The same exclusion list as PMD applies.
- See `pom.xml`'s PMD plugin configuration for the full exclusion list and rationale (classes confirmed to be pure construction/layout/wiring with no real logic to unit-test).
- The one narrow, already-practiced suppression exception (`.claude/workflow.md`'s Constraints section states the general rule): a method overriding a JDK/library interface whose signature mandates more than the enforced parameter ceiling — e.g. `Border.paintBorder(Component, Graphics, int, int, int, int)` (6 parameters), see `RadioGroupWidget.RadioOptionBorder` and `TableWidget.AccentableCellBorder` — may suppress PMD's `ExcessiveParameterList` rule with `@SuppressWarnings("PMD.ExcessiveParameterList")` plus a comment naming the interface. This is specific to parameter count on an unavoidable interface override; it does not extend to complexity, length, coverage, or the module dependency rule below, none of which have an equivalent "the interface forced it" excuse — a violation there must be fixed by decomposing the code, not suppressing the check.

## Module dependency gate (ArchUnit)

- `ModuleDependencyTest` (`src/test/java/com/swiftfaze/veil/ModuleDependencyTest.java`) is a plain JUnit 5 test using ArchUnit (`archunit-junit5`), run by Surefire via `mvn test`/`mvn verify` like any other unit test — no separate command needed.
- Rules enforced:
  - "Engine" code — everything outside `com.swiftfaze.veil.ui`, excluding the `Main` composition root and the `sandbox` package — must not depend on `com.swiftfaze.veil.ui` at all.
  - Classes in `com.swiftfaze.veil.ui.widget` must not depend on screen classes that sit directly in `com.swiftfaze.veil.ui` (screens may depend on widgets, not the reverse).
- `Main` is excluded because it's the composition root that assembles the `JFrame` from UI panels — that wiring role requires depending on `ui` by definition. `sandbox` is excluded because `ClassSandboxPanel` and `DevConsolePanel` are themselves UI panels (plain JPanels that reuse `ui/widget/ListWidget`) that just happen to live outside the `ui` package as dev-only tools — they are UI code, not engine code that should be isolated from UI.
- This mirrors the engine/widgets/screens layering `docs/architecture.md`, `docs/ui-widgets.md`, and `docs/screens.md` already describe conceptually — it's the first mechanical check of that layering at the package level (previously only enforced by the function-level SLAP guidance in `.claude/workflow.md`).
- A violation fails the build with ArchUnit's own violation report naming the offending class and rule, the same way a PMD violation does.
