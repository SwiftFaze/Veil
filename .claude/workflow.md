# Development workflow (Uncle Bob-style agentic pipeline)

This project uses a constraint-first workflow for AI-assisted development,
gated by feature risk rather than one fixed process for everything. Two
paths exist:

- **High-risk path** — auth, payments, data integrity, or public APIs.
  Follow Steps 1-3 below in full before any implementation code is
  written: a written intent doc, a human-approved Gherkin spec, and an
  explicit human approval gate. Do not skip these for this category of
  feature.
- **Standard path** — everything else (the default). Still write a short
  intent doc (Step 1), but skip the blocking Steps 2-3 approval gate —
  go straight into Step 4's agile implementation loop instead: implement
  a slice, look at the result, reconcile the intent doc with what was
  actually built, continue. A `.feature` file is still required by
  Step 5 before the feature is done, it just doesn't have to exist,
  complete and approved, before implementation starts.

This split exists because requiring a fully pre-approved spec before any
code exists, for every feature, doesn't survive contact with
implementation: plans made before an agent starts working reliably
diverge from what actually gets built. Paying that approval-latency cost
is reserved for the class of change where getting it wrong is genuinely
expensive.

## Step order

1. **Intent** — a short written description of the feature already exists
   in `/specs/intent/<feature-slug>.md` before any code or spec is written.
   If it doesn't exist, ask the user for it instead of guessing.
    - If the intent already lives in a GitHub issue instead of being
      dictated fresh, use the `spec-intent` skill instead — give it the
      issue number and it creates/links a branch, moves the tracker item
      to in-progress, and derives `intent.md` from the issue's own
      description.
    - Intent is not written once and frozen — clarifying answers gathered
      while drafting the spec (high-risk path) or while implementing
      (standard path) get appended back here. `.feature` is always
      derived from the current state of this file, never the other way
      around.
    - **Standard path** (the default — anything not auth/payments/
      data-integrity/public-API): this intent doc is a starting point
      for Step 4, not a permanently maintained artifact — it isn't
      required to be indexed or kept up to date long-term (see
      `specs/intent/README.md`). Skip Steps 2-3's blocking approval gate
      entirely; go straight to Step 4 with an agile loop: implement a
      slice, look at the result, reconcile this doc with what was
      actually built, continue with the next slice. Write or regenerate
      the `.feature` file during Step 4/5 as behavior stabilizes, rather
      than requiring it complete and approved before code exists — it
      still must exist, be wired, and pass before the feature is marked
      done.
    - **High-risk path** (auth/payments/data-integrity/public API):
      continue to Step 2 below — this is unchanged from before.
    - See "Model selection" below — this step uses Claude Sonnet 5 for
      both paths.
2. **Spec (high-risk path only)** — generate a Gherkin feature file from
   the intent doc and save it to `/specs/features/<feature-slug>.feature`.
   Use the `spec-feature` skill (`.claude/skills/spec-feature/`) for this.
   Do not write implementation code in this step. The standard path skips
   this step — see Step 1's "Standard path" note above.
    - This step loops with Step 1: every clarifying answer updates
      `intent.md` first, then regenerates the `.feature` file from the
      updated intent, before asking what's still open. See the
      `spec-feature` skill for the full loop.
    - See "Context & session management" below — checkpoint after each
      round of revision, especially once the spec settles.
    - See "Model selection" below — this step uses Claude Sonnet 5 (or
      Claude Opus 5 for auth/payments/data-integrity/public-API specs).
3. **Human approval (high-risk path only)** — stop and wait for the human
   to approve the `.feature` file before writing any implementation code.
   Do not proceed on your own. The standard path skips this step — see
   Step 1's "Standard path" note above; there is no blocking approval
   gate before implementation begins.
    - See "Context & session management" below — checkpoint right after
      approval, since that's a clean boundary worth starting Step 4 from.
    - See "Model selection" below — this step is a human decision, no
      model involved.
