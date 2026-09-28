# Plan: coder/hardener agents, workflow update, skill sync

Status: proposal, 2026-09-28. Run it **before**
[`uncle-bob-tooling-port.md`](uncle-bob-tooling-port.md), whose items go
through the pipeline this plan sets up. Delete this file once its issues
exist.

## Why

Veilclj split Steps 4-7 between two pinned agents, because one agent owning
all of Steps 4-7 was overloaded:

- The **coder** (Haiku) builds the behaviour test-first and commits.
- The **hardener** (Sonnet) makes it clean without changing behaviour.

The `implement-issue` skill owns the sequence and the verification. Veil
still has one Haiku agent doing Steps 4, 5 and 7, briefed through
`.claude/subagent-delegation.md`. The same overload applies here.

## Skill diff: Veil vs Veilclj

Compared with CR and whitespace stripped (the raw diffs are mostly CRLF/LF
noise), and with `Veilclj→Veil` / `VEILCLJ→VEIL` / project `3→2` normalised:

| Skill | Real difference | Action |
|---|---|---|
| `grilling` | none (line endings only) | none |
| `uncle-bob-craft` | one line: the architecture description | none (each repo describes its own) |
| `close-milestone` | repo name only | none |
| `spec-feature` | Veilclj pins `model: opus` | **port** |
| `brainstorm-issue` | `model: opus`; board fields set through `docs/project-board.md` with a read-back check | **port** |
| `brainstorm-milestone` | same as `brainstorm-issue` | **port** |
| `audit-planning` | Priority set through `docs/project-board.md` + read-back | **port** |
| `spec-intent` | Status set through `docs/project-board.md` + read-back | **port** |
| `resume-issue` | Points to `implement-issue` / `--from-hardener`. Adds a note that Step 2 must be handed back to the user, because `spec-feature`'s `model: opus` pin only lasts one turn | **port** |
| `implement-issue` | Veilclj only | **port** (adapted) |
| `spec-intent-auto` | Veil only | **keep**, re-point at coder/hardener |

The read-back matters: `gh project item-edit` prints nothing on success and
can fail silently. Veil's skills currently report success without checking.

## Steps

One PR per step, so each diff stays reviewable. Branch prefix `feat/`, or
`docs/` for step 1.

### 1. `docs/project-board.md` + board-field skills

- Copy Veilclj's `docs/project-board.md` with project number `2` and repo
  `SwiftFaze/Veil`. Use the id-based `item-edit` form, look up field and
  option ids by name, and read the value back.
- In `brainstorm-issue`, `brainstorm-milestone`, `audit-planning` and
  `spec-intent`, replace the inline `gh project item-add/item-edit` blocks
  with a link to it. This makes each skill shorter; `audit-planning` is at
  210/215.
- Check: set a Priority on a scratch issue through the snippet and read it
  back.

### 2. Model pins

- Add `model: opus` to `spec-feature`, `brainstorm-issue` and
  `brainstorm-milestone`. These are the judgment-heavy, grilling-style skills.
- Port `resume-issue`'s "hand Step 2 back to the user" note, which explains
  why an opus pin doesn't survive a multi-turn loop.

### 3. The agents: `.claude/agents/coder.md` and `hardener.md`

Adapted from Veilclj. The ownership split is kept as-is; the Java-specific
parts move into the right role:

| Duty (currently in `workflow.md` Step 4-7) | Goes to |
|---|---|
| Test-first slices; ArchUnit module direction | coder |
| Swing visual verification (`docs/ui-verification.md`) | coder: it's behaviour and rendering, not quality |
| UI work reads `docs/ui-styling.md` itself | coder's reading scope |
| Wiring `.feature` steps; the shared-step-file rule of running `mvn clean verify` twice | coder |
| "Which screens changed" note for the playtest | coder |
| `check-clean.sh` + judgment checklist | hardener |
| PIT on changed classes; confirming `targetClasses` covers them | hardener |
| Step 7 docs decision, **including the GitHub wiki** (`docs/wiki.md`) | hardener |

