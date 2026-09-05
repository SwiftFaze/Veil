# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

@.claude/workflow.md

## Project

Veil is a 2D ASCII-tile desktop RPG built with Java 17 Swing (no game engine). Rendering draws Unicode/ASCII glyphs with `Graphics2D.drawString` onto a `JPanel`; there is no sprite/texture pipeline for tiles.

## Build & run

- Build: `mvn compile`
- Unit tests + Cucumber acceptance tests: `mvn test`
- Everything, including integration tests: `mvn verify`
- Run a single unit test: `mvn test -Dtest=PlayerTest#movingRightIncreasesX`
- Run a single Cucumber scenario: `mvn test -Dcucumber.filter.name="A newly created player starts as a Warrior"`
- Run a single integration test: `mvn verify -Dit.test=ModLoaderIT`
- See `docs/testing.md` for the full breakdown of the three test layers (unit / acceptance / integration) and why they're separated.
- Run the game from source: `mvn compile exec:java` (no packaging or version number needed).
- Run the dev-only class/stats sandbox instead of the game: `mvn compile exec:java -Dexec.mainClass=com.swiftfaze.veil.sandbox.ClassSandbox` (not part of the packaged build — see `docs/architecture.md`).
- `mvn package` also produces `target/Veil-<version>-app.jar`, a runnable fat jar (`java -jar` it directly) — see `docs/release.md` for how CI turns that into Windows/Linux/macOS installers on release.
- Mutation testing (workflow Step 6): `mvn org.pitest:pitest-maven:mutationCoverage` — report lands in `target/pit-reports/`. See `docs/testing.md`.

## CI / releases

- `.github/workflows/ci.yml` runs `mvn verify` on every PR/push to `master`/`develop`; it's a required status check on both (branch protection).
- Versioning and changelog generation are fully automatic via Release Please — see `docs/release.md`. Don't hand-edit `pom.xml`'s `<version>` or write `CHANGELOG.md` entries by hand; they're derived from Conventional Commits.
- Two release channels: `master` → stable (`vX.Y.Z`), `develop` → beta prereleases (`vX.Y.Z-beta.N`, marked "Pre-release" on GitHub). Merging `develop` into `master` is what promotes accumulated beta work into the next stable release.
- **No direct commits to `master`, ever — including for admins.** Branch protection has `enforce_admins` on, so this is enforced, not just a convention. The only two ways changes reach `master`:
  - A PR from `develop` (a release promotion), or
  - A PR from a `hotfix/*` branch (an urgent fix that can't wait for the next promotion — branch off `master`, fix, PR back to `master`, then bring the same fix into `develop` too so it isn't lost on the next promotion).
  - The `master-source-check` CI job (in `.github/workflows/ci.yml`) enforces this mechanically: it fails any PR into `master` whose head branch isn't `develop`, `hotfix/*`, or Release Please's own `release-please--branches--master`.
