# Subagent delegation rules

Rules for handing implementation work (Steps 4/5/7 of `.claude/workflow.md`)
off to a subagent, and for verifying what comes back. Read this before
dispatching or resuming a Step 4/5/7 agent, or a fork. `workflow.md` covers
the pipeline shape (what step does what); this file covers the mechanics
and failure modes of the handoff itself.

## Choosing agent type and model

- **Default to a fresh agent pinned to Haiku 4.5, not `/fork`.** Haiku's
  per-token cost plus a tight, self-contained prompt beats a fork's "free"
  inherited context running on the pricier parent model. The orchestrator
  must do the exploration once and compress it directly into the handoff
  prompt: every file path needed (with line numbers), the actual code
  being referenced (not just its name), and the reasoning/decisions
  already made — everything the agent would otherwise rediscover by
  reading files.
- **Tell the agent explicitly not to scan or explore the codebase beyond
  the files listed.** If something it needs is missing, wrong, or
  insufficient, it should stop and report exactly what's missing rather
  than grepping/globbing around for it — the orchestrator supplies the
  missing piece and resumes it. This constraint is what makes the cheaper
  model actually cheaper; without it, the agent falls back to exploring
  the codebase itself.
- **Fall back to `/fork` only when the context genuinely can't be
  compressed into a prompt economically** — the relevant material is too
  sprawling, exploratory, or spread across too many files/decisions to
  excerpt without the prompt-writing itself costing nearly as much as
  just forking. Exception, not default.
- **A fresh Haiku agent is also right whenever true isolation is
  needed** — e.g. multiple tickets in parallel across separate
  worktrees, where a fork's shared-context model isn't appropriate
  anyway. Same excerpt-pasting rule applies.
- **Run independent Step 4 implementations in parallel as a normal
  option.** When more than one ticket is ready, launch a separate fresh
  Haiku agent per ticket, each in its own git worktree — the default way
  through multiple ready tickets, not an edge-case fallback.

## Staying in one agent across Steps 4→5→7

Steps 4, 5, and 7 are a single continuous handoff, not three separate
delegations: stay in the same fresh Haiku agent across 4→5→7 for one
ticket rather than re-briefing a new one at each step. The one question
worth asking before any of the three is whether this specific piece of
work needs true isolation (parallel work in a separate worktree) — that's
the one case where a fresh, separately-briefed agent (or `/fork`) is
actually the right call instead of continuing the existing one.

## Verifying what comes back

Never relay a subagent's "done" report as fact without checking it
yourself first — applies to every completion, not only visual-verification
claims (`docs/ui-verification.md` states this for that one case; it
generalizes). A subagent's summary describes what it intended to do, not
necessarily what it did, no matter how confident the report reads. Before
treating any step as finished: open the file it claims to have produced,
run the command it claims passed (`mvn verify`, the specific test, the
specific grep), read the diff of what actually changed. Confidence and
detail in a report are not evidence.

- **A passing metric is not evidence unless its scope is also confirmed.**
  A metric can be genuinely passing while measuring the wrong code — e.g.
  a mutation-testing score computed against `pom.xml`'s `targetClasses`
  list before that list was updated to include the feature's new classes.
  Before relaying any subagent-reported metric (coverage, mutation score,
  test count, lint result), confirm what it actually covers, not just that
  the tool ran and returned a number.
- **A mandatory step cannot be silently skipped, downgraded, or
  rationalized away.** If an agent cannot complete a step the prompt
  marked mandatory, the correct move is to stop and report the specific
  blocker back to the orchestrator — not to proceed anyway with a caveat,
  and not to substitute a weaker check for the one actually required. A
  report that mentions skipping a mandatory step is a first-class finding,
  not a footnote.
- **PMD "fixed" means decomposed, not suppressed.** Grep the diff for any
  `@SuppressWarnings("PMD...")` outside the one narrow parameter-count/
  interface-override exception documented in `workflow.md`'s Constraints
  section. If found, remove it and rerun PMD to confirm the violation is
  real, then require decomposition instead — a green PMD run achieved by
  suppression is not a closed PMD loop.
- **A green acceptance-test suite doesn't prove real keyboard/focus
  behavior.** See `docs/testing.md`'s note on `ActionMap`-driven step
  definitions vs. genuine key events — if the feature involves keyboard
  focus crossing a window or component boundary, confirm at least one
  scenario exercises that through real input, not just the direct
  `ActionMap` shortcut, before trusting a full pass as proof it works.

## Escalation path when verification finds a real problem

1. **First failure** — send the same agent a corrective follow-up (via
   `SendMessage`, resuming it) naming the specific problem and how to fix
   it, including evidence (the actual error, diff, file content) — not
   just "this didn't work, try again." Most corrections land here.
2. **Second failure of the *same class*** — the correction from step 1
   didn't land, or the agent repeated a mistake it was already told
   about. Switch to `/fork` for the next attempt instead of a third
   fresh/resumed round — a fork inherits full context including the
   diagnosis from steps 1-2, so it starts already knowing what went
   wrong. Only escalate after a second same-class failure, not the first.
3. **If the fork also fails the same check**, that's a signal the problem
   is in the diagnosis or approach itself, not the executing agent — stop
   and reconsider rather than escalating further.

A fork is expensive per token (runs on the parent's own model, not Haiku)
and inherits an already-large conversation, so give it a tight, closeable
checklist rather than an open-ended "finish the rest." Put the cheapest,
most failure-prone self-checks first (e.g. "confirm the config change
actually landed before reporting the metric it enables") — a fork can run
out of its own session budget mid-checklist, and if it does, a checklist
ordered this way still leaves the highest-value confirmations done.
