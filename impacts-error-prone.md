# Impacts: Error Prone expanded beyond its default check set (#219)

Every violation the ADD checks in [`docs/error-prone-checks.md`](docs/error-prone-checks.md)
found in the existing code, measured at the spec commit (`bcefd40`) with every
candidate check enabled as a warning (`-XepAllDisabledChecksAsWarnings`,
`-Xmaxwarns 100000`), over `src/main` and `src/test`. Unlike the PMD record in
[`impacts.md`](impacts.md), **every entry here is fixed in this change**: an Error
Prone check at ERROR fails `javac`, with no diff-scoping to defer to. No
`@SuppressWarnings`, no demotion, no auto-patching.

Paths are relative to `src/main/java/com/swiftfaze/veil/` (M) or
`src/test/java/com/swiftfaze/veil/` (T).

## Not yet enabled

Measured but not fixed yet: `BooleanParameter` (38), `ConstantField` (13), `MethodCanBeStatic` (40), `MissingBraces` (33), `NonFinalStaticField` (13), `ParameterMissingNullable` (82), `ReturnMissingNullable` (18), `UnnecessarilyFullyQualified` (81), `WildcardImport` (26).

## Fixed

| Check | Count | Files | Fix applied | Player-visible change |
|-------|-------|-------|-------------|-----------------------|
| `CheckedExceptionNotThrown` | 1 | `T:mods/ModSchemaValidationIT.java` | Dropped the `throws IOException` that `validateFile` never threw, and the dead catch around its call. | No |
| `FieldMissingNullable` | 1 | `T:steps/DevConsoleSteps.java` | Annotated the test field that is reset to `null` with `@Nullable`. | No |
| `InconsistentOverloads` | 1 | `M:ui/widget/TableWidget.java` | `TableWidget(columnRenderers, columnHeaders)`: the shared `columnRenderers` parameter now comes first in both constructors; two callers updated. | No |
| `MixedMutabilityReturnType` | 1 | `T:testing/quality/CrapBaseline.java` | `CrapBaseline.load` now returns an unmodifiable map on both paths. | No |
| `OperatorPrecedence` | 1 | `T:steps/DevConsoleSteps.java` | Parenthesised a mixed `||`/`&&` condition (no change in evaluation). | No |
| `ReturnsNullCollection` | 1 | `T:steps/DevConsoleSteps.java` | Test helper `getRow` returns an empty list instead of `null` past the last row. | No |
| `StreamResourceLeak` | 1 | `T:steps/ApprovalTestsGlyphGridSteps.java` | Closed the `Files.walk` stream with try-with-resources. | No |
| `UnescapedEntity` | 1 | `M:sandbox/PlayerSandboxProvider.java` | Wrapped `Supplier<Player>` in `{@code}` in Javadoc. | No |
| `UnnecessaryLambda` | 1 | `T:steps/AppIconAndVersionSteps.java` | Replaced a `Supplier` constant holding a lambda with a private method and a method reference. | No |
| `UnusedException` | 1 | `M:mods/CalcExpressionParser.java` | Kept the `NumberFormatException` as the cause of the `IllegalArgumentException` it is rethrown as. | No (better error detail only) |
| `DeduplicateConstants` | 2 | `T:steps/DevConsoleSteps.java` | Used the existing `CLASSES_PROVIDER_NAME` constant instead of repeating `"Classes"`. | No |
| `DefaultLocale` | 2 | `M:sandbox/ClassSandboxPanel.java`, `T:steps/ClassSandboxSteps.java` | `String.format(Locale.ROOT, ...)` for the class-sandbox stats label and its test. | No for these digit-only formats; previously a locale with non-ASCII digits could have changed them |
| `PreferJavaTimeOverload` | 2 | `T:game/GamePanelRealKeyEventTest.java`, `T:testing/qa/QaRunner.java` | `TimeUnit.MILLISECONDS.sleep(...)` instead of `Thread.sleep(long)`. `Thread.sleep(Duration)` would be the check's own suggestion, but it is Java 19+ and the project targets 17. | No |
| `MissingOverride` | 3 | `M:entities/items/Item.java`, `M:world/Tile.java` | Added `@Override` to `Item.getId/getName` and `Tile.getId` (they implement `Inspectable`). | No |
| `ThrowSpecificExceptions` | 3 | `M:testing/approval/ApprovalCheck.java`, `T:steps/ApprovalTestsGlyphGridSteps.java` | `UncheckedIOException` instead of `RuntimeException` when wrapping an `IOException`. | No |
| `UnusedVariable` | 3 | `T:steps/ClassStatsSandboxSteps.java`, `T:steps/UiComponentFrameworkSteps.java` | Removed two unread test fields and an unused helper parameter. | No |
| `AvoidObjectArrays` | 4 | `M:entities/buildings/Building.java`, `M:world/WorldScene.java`, `T:testing/aps/AcceptanceMutator.java` | `Building` blueprint is now `List<List<Tile>>` (copied on construction) instead of `Tile[][]`; `WorldScene.renderToGrid` returns one `String` per row instead of `char[][]`; `AcceptanceMutator.execute` takes `List<String>`. Callers and tests updated. | No - same tiles placed, same glyphs rendered (approval fixtures unchanged) |
| `FieldCanBeFinal` | 4 | `M:ui/SettingsScreenPanel.java`, `T:steps/UiComponentFrameworkSteps.java` | Made never-reassigned fields `final`. | No |
| `RedundantNullCheck` | 4 | `M:entities/quests/Quest.java` | `Quest.Objective.target` and `Quest.Reward.id/count/calc` are genuinely optional in mod JSON (`ModLoader` passes `null`), so they are now `@Nullable` - the checks were right and the declared types were wrong. | No |
| `StringSplitter` | 4 | `T:steps/CrapGateSteps.java`, `T:steps/ModLoaderSteps.java`, `T:testing/quality/CrapBaseline.java`, `T:testing/uml/UmlIr.java` | `split(regex, -1)`: identical result for these first-segment / trimmed-line uses, without `split`'s silent trailing-empty-string removal. | No |
| `UngroupedOverloads` | 6 | `M:ui/DetailsPaneWidget.java`, `M:ui/ListDetailLayoutUtility.java`, `T:testing/aps/MutationReport.java` | Moved overloads next to each other (`DetailsPaneWidget` constructors, `ListDetailLayoutUtility.buildDetailsPanel`, `MutationReport.skipped`). | No |
| `UnnecessaryParentheses` | 6 | `T:steps/ModLoaderSteps.java` | Removed redundant parentheses around `i++`. | No |
| `TooManyParameters` | 7 | `T:steps/DevConsoleSteps.java`, `T:steps/ModLoaderSteps.java`, `T:steps/UiComponentFrameworkSteps.java` | Cucumber steps with 6-11 captures now take fixture objects built by `@ParameterType`s (`{tileLook}`, `{baseStats}`, `{itemLook}`, `{itemEffect}`, `{questObjective}`, `{itemAndXpRewards}`, `{quotedList}`); the .feature wording is unchanged. | No (test glue only) |
| `PrivateConstructorForUtilityClass` | 8 | `M:Main.java`, `M:sandbox/ClassSandbox.java`, `M:testing/approval/ApprovalCheck.java`, `M:testing/approval/ApprovalReapprove.java`, `M:ui/GameWindow.java`, `M:ui/SettingsKeybindsWindow.java`, +2 more | Added a private constructor (and `final`, per PMD `ClassWithOnlyPrivateConstructorsShouldBeFinal`) to the static-only classes. | No |
| `RemoveUnusedImports` | 8 | `M:sandbox/PlayerDetailPanel.java`, `M:ui/ListDetailLayoutUtility.java`, `T:ModuleDependencyTest.java`, `T:steps/ClassStatsSandboxSteps.java`, `T:steps/ModLoaderSteps.java`, `T:steps/UiComponentFrameworkSteps.java`, +2 more | Removed unused imports. | No |
| `IdentifierName` | 12 | `M:Main.java`, `M:ui/widget/TerminalScrollBarUI.java`, `T:mods/ModLoaderIT.java`, `T:mods/ModSchemaValidationIT.java`, `T:steps/ModLoaderSteps.java`, `T:steps/WorldSingleFloorRenderingSteps.java` | Renamed `TerminalScrollBarUI` -> `TerminalScrollBarUi`, `buildUIScreens` -> `buildUiScreens`, step methods with `ID`/`XY` -> `Id`/`Xy`, and the integration tests `ModLoaderIT`/`ModSchemaValidationIT` -> `...It`, with Failsafe's `<includes>` set to `**/*It.java` (its default is `*IT`) and `CLAUDE.md`/`docs/testing.md` updated to match. | No |
| `SystemOut` | 12 | `M:testing/approval/ApprovalReapprove.java`, `T:testing/aps/AcceptanceMutator.java`, `T:testing/qa/QaRunner.java`, `T:testing/quality/CrapReport.java`, `T:testing/uml/PitMutationMetrics.java`, `T:testing/uml/UmlExport.java` | `ApprovalReapprove` (main) logs its status lines through SLF4J. The test-scope CLIs (`AcceptanceMutator`, `MutationReport`, `QaRunner`, `UmlExport`, `PitMutationMetrics`, `CrapReport`) write report lines to a `Consumer<String>` instead of a `PrintStream`; their `main` passes `LOGGER::info` and tests collect into a list. No script parses these streams (`check-clean.sh` reads `target/crap/crap.txt`). | No in-game change. Dev-tool console output now carries the logback prefix (time, level, thread, logger) |
| `MissingDefault` | 13 | `M:sandbox/PlayerDetailPanel.java`, `M:ui/SettingsKeybindsPanel.java`, `M:ui/SettingsScreenPanel.java`, `T:steps/DevConsoleSteps.java`, `T:steps/ModLoaderSteps.java`, `T:steps/UiComponentFrameworkSteps.java` | Main: explicit no-op `default` (with the reason) in the attribute-row, keybinds footer/navigation and settings-confirm switches - same behaviour. Test steps: `default` now throws `IllegalArgumentException` naming the unhandled key, so a typo in a step argument fails instead of silently doing nothing. | No |

Fixed so far: 113 violations across 28 checks.