- **Feature/fix/docs PRs into `develop` must be squash-merged** (`gh pr merge <n> --squash --delete-branch`, or GitHub UI's "Squash and merge") — never "Create a merge commit". A regular merge commit duplicates every changelog entry: GitHub's merge-commit body repeats the original commit's conventional-commit-formatted subject line as its own second line, and Release Please's git-log walk counts both the original commit and the merge commit as separate qualifying commits (e.g. PRs #149, #157, #160 each produced two near-identical lines in the same `CHANGELOG.md` release section — that's how this was caught). The one exception is the `develop` → `master` promotion PR (see the `close-milestone` skill), which must stay a true merge (`gh pr merge --merge`) — squashing it would collapse every accumulated beta commit into one, losing the granular history Release Please needs to generate `master`'s changelog.

## Architecture

See `docs/README.md` for the full docs index. In short: `docs/architecture.md` covers the game engine and data model (entry point/window assembly, the `GamePanel` render loop, the flat single-layer `WorldScene`/`Tile` world model, JSON building blueprints, player movement, Key Bindings-based input, JSON-driven player classes/stats, the class/stats sandbox, and the `DrawableAsciiEntity` rendering contracts); `docs/ui-widgets.md` covers the reusable Swing widget framework and theming; `docs/ui-styling.md` covers the concrete layout/spacing, typography, and color rules any panel or widget should follow; `docs/screens.md` covers how those widgets compose into the game's actual screens (title, settings, keybinds, inventory, Codex); and `docs/components.md` covers the self-describing list/table/detail UI contract (`Identifiable`/`DetailDescribable`/`Inspectable`) that any new Swing panel or mod-loaded content type should follow.

## Spec-first workflow layout

This repo follows a risk-gated agentic pipeline (see `.claude/workflow.md` for the step-by-step mechanics, including the high-risk vs. standard path split — this section only covers repo-specific file locations and extensions):

- `/specs/intent/<feature-slug>.md` — copy `specs/intent/TEMPLATE.md` to start one by hand, use the `spec-intent` skill to derive one from an existing GitHub issue, or `brainstorm-issue` for an idea that isn't ready to be an intent doc yet. Local scratch only: everything under `specs/intent/` except `README.md`/`TEMPLATE.md` is gitignored, so these never get committed — see `specs/intent/README.md`.
- `/specs/features/<feature-slug>.feature` — wired to Cucumber via `mvn test` (see `RunCucumberTest`). **One `.feature` file per distinct concept** — an intent covering multiple unrelated things (a new class *and* a new biome) produces multiple `.feature` files (`class-warrior.feature`, `biome-jungle.feature`), never one bundled file. See `specs/features/README.md`.
- `docs/` — narrative/reference documentation (architecture, testing) kept up to date as part of each feature's definition of done, not left to be reverse-engineered from diffs.

**Step 7 (Documentation) for this repo also covers the player-facing [GitHub wiki](https://github.com/SwiftFaze/Veil/wiki)**, not just `docs/`: any change to a class's base stats, a new class/attribute, a changed combat formula, or other player-visible game data must update the matching wiki page in the same change. See `docs/wiki.md` for what's covered and how to edit it (it's a separate git repo, no PR needed).

### Repo-specific Step 4 addendum — Visual verification (Agent)

Whenever Step 4 (Implementation) touches Swing rendering, layout, sizing,
or text content, the implementing agent must render the actual component
and look at it before considering the step done — passing `mvn test`
only proves the code runs, since none of this project's tests assert on
pixel layout or rendered text. See `docs/ui-verification.md` for the
concrete technique (render the real component to an image via a
throwaway diagnostic class in the scratchpad, inspect it with the `Read`
tool). This is the agent's own check, done during/after implementation;
it does not replace the human playtest below.

### Repo-specific Step 4.5 — Manual playtest (Human)

Inserted between the global pipeline's Step 4 (Implementation) and Step 5
(Acceptance tests): after implementation lands, the human runs the game
(`mvn compile exec:java`) and actually plays through the changed behavior
before acceptance tests get wired up.

- **Why this exists:** `mvn verify` and Cucumber can confirm the code does
  what the spec says, but not whether movement, menu navigation, or
  rendering actually *feel* right — that's a judgment call only a human
  playtesting the running game can make.
- **Model:** no model — a human decision gate, like Step 3.
- **For a multi-area change** (e.g. a restructure spanning several
  `.feature` files), playtest each area right after it's implemented, not
  only once at the end — this mirrors the per-area `mvn verify`
  checkpoints already used in intent docs' Verification sections.
- Note what was tested and any issues found (PR description or a status
  note) so Step 7 documentation and PR review can see it.
- No feature is "done" without this playtest, same as Steps 5-7 aren't
  optional per the global workflow's notes for the agent.

### Repo-specific Step 7.5 — Close the linked issue

Inserted after the global pipeline's Step 7 (Documentation), once the
feature's PR has merged: close the GitHub issue that started this piece
of work.

- **Why this exists:** feature PRs merge into `develop`, not this repo's
  default branch (`master`) — GitHub's "Closes #N"/"Fixes #N" auto-close
  only fires on a merge into the *default* branch, so linked issues here
  do **not** close themselves on merge. This is a manual step every time,
  not a formality GitHub already handles.
- `gh issue close <number> --repo SwiftFaze/Veil --reason completed`
- Also applies to issues with no separate spec pipeline (filed via
  `brainstorm-issue`/`brainstorm-milestone`) once the work they describe
  is actually done, not just merged as a partial slice.
- The VEIL project board's `Status` field normally follows a closed issue
  to Done on its own (project workflow); if it doesn't, set it explicitly
  rather than leaving a closed issue showing as still in progress on the
  board.
