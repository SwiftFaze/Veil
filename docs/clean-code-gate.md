# The Clean Code gate

One command, run identically by the agent doing the work and the agent
checking it:

```
bash .claude/tools/check-clean.sh
```

## Why it exists

The recurring failure is a subagent reporting "done" and the orchestrator
bouncing it for failing tests, smells, or Clean Code violations. That loop is
expensive because the two sides were checking *different things* — the subagent
checked whatever it remembered from its handoff prompt, the orchestrator checked
whatever it thought to look for. Same script, same rules, same output closes the
gap by construction: there is nothing the orchestrator can reject the work for
that the subagent could not have seen first.

This is `CLAUDE.md`'s "if it is mechanically checkable, write the check, not the
rule" applied to `uncle-bob-craft`. The prose skill still owns the judgment;
everything a tool can decide has moved here.

## Scope: added lines, not whole files

By default the gate judges **only the lines your change added**, measured
against the merge-base with `develop`, including uncommitted and untracked
work. Two reasons:

- The rules in `.pmd-clean-code.xml` find ~1000 violations across the existing
  codebase. Binding them to `mvn verify` would have turned `develop` red the day
  they landed, and a gate that is red on arrival teaches everyone to ignore it —
  the same reasoning as the budget baselines in `docs/instruction-files.md`.
- You are accountable for the lines you wrote, not for the file you happened to
  open.

New files are included even before `git add`, because an agent's brand-new class
is untracked and `git diff` cannot see it.

## Running it

**Subagent, before reporting a task finished** — this is mandatory, see
`.claude/workflow.md` Step 4:

```
bash .claude/tools/check-clean.sh
```

Exit 0 means the mechanical checks pass. It does **not** mean you are done: the
run ends by printing a judgment checklist you still owe answers to.

During the edit loop, `--fast` skips `mvn verify` for a quicker signal. A
`--fast` run is never sufficient to report a task finished, and the script says
so in its own output.

**Orchestrator, verifying a subagent's report** — the same command, from the
same branch:

```
bash .claude/tools/check-clean.sh
```

If the subagent worked in a git worktree, run it there, or pass
`--base <the branch point>`. What to check in the report:

1. Exit status is 0.
2. Every **advisory** finding the script listed has a disposition in the report
   — fixed, or one line saying why it is correct as written.
3. Every **judgment checklist** line has a PASS/FAIL *and evidence naming a
   file, function, or test*. "All good" is not an answer, and a checklist
   returned without evidence is the same signal as a skipped step.

`.claude/subagent-delegation.md` still applies: a report is not evidence. The
point of this tool is that verifying it costs one command.

Other flags: `--all` (whole repo, for baselining — not the gate), `--files`
(whole changed files rather than added lines), `--base <ref>`.

## What is checked, and how far it automates

"Automatable" means a tool decides pass/fail with no human reading the code.

| # | Concern | Automation | Enforced by |
|---|---|---|---|
| 1 | Method length (>40), cyclomatic complexity (>8), parameters (>4) | Full | `.pmd-minimal.xml`, repo-wide at `mvn verify` |
| 1 | Nesting depth (>3), cognitive complexity (>15), NPath (>200) | Full | `.pmd-clean-code.xml` |
| 2 | SRP — method/field/public counts, fan-out, God class | **Partial** — proxies for cohesion, not proofs | `.pmd-clean-code.xml` (advisory) + checklist |
| 3 | Naming conventions, short/long names, linguistic (`isX` returns boolean) | Full | `.pmd-clean-code.xml` |
| 3 | Abbreviations | **Partial** — closed deny-list | `VeilAbbreviatedName` (advisory) |
| 3 | Intent-revealing names | **Judgment** | Checklist |
| 4 | Duplicated blocks (50+ tokens), repeated literals | Full | CPD + `AvoidDuplicateLiterals` |
| 5 | Commented-out code, TODO/FIXME, empty bodies, oversized comments | Full | script + PMD |
| 5 | Comments explaining *what* instead of *why* | **Judgment** | Checklist |
| 6 | Missing asserts, assert count, `@Test` misuse, control flow in tests | Full / partial | `.pmd-clean-code.xml` |
| 6 | FIRST, AAA structure, one reason to fail | **Judgment** | Checklist |
| 7 | Unused fields, methods, locals, parameters, assignments, imports | Full | `.pmd-clean-code.xml` |
| 8 | Swallowed/broad catches, lost stack traces, `printStackTrace`, `println` | Full | `.pmd-clean-code.xml` |
| 8 | Magic numbers and strings | **Partial** — literals are legitimate in a renderer | `VeilMagicNumber` (advisory) |

Test-quality rules run only against `src/test`; SRP rules only against
`src/main`. The script routes them.

### Blocking vs advisory

