@manual-verification
Feature: PMD and JaCoCo CI-enforced quality gates
  `mvn verify` enforces the function-size/complexity/parameter/duplication
  and coverage budgets documented in `.claude/workflow.md`, instead of
  relying on the agent's own judgment to follow them. Pure Swing
  layout/wiring classes with no real branching logic are excluded from
  both gates, the same way they're already excluded from PIT mutation
  testing.

  Scenario: A compliant codebase passes verify
    Given no non-excluded function exceeds cyclomatic complexity 8, 40 lines, or 4 parameters
    And no non-excluded code block duplicates another at or above PMD CPD's default 100-token threshold
    And repo-wide line coverage across non-excluded classes is at least 85%
    When `mvn verify` is run
    Then the build succeeds

  Scenario Outline: A single-metric PMD violation fails the build
    Given a non-excluded function <violates>
    When `mvn verify` is run
    Then the build fails with a PMD violation report identifying the offending function and rule

    Examples:
      | violates                                   |
      | has cyclomatic complexity greater than 8   |
      | is longer than 40 lines                    |
      | takes more than 4 parameters               |

  Scenario: Duplicate code fails the build
    Given two non-excluded code blocks duplicate each other at or above PMD CPD's default 100-token threshold
    When `mvn verify` is run
    Then the build fails with a PMD CPD violation report identifying both duplicate locations

  Scenario: Coverage below threshold fails the build
    Given repo-wide line coverage across non-excluded classes is below 85%
    When `mvn verify` is run
    Then the build fails with a JaCoCo coverage-check violation

  Scenario: Excluded pure-layout classes are not held to either gate
    Given a class on the pure-Swing-layout/wiring exclusion list has no unit test coverage and would otherwise exceed a PMD budget
    When `mvn verify` is run
    Then PMD does not fail the build over that class's complexity, length, parameter count, or duplication
    And JaCoCo does not count that class's lines toward or against the repo-wide 85% coverage threshold

  Scenario: workflow.md's coverage wording matches what's actually enforced
    Given `.claude/workflow.md`'s constraints section
    Then it states coverage is enforced repo-wide (e.g. "85% repo-wide line coverage"), not "on changed files"

  # Non-goals:
  #   - Moving PIT mutation testing into CI as a gate — stays a manual
  #     Step 6 command (mvn org.pitest:pitest-maven:mutationCoverage).
  #   - CRAP score (complexity x coverage composite) — no maintained tool
  #     for it against JaCoCo.
  #   - SpotBugs, SonarQube, or any static analysis tool beyond PMD.
  #   - True changed-files-only coverage enforcement (needs a diff-coverage
  #     tool beyond plain JaCoCo) — repo-wide is the accepted reading.
  #   - Writing new unit tests for a class purely to pad the coverage
  #     number once it's confirmed to be pure layout with no real logic.
  #
  # Risks:
  #   - PIT's <targetClasses> allowlist in pom.xml, the starting point for
  #     the new exclusion list, was found stale during spec drafting: it
  #     named two classes that no longer exist (SelectableMenu, MenuPanel
  #     — superseded by the widget-framework work in
  #     ui-component-framework.md), and its complement would otherwise
  #     have excluded several classes that are clearly not "pure layout
  #     with no logic to unit test" — e.g. ListWidget (146 lines),
  #     TableWidget (351 lines), RadioGroupWidget (285 lines), and
  #     SliderWidget (91 lines), all of which already have dedicated
  #     *Test.java files and real selection/scroll/bounds logic. Resolved
  #     via the intent doc's Clarifications: the exclusion list is built
  #     hybrid-style (start from PIT's list, pull back in any class with
  #     an existing dedicated unit test, exclude untested classes only
  #     after confirming they're genuinely pure layout), and this issue
  #     also removes PIT's two dead entries as a drive-by fix.
  #   - Retrofitting the gates onto the existing codebase "green by the
  #     time it's done" may still require some new test-writing for
  #     currently-untested classes that turn out to have real logic —
  #     actual size unknown until Step 4 runs PMD/JaCoCo for real and
  #     applies the hybrid derivation per class.
  #
  # Open questions:
  #   - None outstanding — the exclusion-list derivation method and the
  #     PIT stale-entry cleanup were both settled via a grilling round
  #     during spec drafting.
