# Feature specs (Gherkin)

One `<feature-slug>.feature` file per **distinct concept**, generated from
the matching file in `/specs/intent/`. Do not hand-edit a `.feature` file
ahead of its intent doc — update the intent doc first, then regenerate.

**One feature file, one thing.** If an intent doc covers multiple
unrelated concepts (e.g. a new class *and* a new biome), don't bundle them
into one combined file like `added-class-and-biome.feature` — split into
`class-warrior.feature` and `biome-jungle.feature`, each named for the
specific thing it covers. A single intent doc can produce more than one
`.feature` file when it isn't actually a single cohesive concept.

These files are copied onto the test classpath at build time (see the
`testResources` config in `pom.xml`) and executed by `RunCucumberTest` via
`mvn test`. Step definitions live under
`src/test/java/com/swiftfaze/veil/steps/`. See `docs/testing.md` for
the full test-layer breakdown.

## Index

Concept-based naming (above) means related behavior is spread across
several files instead of one per Java class — this table is the browsable
map of what's covered where. **Whoever adds, removes, or renames a
`.feature` file updates this table in the same change** — it's part of
that change's definition of done, not separate cleanup.

| File | Covers |
|---|---|
| `default-player-class.feature` | New player defaults to the Warrior class |
| `data-driven-player-class.feature` | Data-driven PlayerClass loaded from `mods/core/classes/*.json` with per-level stat growth |
| `class-stats-sandbox.feature` | Dev-only `ClassSandbox` stat display |
| `sandbox-dev-console.feature` | Searchable dev-console provider framework the sandbox's menu/search/keybinding shell is generalized into, with `ClassSandboxPanel` as its first registered provider |
| `data-driven-tile.feature` | Tile definitions loaded from JSON + registry |
| `data-driven-item.feature` | Item definitions loaded from JSON + stat registry, minimal InventoryPanel wiring |
| `data-driven-quest.feature` | Quest definitions loaded from JSON + item registry, minimal per-player quest-state tracking |
| `mod-loader.feature` | External `mods/` directory loading (dependency order, overrides) |
| `building-loader-failure-path.feature` | Mod loader failure/error path |
| `world-single-floor-rendering.feature` | Single-floor world rendering |
| `world-scene-population-and-building-placement.feature` | World scene population and building placement |
| `camera-behavior.feature` | Camera follow/scroll behavior; viewport resizing (tracking a resizable Windowed frame's live panel size) and its 5x5-tile minimum-size floor, added for `fullscreen-windowed-toggle.feature` |
| `keyboard-input-and-menu-navigation.feature` | Key Bindings input and menu navigation |
| `installer-mods-bundling.feature` | jpackage installer build bundles `mods/core` alongside the executable |
| `ui-component-framework.feature` | Shared terminal-style widget framework (list/button/popup, focus manager, selected-state styling), proven by `ClassSandboxPanel` and in isolation; supersedes SelectableMenu-based scenarios previously in `keyboard-input-and-menu-navigation.feature`, `ui-panel-rendering-and-composition.feature`, `class-sandbox-panel-selection.feature`, and `data-driven-item.feature`. Its former in-game inventory-popup proof case was removed alongside `EastPanel` |
| `ui-widget-table.feature` | Keyboard-navigable, full-width, bordered table widget (row/column selection, wrap-around, row-level confirm, header row, non-selectable mode), proven in isolation; its former real consumers (the inventory popup's field/value and effects tables) were removed alongside `EastPanel` |
| `ui-widget-radio-group.feature` | Single-select radio group widget (vertical by default, optional horizontal), proven in isolation; its former real consumer (the inventory popup's "Drop item?" confirmation) was removed alongside `EastPanel` |
| `ui-widget-pattern-field.feature` | Regex-pattern-validated text field widget (valid/invalid state via a new WidgetTheme color) — no real consumer yet, proven in isolation |
| `startup-welcome-screen.feature` | Title screen shown on launch (VEIL title + Continue/New/Load/Settings/Exit menu), replacing direct-to-world startup |
| `settings-screen.feature` | Settings screen shell (brightness/fullscreen/font/theme/volume/keybinds/folders/about/reset), visual/input shape only |
| `settings-keybinds-page.feature` | Dedicated keybind-rebinding sub-page (action list, press-any-key popup, Apply/Cancel/Go back), display only |
| `ui-widget-slider.feature` | Bounded-value slider widget (Left/Right adjusts within [min, max], no wrap) — no real backing system, proven in isolation |
| `confirmation-popup-variant.feature` | Centered, content-sized PopupWidget presentation, proven by a Yes/No confirm dialog on the settings screen's Reset to Defaults |
| `widget-theming.feature` | Mod-driven `theme.json` loaded via `ModLoader` (id/collision/overrides), populating all 10 `WidgetTheme` colors from `mods/core/theme.json`; a second mod's theme loads without activating |
| `pmd-jacoco-quality-gates.feature` | `mvn verify` fails on PMD complexity/length/parameter/duplication violations or sub-85% repo-wide JaCoCo coverage, with pure-layout Swing classes excluded from both; build-pipeline concern, no Java code path (`@manual-verification`) |
| `deterministic-gauntlet-workflow.feature` | ArchUnit engine/widgets/screens module-dependency gate, PMD fix-loop and parallel fan-out guidance wired into Step 4, tunable-threshold rationale, default agile slice-by-slice implementation path (full intent->spec->approval reserved for high-risk work), and `specs/intent/` no longer a permanently indexed artifact; process/steering-doc concern, no Java code path except the ArchUnit check (`@manual-verification`) |
| `controls-hint-bar.feature` | Persistent bottom-of-window hint bar reflecting current key bindings, updating at row/sub-focus granularity across Title, Settings, Keybinds, Inventory, and Codex; contextual world-action hints out of scope (tracked in #133) |
| `pause-screen-esc.feature` | ESC opens a pause menu overlay (Resume / Settings / Exit to Main Menu) mirroring Inventory/Codex's popup pattern; freezes movement while paused |
| `settings-persistence.feature` | `settings.json` loaded at startup / written on change for the Settings and Keybinds screens; newly-wired Settings-screen Reset to Defaults; Keybinds page's Apply/Cancel/Go back/Escape/Reset to Defaults given real, distinct (and confirmation-gated) meaning for the first time |
| `fullscreen-windowed-toggle.feature` | Settings screen's Fullscreen/Windowed radio applies live to the real game window (toggle, Reset to Defaults, and launching with an already-persisted value); the F5 hot-reset dev feature is removed entirely; camera-viewport-follows-panel-size behavior is covered in `camera-behavior.feature` instead |
| `persist-windowed-window-size.feature` | `settings.json` round-trip of a new WindowWidth/WindowHeight pair on `SettingsConfig` (default sentinel 0, missing/corrupt-file fallback); actual capture-on-exit, restore-on-launch, and screen-bounds clamping against a real JFrame are non-goals, verified by manual playtest instead |