**Blocking** rules are high-precision: a hit is a defect. They fail the gate.

**Advisory** rules are heuristic proxies — a 2D renderer legitimately contains
numbers, a tile coordinate is legitimately called `x`, and a class can pass
every count and still do two things. They print with `file:line` and must each
be dispositioned in the completion report, but they do not fail the gate on
their own.

Advisory is not "ignorable". An advisory finding with no disposition is an
incomplete report.

## Known carve-outs, and why

Each of these was measured against this repo, not assumed:

- **`x y z w h dx dy dw dh cx cy g g2 e id`** are exempt from the short-name
  rule. The unmodified rule produced 224 hits, of which `e` (ActionEvent, 60),
  `x`/`y` (tile coordinates, 48), `g` (Graphics2D, 16) and `id` (43) are all
  clearer than any longer name. Names like `r` and `c` are still flagged —
  checking their sites showed genuine "what is this?" locals.
- **`logger` / `log`** are exempt from the constant-name pattern. It is the
  slf4j convention the repo already follows; without this the rule fires on
  every new file.
- **`VeilNoLogicInTests` is advisory**, not blocking. It fires on
  `NoDuplicateStepDefinitionsTest`, which loops over step definitions on
  purpose. Structural tests that scan the codebase legitimately contain a loop.
- **Assert ceiling is 3, not 1.** One assert per test is the ideal, but a strict
  1 flags every legitimate "assert the value AND that it was persisted" pair and
  trains people to suppress the rule. The checklist enforces the real rule — one
  *concept* per test.
- **`GenericsNaming` and `AvoidLosingExceptionInformation` are absent.** PMD
  7.17 reports both as scheduled for removal in PMD 8.
- **`FieldNamingConventions`'s `staticFieldPattern` matches `constantPattern`
  (UPPER_SNAKE).** `WidgetTheme`'s color fields are `public static` (not
  `final`, so `applyTheme()` can repopulate them from a mod-loaded theme) but
  are conceptually fixed named color slots, each name mirroring a theme JSON
  key 1:1 — the default camelCase `staticFieldPattern` would rename
  `NORMAL_TEXT` to `normalText`, breaking that mapping for every one of its
  12 pre-existing fields for no benefit. Surfaced 2026-09-07 when adding a
  13th field (`TABLE_HEADER_TEXT`) put a line in this file into a diff for
  the first time since the gate shipped.

## Suppressions

`@SuppressWarnings("PMD...")` on an added line fails the gate. "Fixed" means
decomposed. The single documented exception is `ExcessiveParameterList` on a
method overriding a JDK/library interface whose signature mandates 5+
parameters — see `docs/testing.md` § "Code quality gates".

If a rule is genuinely wrong for your case, **stop and report the blocker**.
Do not suppress it, and do not report the task done with a caveat.

## The judgment checklist

Printed by the script when the mechanical checks pass. These cannot be
automated; they are the part `uncle-bob-craft` still owns. Answer every line
with PASS or FAIL **and one clause of evidence naming a file, function or
test**.

**SLAP — one level of abstraction per function.** FAIL if any function you
added both calls named steps *and* does its own detail work (index arithmetic,
string building, null checks) in the same body.
*Pass looks like:* `renderRow` calls `drawGlyph`/`drawBorder` and contains no
arithmetic of its own. *Fail looks like:* `renderRow` calls `drawGlyph` and also
computes `x * TILE + offset - 1` inline.

**SRP — one sentence, no "and".** FAIL if describing any class you touched needs
"and" to be accurate.
*Fail looks like:* "parses the mod JSON **and** registers the entities".

**Naming — understandable without the implementation.** FAIL if a reader must
open the body to know what a name holds or does, or if any name needed a comment
to explain it. Renaming is the fix, not commenting.

**Why-not-what comments.** FAIL if any comment you added restates the code below
it. A comment earns its place by explaining a decision, a constraint, or a
non-obvious consequence.
*Fail looks like:* `// increment the counter` above `counter++`.
*Pass looks like:* `// PMD's minimum is strictly-greater-than, verified 2026-09-06`.

**Test intent — one reason to fail.** FAIL if any test you added could fail for
two unrelated reasons, or if its name does not say which reason.
*Fail looks like:* `testPlayer()`. *Pass looks like:*
`movingRightIncreasesX()`.

**AAA structure.** FAIL if arrange/act/assert are not visibly in that order, or
an assertion appears before the act.

**No new debt.** FAIL if you added a code path that exists only to make a test
pass, or an abstraction with a single caller added "for later".

## Thresholds are dials

Every number here is a dial set against this codebase, not a law. If you change
one, change it in `.pmd-clean-code.xml` **and** in the table above in the same
commit, and say why in the commit message. Do not loosen a threshold because one
change did not fit under it — that is the decomposition signal working.
