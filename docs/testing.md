# Testing

Three test layers, run at different points, kept in separate file-naming
conventions so Maven can tell them apart automatically. Enforcement layers
on top of them (mutation testing, PMD/JaCoCo, Error Prone/NullAway, the
ArchUnit module-dependency gate) live in their own files below — this file
stays the short index once those grew past a single-file instruction budget.

## Unit tests

- Location: `src/test/java/**/*Test.java`
- Runner: Surefire, bound to `mvn test`
- Fast, no real I/O — the norm for new production code.
- Run a single test: `mvn test -Dtest=PlayerTest#movingRightIncreasesX`
- Property-based testing with jqwik — see [`testing-property-based.md`](testing-property-based.md).

## Acceptance and approval tests

Cucumber `.feature` scenarios and golden-fixture (glyph-grid) rendering
tests — see [`testing-acceptance.md`](testing-acceptance.md).

## Integration tests

- Location: `src/test/java/**/*IT.java`
- Runner: Failsafe, bound to `integration-test`/`verify` — **not** run by
  plain `mvn test`. Run them with `mvn verify`.
- Reserved for tests that need real I/O or cross-class wiring that unit
  tests shouldn't pay for on every run (e.g. `ModLoaderIT`, which loads
  actual mod content off disk instead of mocking the file read).
- Run a single integration test: `mvn verify -Dit.test=ModLoaderIT`

## Everything together

`mvn verify` runs all three layers: unit tests and the Cucumber suite via
Surefire, then integration tests via Failsafe.

## Enforcement gates

- [`testing-quality-gates.md`](testing-quality-gates.md) — mutation testing
  (PIT), PMD/JaCoCo, and the Error Prone/NullAway compile-time gates.
- [`testing-module-dependency.md`](testing-module-dependency.md) — the
  ArchUnit module-dependency and package-cycle gate.
