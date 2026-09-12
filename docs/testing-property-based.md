# Property-Based Testing with jqwik

This document covers property-based testing in Veil, the complementary approach to example-based unit tests. See `docs/testing.md` for the broader testing strategy.

## When to Use Property Tests

A **property test** is the right tool when the code implements an **invariant over a range**:
- Bounds and safety: "any coordinate outside the world returns null and is not walkable"
- Determinism: "the same inputs always produce the same outputs"
- Algebraic properties: "a value is never negative given non-negative inputs"
- Commutativity/associativity: "operation order doesn't matter"

An **example test** is still the right tool for:
- Specific behavior: "this input produces this exact output" (the author already knows this is correct)
- Stateful sequences: "action A, then B, then C produces state D" (examples shine here)
- Edge cases that aren't invariants: "index 5 in a 10-element list works"

In practice: **One property test replaces dozens of example tests** by automatically generating inputs. But they complement each other — use both. The property tests in this repo focus on the highest-value invariants: world bounds safety, stat computation determinism, and formula non-negativity.

## Assert vs Boolean Return

All property tests in this repo use **assertions** (e.g., `assertEquals`, `assertTrue`) in `@Property` methods. This is consistent with:
1. Existing example tests, which also use assertions
2. jqwik's shrinking: when a property fails on a large input, jqwik shrinks it to the minimal failing case and reports the assertion message, giving clearer failure output

There is no convention rule here (some teams use `boolean @Property` returns, jqwik supports both), but assertions are preferred because they integrate with IDE debugging (breakpoints trigger on assertion failures) and produce better error messages.

**PMD's `UnitTestShouldIncludeAssert` rule targets `@Test` but not `@Property` methods** — so there is no added complexity in configuration.

## Try Count and Wall-Clock Cost

To balance test coverage with CI speed, Veil caps jqwik's default try count at **100 tries per property** (jqwik's default is 1000).

Measured wall-clock time for `mvn test` with 3 properties (100 tries each):

```
Before adding property tests:  ~12 seconds
After adding property tests:   ~15 seconds
Delta:                         ~3 seconds (25% increase, acceptable)
```

This is configured in `src/test/resources/junit-platform.properties`:
```
jqwik.tries.default=100
```

## Fixed Seed for CI Reproducibility

All property tests in CI use a fixed seed (configured in `junit-platform.properties`):
```
jqwik.seed.initial=42859154278924201
```

This ensures:
1. **Reproducibility**: if a property fails in CI, the exact same seed reproduces the failure locally
2. **Consistency**: every CI run tests the same input distribution
3. **Bisectability**: you can revert a commit and confirm the property was passing before a change

If a property fails, the test output reports the seed — you can then pass it to jqwik to reproduce locally (via the `@Property(seed = "...")` annotation or by setting the same seed in your own properties file).

## Configuration Files

- `src/test/resources/junit-platform.properties` — jqwik configuration (try count, seed)
- `src/test/java/com/swiftfaze/veil/testing/property/VeilArbitraries.java` — shared arbitrary generators for domain types
- Property test classes: `*Test.java` files in the same package as their subject (e.g., `WorldSceneTest` lives in `com.swiftfaze.veil.world`)

## Shared Arbitraries

The `VeilArbitraries` utility provides reusable generators for common types:
- `worldDimension()` — world width/height [1, 200]
- `coordinate()` — any x/y coordinate, including out-of-bounds [-50, 250]
- `level()` — game levels [0, 200]
- `calcExpression()` — well-formed CalcExpressionParser expressions (recursive, depth-bounded)
- `seed()` — random seeds (for future world-gen tests)

Use `@Provide`-annotated methods in test classes to delegate to these, or call them directly if jqwik's `@ForAll(supplier = "...")` pattern fits.

## Examples

### Property: Bounds Safety

```java
@Provide
Arbitrary<Integer> worldDimensions() {
    return VeilArbitraries.worldDimension();
}

@Provide
Arbitrary<Integer> coordinates() {
    return VeilArbitraries.coordinate();
}

@Property
void tileReturnsNullAndIsWalkableReturnsFalseOutOfBounds(
        @ForAll("worldDimensions") int width,
        @ForAll("worldDimensions") int height,
        @ForAll("coordinates") int x,
        @ForAll("coordinates") int y) {
    WorldScene scene = sceneOf(width, height);

    // Call both methods for every generated coordinate to verify:
    // 1. Neither throws an exception for any coordinate (in- or out-of-bounds)
    // 2. Out-of-bounds coordinates return null and false respectively
    Tile tile = scene.getTile(x, y);
    boolean walkable = scene.isWalkable(x, y);

    boolean isInBounds = x >= 0 && x < width && y >= 0 && y < height;
    if (!isInBounds) {
        assertNull(tile);
        assertFalse(walkable);
    }
}
```

### Property: Determinism

```java
@Property
void applyStatsAtLevelIsDeterministic(
        @ForAll("growthCalcs") String growthCalc,
        @ForAll("levels") int level) {
    PlayerClass testClass = new PlayerClass("test:det", "Determinism",
            Map.of("strength", new PlayerClass.StatCurve(10, growthCalc)));
    Stats stats1 = new Stats();
    Stats stats2 = new Stats();
    testClass.applyStatsAtLevel(stats1, level);
    testClass.applyStatsAtLevel(stats2, level);
    assertEquals(stats1.getStrength(), stats2.getStrength());
}
```

### Property: Non-Negativity

```java
@Property
void attackPowerAndDefenseAreNonNegative(
        @ForAll("nonNegativeStats") int strength,
        @ForAll("nonNegativeStats") int dexterity,
        @ForAll("nonNegativeStats") int constitution) {
    Stats stats = new Stats();
    stats.setStrength(strength);
    stats.setDexterity(dexterity);
    stats.setConstitution(constitution);
    assertTrue(stats.getAttackPower() >= 0);
    assertTrue(stats.getDefense() >= 0);
}
```

See the property test files themselves (`WorldSceneTest`, `PlayerClassTest`, `StatsTest`) for the full implementation.

## Related Issues

- **#200** — initial property-based test suite (this feature)
- **#67** — world-gen properties (future follow-up, can reuse `VeilArbitraries.seed()` and the patterns established here)