4. **Implementation** — once the spec is approved, hand off to Claude
   Haiku 4.5 to implement the feature and write unit tests.
    - **Default to a fresh agent pinned to Haiku 4.5, not `/fork`, for
      this handoff — the goal is minimum token spend, and Haiku's
      per-token cost plus a tight, self-contained prompt beats a fork's
      "free" inherited context running on the pricier parent model.**
      The orchestrator (the Sonnet 5 session running Steps 1-3) must do
      the exploration once and compress it directly into the handoff
      prompt: an explicit list of every file path it needs (with line
      numbers), the actual code being referenced (not just its name), and
      the exact reasoning/decisions already made (e.g. "don't redefine
      step X, it already exists at Y and collides") — everything the
      agent would otherwise have to rediscover by reading files.
    - **Tell the agent explicitly not to scan or explore the codebase
      beyond the files listed.** If something it needs turns out to be
      missing, wrong, or insufficient, it should stop and report exactly
      what's missing rather than grepping/globbing around for it — the
      orchestrator can then supply the missing piece and resume it. A
      prompt with a complete file list plus this instruction is what
      makes the cheaper model actually cheaper; without it, the agent
      falls back to exploring the codebase itself.
    - **Fall back to `/fork` (accepting it runs on the parent's model,
      not Haiku) only when the context genuinely can't be compressed into
      a prompt economically** — e.g. the relevant material is too
      sprawling, exploratory, or spread across too many files/decisions
      to excerpt without the prompt-writing itself costing nearly as much
      as just forking. This should be the exception, not the default.
    - **A fresh Haiku agent is also the right choice, not just the cheap
      one, whenever true isolation is needed** — e.g. multiple tickets
      being implemented in parallel across separate worktrees, where a
      fork's shared-context model isn't appropriate anyway. Same
      excerpt-pasting rule applies.
    - **Run independent Step 4 implementations in parallel as a normal
      option, not only as an edge-case exception.** When more than one
      ticket is ready at once, launch a separate fresh Haiku agent per
      ticket, each in its own git worktree — this is the default way to
      work through multiple ready tickets, not a fallback reserved for
      unusual circumstances, since each ticket's Step 4 agent is already
      isolated and disposable by design.
    - Keep every function within the complexity budget below. These are
      enforced by CI, not by your judgment alone.
    - **Close the PMD loop before moving to Step 5.** After implementing,
      run `mvn verify` (or `mvn org.apache.maven.plugins:maven-pmd-plugin:check`
      and `:cpd-check` directly for a faster inner loop), fix every
      violation it reports, and rerun until it's clean. This is a
      deterministic loop the agent must satisfy — not a one-time
      self-check against the `uncle-bob-craft` checklist below — because
      PMD is a CI-enforced gate (see `pom.xml`): an implementation that
      hasn't closed this loop isn't done, no matter how clean the code
      looks by eye.
    - **Respect the module dependency direction.** `ModuleDependencyTest`
      (a plain JUnit test using ArchUnit — see Constraints below) fails
      `mvn verify`/`mvn test` if "engine" code (everything outside
      `com.swiftfaze.veil.ui`, excluding the `Main` composition root and
      the `sandbox` package) depends on `com.swiftfaze.veil.ui`, or if a
      class in `com.swiftfaze.veil.ui.widget` depends on a screen class
      that sits directly in `com.swiftfaze.veil.ui`. Fix a violation by
      inverting the dependency, extracting an interface, or moving the
      offending class — not by weakening the rule.
    - Before considering a file or logical unit done, self-apply the
      `uncle-bob-craft` skill's "writing or refactoring code" checklist
      (small single-purpose functions, dependencies pointing inward,
      design patterns only when duplication/variation actually justifies
      them) — this is a self-check the same Haiku agent runs while
      writing, not a separate review pass or extra agent call, so it adds
      no cost and does not change the "implementation code is not
      reviewed, by design" rule in the review table below. The
      orchestrator's handoff prompt should tell the agent to do this
      explicitly, the same as it states the complexity budget.
    - **If the change adds or touches a Swing panel/widget, its padding,
      font sizes, or colors must follow `docs/ui-styling.md`** (outer
      padding, gap between components, h1/h2/p sizes, and the rule that a
      color is a `WidgetTheme` key, never a hardcoded literal) — since Step
      4 agents are told not to explore beyond the files they're handed, the
      orchestrator's handoff prompt must include `docs/ui-styling.md`
      itself whenever the ticket touches UI, not just describe the rules
      secondhand.
    - **If the change touches Swing rendering, layout, sizing, or text
      content, visually verify it before considering the step done** —
      compiling and `mvn test` passing only proves the code runs, not that
      it renders correctly; this project's tests don't assert on pixel
      layout or actual rendered text. See `docs/ui-verification.md` for
      the concrete how-to (render the real component to an image via a
      throwaway diagnostic class, inspect it with the `Read` tool). This
      is separate from and does not replace the repo-specific Step 4.5
      manual playtest in `CLAUDE.md` — that's a human verifying real
      interactive feel, this is the agent verifying its own rendering
      before handing the step off.
    - See "Context & session management" below for checkpoint guidance —
      this step is the most common place sessions run long.
    - See "Model selection" below — this step uses Claude Haiku 4.5.
5. **Acceptance tests** — wire the approved `.feature` file to the project's
   test runner so it's executable, not just documentation.
    - Continue in the same fresh Haiku agent from Step 4 rather than
      starting over — see "Context handoff rule" below for when isolation
      (a new agent, or the `/fork` fallback) is actually warranted instead.
      Either way, this step needs to know exactly what was implemented —
      don't hand it off as a bare ticket reference to a blank-context
      subagent, or it will re-explore the diff to figure out what changed.
    - **Before reporting this step done, check for duplicate step
      definitions.** Cucumber matches step text regardless of the
      Given/When/Then keyword, so two methods annotated with the same
      literal step text — even under different keywords — is always a
      duplicate. A duplicate step definition poisons Cucumber's whole
      glue registry, not just the two colliding methods: every scenario
      in the suite can fail, cascading into completely unrelated feature
      files in a way that makes the real cause hard to spot (see
      `docs/testing.md`'s troubleshooting note). Run:
      `grep -ohE '@(Given|When|Then)\("[^"]*"\)' src/test/java/com/swiftfaze/veil/steps/*.java | sed -E 's/@(Given|When|Then)\("(.*)"\)/\2/' | sort | uniq -d`
      and confirm it prints nothing. If the same literal step text
      genuinely needs different behavior depending on whether it's a
      setup precondition or a later assertion, that's a sign the text
      needs to be reworded into two distinct steps, not that two
      annotations on the same text are safe — see
      `UiComponentFrameworkSteps.theConfirmationPopupIsShown()` for the
      correct single-method pattern (guard with `if (x == null) { build
      it } else { just assert }`) when the same text is reused as both a
      fresh-build precondition and a later assertion.
    - **If this step touches a step-definitions file other `.feature`
      files also depend on** (this repo's `UiComponentFrameworkSteps.java`
      backs several features at once), run `mvn clean test` (or
      `mvn clean verify`) **twice in a row** after your changes and
      require identical results before reporting done. "Tests pass" once
      isn't sufficient evidence when shared test infrastructure changed —
      a real problem there can manifest as flaky, run-order-dependent
      failures instead of a clean, deterministic one.
    - See "Context & session management" below — checkpoint per scenario,
      not just at the end of the ticket.
    - See "Model selection" below — this step uses Claude Haiku 4.5.
6. **Mutation testing** — run the mutation test suite against new/changed
   code before considering the feature done. This is the check on the unit
   tests, since they won't be manually reviewed.
    - See "Model selection" below — tooling only, no model involved.
7. **Documentation** — update the codebase docs affected by this change
   before marking the feature done. This is not optional cleanup, it's part
   of the definition of done:
    - Continue in the same fresh Haiku agent from Steps 4-5 rather than a
      blank-context subagent (see "Context handoff rule" below). This step
      needs to know precisely what changed to write an accurate doc update
      — handing it a ticket reference alone forces it to re-derive that
      from the diff, the same cost problem Step 4 had.
    - If the change adds/changes a public API endpoint, method, or
      configuration property, update the relevant reference doc (OpenAPI
      description, `docs/architecture.md`, public API doc, or Javadoc for a
      library's public surface).
    - If the change introduces a new domain concept, non-obvious design
      decision, or deviates from an existing pattern, add or update an entry
      in `docs/` explaining it — don't just leave it discoverable only by
      reading the diff.
    - If the change is a library, update the CHANGELOG.
    - If implementation adds, removes, or renames a `specs/features/*.feature`
      file (e.g. a superseded spec being replaced), update its row in the
      `## Index` table in `specs/features/README.md` in the same change —
      don't leave a dangling or missing entry for the human reviewer to
      catch.
    - If nothing user-facing or architecturally significant changed (e.g. a
      bugfix with no behavior/contract change), state explicitly that no doc
      update was needed rather than skipping the step silently.
    - Do not put narrative domain explanations into CLAUDE.md itself — that
      file stays behavioral/instructional. Narrative and reference
      documentation belongs in `docs/`, with CLAUDE.md linking to it only if
      the agent needs to be pointed there.
    - See "Model selection" below — this step uses Claude Haiku 4.5.

## Context & session management

Claude Code auto-compacts by default once context usage gets high (around
83% capacity). That's a safety net, not a strategy — it fires reactively,
regardless of whether you're mid-edit or mid-thought, and can produce a
worse summary than a checkpoint taken at a clean boundary. Don't rely on
it as your only mechanism; checkpoint proactively at natural breakpoints
in every step below, not just when a limit warning appears.

### Steps 2-3 (Spec drafting + human approval)

This is a back-and-forth, human-driven step — revising the Gherkin spec
and intent doc across several rounds of questions and edits — and it
accumulates context the same as any agent-heavy step, even when most of
the turns are the human talking directly to the main session rather than
delegating to a subagent. Checkpoint with `/compact` after each round of
revision is settled (not mid-edit), and especially right after the spec
is approved — that approval is a clean, meaningful boundary worth
starting the next step from fresh.

### Steps 4-5 (Implementation + acceptance tests)

These are the steps most likely to run long. Checkpoint with `/compact`
after each sub-unit of work is verified working — not just once at the end
of the whole ticket:

- In Step 4: after each file or logical unit passes its own tests, before
  moving to the next one.
- In Step 5: after each scenario from the `.feature` file is wired and
  passing, before wiring the next scenario.

Do not wait until output feels sluggish or repetitive to compact — by
then the session is already in degraded mode. Compacting at a clean
boundary (tests green, nothing mid-edit) produces a much better summary
than compacting mid-failure with half-applied changes in flight.

### Steps 6-7 and elsewhere

Usually short enough that this rarely matters. If a session does run long
here too, apply the same principle: checkpoint at a clean boundary, not
reactively.

### Between pipeline steps

Prefer starting a fresh session (`/clear` or a new session) over
continuing indefinitely, once a step is fully complete and its artifact is
committed to disk (approved `.feature` file, working implementation,
passing acceptance tests, updated docs). The next step's real memory is
that artifact, not the conversation — so nothing is lost by starting
clean, and the new session runs faster with a smaller window to manage.

Before clearing or starting fresh, write a short status note either in the
ticket/PR description or as a commit message, stating: which step just
finished, which files were touched, and what the next step should do
first. This is what a `/resume` or a fresh session should read first to
reconstruct state — don't rely on the agent re-deriving this from a full
diff read.

### On `/resume`

When resuming a session (new terminal, next day, after a `/clear`), first
read: the ticket's current git diff/log, the approved `.feature` file for
this ticket, and any status note left per the paragraph above. Do this
before touching any other file — it's the fastest way to reconstruct
exactly where the previous session left off without re-exploring the
whole codebase.

### Signs a checkpoint is overdue (don't wait for these — they mean you're already late)

- Repeating a fix already tried earlier in the same session
- Referencing a file structure that's since changed
- Losing track of which files in a multi-file change have been touched
- Responses feeling more generic or hedged than earlier in the session

## Context handoff rule

Steps 4, 5, and 7 are a single continuous handoff, not three separate
delegations: stay in the same fresh Haiku agent across 4→5→7 for one
ticket rather than re-briefing a new one at each step (see Step 4 above
for the full default-vs-`/fork` reasoning). The one question worth asking
before any of the three is whether this specific piece of work needs true
isolation — parallel work in a separate worktree — since that's the one
case where a fresh, separately-briefed agent (or `/fork`) is actually the
right call instead of continuing the existing one.

## Verifying subagent completions

Never relay a subagent's "done" report as fact without checking it
yourself first — this applies to every subagent completion, not only
visual-verification claims (`docs/ui-verification.md` already states this
for that one case; it generalizes). A subagent's summary describes what
it intended to do, not necessarily what it did, no matter how confident
or detailed the report reads. Before treating any step as finished, the
orchestrator re-runs the actual check itself: open the file the subagent
claims to have produced, run the command it claims passed (`mvn verify`,
the specific test, the specific grep), read the diff of what it actually
changed. Confidence and detail in a report are not evidence.

**If your independent check finds the subagent's work is wrong, follow
this escalation path** rather than either blindly re-dispatching the same
prompt again or jumping straight to a fork:

1. **First failure** — send the same agent a corrective follow-up (via
   `SendMessage`, resuming it) that names the specific problem you found
   and how to fix it, including the evidence (the actual error, the
   actual diff, the actual file content) — not just "this didn't work,
   try again." Most corrections land here.
2. **Second failure of the *same class* of mistake** — this means the
   correction from step 1 didn't land, or the agent repeated a mistake it
   was already told about. Switch to `/fork` for the next attempt instead
   of dispatching a third fresh/resumed round on the original agent — a
   fork inherits your full context, including the diagnosis from steps
   1-2, so it starts already knowing what went wrong instead of
   rediscovering it. Only escalate to fork after this second same-class
   failure, not on the first one.
3. If the fork also fails the same check, that's a signal the problem is
   in the diagnosis or the approach itself, not the executing agent —
   stop and reconsider rather than escalating further.

## Model selection

Not every step needs the same model. Pin concrete models per step rather
than leaving it to whoever happens to be running the session — this keeps
cost predictable and avoids under-provisioning judgment-heavy steps.

- **Step 1 (Intent)** — Claude Sonnet 5, for both paths. Even a short
  standard-path intent doc involves enough judgment (scope boundaries,
  what's worth writing down) to warrant the balanced model over the
  cheapest one.
- **Step 2 (Spec, high-risk path only)** — Claude Sonnet 5, or Claude
  Opus 5 for specs touching auth, payments, data integrity, or public
  APIs — the same class of feature Step 3 won't let skip human approval.
  The standard path skips this step entirely; no model is spent on it.
- **Extended thinking for Step 1, and Step 2 on the high-risk path** — a
  separate lever from model choice. Use it for the judgment-heavy parts
  of these steps: resolving scope boundaries, deciding what's in/out per
  the intent doc, designing scenarios that actually catch bugs (e.g. an
  asymmetric fixture to catch an x/y transposition, not a symmetric one
  that would pass either way). Trigger it with "think hard" / "think
  harder" in the prompt, or a session-wide thinking setting. Skip it for
  the mechanical parts of these steps (reading files, running `gh`
  commands) — no ambiguity to reason through, so it's just added latency.
- **Step 3 (Human approval, high-risk path only)** — no model. This is a
  human decision gate, not agent work. The standard path has no
  equivalent blocking gate.
- **Steps 4-5 (Implementation, Acceptance tests)** — Claude Haiku 4.5, via
  a fresh, non-fork agent with a self-contained, excerpt-rich prompt — see
  Step 4 above and "Context handoff rule" above for the full default vs.
  `/fork` reasoning.
- **Step 6 (Mutation testing)** — no model. This step is tooling
  (running the mutation test suite), not agent judgment.
- **Step 7 (Documentation)** — Claude Haiku 4.5, continuing in the same
  fresh Haiku agent as Steps 4-5 (see "Context handoff rule" above),
  since it only needs to describe what already changed rather than make
  new judgment calls.

If the model lineup changes, update the names here rather than reverting
to vague relative terms — a stale name is easier to spot and fix than a
permanently vague policy.

## Constraints (mechanically enforced, not optional)

- Max function length: 40 lines
- Max cyclomatic complexity: 8
- Max function parameters: 4
- Minimum line coverage: 85% repo-wide (JaCoCo)
- Module dependency direction (ArchUnit, `ModuleDependencyTest`, a plain
  JUnit test run via `mvn test`/`mvn verify`): "engine" code (everything
  outside `com.swiftfaze.veil.ui`, excluding the `Main` composition root
  and the `sandbox` package — see `docs/testing.md`'s "Module dependency
  gate" section for why those two are excluded) must not depend on
  `com.swiftfaze.veil.ui` at all; classes in `com.swiftfaze.veil.ui.widget`
  must not depend on screen classes that sit directly in
  `com.swiftfaze.veil.ui` (screens may depend on widgets, not the
  reverse).

These are enforced by the linter/CI config in this repo, not by asking the
agent to "try to keep things clean." If a change can't meet these limits,
stop and flag it rather than disabling the check — with one narrow,
already-practiced exception: a method overriding a JDK/library interface
whose signature mandates more than 4 parameters (e.g.
`Border.paintBorder(Component, Graphics, int, int, int, int)`, see
`RadioGroupWidget.RadioOptionBorder` and `TableWidget.AccentableCellBorder`)
may suppress PMD's `ExcessiveParameterList` rule with
`@SuppressWarnings("PMD.ExcessiveParameterList")` plus a comment naming
the interface — the parameter count isn't a choice the code made, it's
the contract being implemented. This exception is specific to parameter
count on an unavoidable interface override; it does not extend to
complexity, length, coverage, or the module dependency rule, none of
which have an equivalent "the interface forced it" excuse.

**Not mechanically enforced — self-applied only:** the Single Level of
Abstraction Principle (SLAP — no function may call more than one level
of abstraction below itself) has no PMD rule or other tool backing it in
this repo; there is no off-the-shelf static-analysis check for it.
Treat it as design guidance applied via the `uncle-bob-craft` skill's
checklist in Step 4, not a build gate — violating it won't fail
`mvn verify` the way the constraints above do. Don't describe it as
enforced elsewhere in this repo's docs; if a mechanical check for it
becomes practical later, promote it into the list above.

The function-length/complexity/parameter/coverage numbers above are a
deliberate, adjustable dial for agent-authored code, not a fixed
constant. Robert C. Martin has described moving his own CRAP
(complexity x coverage) threshold from 4 — tuned for human-authored code
— toward 6-8 for agent-authored code, and says he hasn't found the
ceiling yet: agents have a larger and more reliable short-term memory
than humans, so they can hold more local complexity without getting
lost the way a human would. If these numbers change in this repo, treat
it as a deliberate, documented experiment (record the new value and the
reasoning here), not silent drift — don't loosen a threshold casually
just because one change doesn't fit under it.

## What gets reviewed vs. not

| Artifact                         | Written by | Reviewed by human?                                                |
|-----------------------------------|------------|--------------------------------------------------------------------|
| Intent doc                       | Human      | N/A (it's the source)                                             |
| Gherkin acceptance spec          | Agent      | Yes — always, before implementation starts                        |
| QA procedures                    | Agent      | Yes — rigor scales with criticality                               |
| Implementation code              | Agent      | No, by design — leverage comes from not reading it                |
| Unit tests                       | Agent      | No, by design                                                     |
| Mutation test results            | Tooling    | Human skims summary only                                          |
| Codebase docs (docs/, CHANGELOG) | Agent      | Spot check — rigor scales with how public/critical the surface is |

On the standard path, no Gherkin spec exists ahead of implementation, so
the "always, before implementation starts" review timing above applies
to the high-risk path only — the `.feature` file the standard path
produces during Step 4/5 is still reviewed, just after the fact rather
than as a blocking gate.

## Notes for the agent

- Classify a feature as high-risk (auth, payments, data integrity, or
  public APIs) before choosing a path — one matching characteristic is
  enough to require the full high-risk path, not all of them. If
  genuinely unsure which path applies, default to the high-risk path:
  the cost of an unnecessary approval gate is much lower than skipping
  one that was actually warranted.
- If the intent doc is missing or ambiguous, use the `grilling` skill
  (`.claude/skills/grilling/`) to ask what's unclear rather than inventing
  scope — it batches every open question into dependency-ordered rounds
  instead of a flat prompt, which fits this repo's iterative intent/spec
  loops better than a one-shot `AskUserQuestion` call. This applies to
  Steps 1-2 and to the `brainstorm-issue`/`brainstorm-milestone` skills
  too. Reserve `AskUserQuestion` for a genuinely standalone, self-contained
  multiple-choice pick that has no other open question hanging off it
  (e.g. confirming a milestone match).
- If a requested change would violate a constraint above, say so explicitly
  and propose a decomposition instead of quietly exceeding the limit.
- Never mark a feature "done" without the acceptance tests passing against
  the approved `.feature` file.
- Never mark a feature "done" without completing the documentation step
  (Step 7), even if the conclusion is "no doc update needed" — say so
  explicitly rather than omitting the step.
- Never leave the linked GitHub issue open once its PR has merged (or,
  for a `brainstorm-issue`/`brainstorm-milestone` issue with no separate
  spec pipeline, once the work it describes is actually done). See the
  repo-specific closing step in root `CLAUDE.md` for why this can't be
  left to GitHub's own auto-close.