- **Coder "done when":** `mvn verify` passes, not just `mvn test`, because
  PMD-minimal, ArchUnit, JaCoCo 85% and NullAway only run at `verify`. The
  work must also be committed (`feat:`/`fix:`/`test:`). The coder doesn't
  refactor for PMD-strict/CPD/PIT.
- **Hardener:** refactor only. A gate that needs a behaviour change goes back
  to the coder through the orchestrator. Commit `refactor:`/`test:`/`docs:`.
- Models: coder `haiku`, hardener `sonnet`, as in Veilclj.

### 4. `implement-issue` skill; retire `subagent-delegation.md`

- Port `.claude/skills/implement-issue/SKILL.md` with the Java commands:
  `mvn verify` for the coder check, `check-clean.sh` for the hardener check,
  and PIT scope instead of `clj-mutate` scope.
- **Ship it with the QA step (Veilclj's step 3) left out.** Veil has no QA
  runner yet, so the "no procedure and no `QA: none` line means the spec is
  incomplete" branch would bounce every feature. Tooling-port item 3 adds the
  step when it adds the runner.
- Move what's still useful from `.claude/subagent-delegation.md` into the
  skill, then delete the file:
  - the #136 story
  - the escalation ladder
  - the "green acceptance doesn't prove focus" check, pointing to
    `docs/testing-acceptance.md`
  - the PIT `targetClasses` scope warning
- Update the references to it in `resume-issue`, `spec-intent-auto`,
  `docs/clean-code-gate.md` and `docs/instruction-files.md`.
- Update `spec-intent-auto`: its Steps 4-7 dispatch `coder`, then (with no
  human playtest in between, because the playtest is its one final
  checkpoint) `hardener`, and then it verifies each the same way
  `implement-issue` does. Link the verification section rather than copying
  it.

### 5. `workflow.md` Steps 4-7

`workflow.md` is at **150/150**, so every line added needs one removed.
Veilclj's Steps 4-7 are shorter because the mechanics live in
`implement-issue`. The rewrite goes as follows:

- **Step 4** says: coder, run through `implement-issue`. It keeps test-first,
  the ArchUnit direction, the note that rendering can't be proved by tests,
  and the pointer to `ui-verification.md`. It drops the dispatch mechanics.
- **Step 5** says: coder, same agent. It keeps the shared-step-file rule.
- **Steps 6-7** say: hardener. They keep the PIT `targetClasses` scope check,
  and the wiki line under the docs decision.
- **Notes for the agent:** unchanged, except that it points at
  `implement-issue` instead of `subagent-delegation.md`.
- `CLAUDE.md` Step 4.5: the playtest now sits between coder and hardener, not
  after the whole of Steps 4-7. Update the sentence, not the budget: that file
  is at 96/100.

### 6. Budgets and the checklist line

- `check-instruction-budget.sh`: replace the
  `.claude/subagent-delegation.md:90` entry with `.claude/agents/*.md:<size of
  the larger agent file>`, and turn the `workflow.md` entry into the
  `workflow*.md` glob Veilclj uses.
- Add Veilclj's **"Single answer"** line to the judgment checklist in
  `check-clean.sh` and `docs/clean-code-gate.md`: "for each thing a changed
  `ui` class shows or decides, name the engine method that supplies it".
  `clean-code-gate.md` is at 244/250, so trim first.

## Verification

- `bash .claude/tools/check-instruction-budget.sh` passes.
- `grep -r subagent-delegation .claude docs CLAUDE.md` returns nothing.
- Dry run: take a small open issue through `/spec-intent`, `/spec-feature` and
  `/implement-issue`. Confirm the coder commits, the pipeline stops for the
  playtest, the hardener's diff changes no `.feature` scenario or test
  expectation, and the PR body has `Closes #N`.

## Out of scope

Keeping the two repos' skills in sync with a script. The remaining differences
are legitimate: repo names, project numbers, and each repo's own
architecture. A cross-repo check would mostly flag those, which is the "low
value, not a need for stronger enforcement" case. Re-diff by hand when either
repo's skills change significantly.
