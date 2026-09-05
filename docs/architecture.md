# Architecture

Veil is a 2D ASCII-tile desktop RPG built with Java 17 Swing (no game
engine). Rendering draws Unicode/ASCII glyphs with `Graphics2D.drawString`
onto a `JPanel`.

This file covers the game engine and data model. UI is documented
separately: `docs/ui-widgets.md` covers the reusable Swing widget
framework and theming, `docs/screens.md` covers how those widgets
compose into the game's actual screens, and `docs/components.md` covers
the self-describing list/detail data contract screens use to feed those
widgets. See `docs/README.md` for the full doc index.

This engine/widgets/screens layering is mechanically enforced, not just
documented: `ModuleDependencyTest` (ArchUnit) fails the build if engine
code depends on the UI layer, or if a widget depends on a screen. See
`docs/testing.md`'s "Module dependency gate" section for the exact rule
and why `Main` and `sandbox` are excluded.

**Entry point / window assembly** (`Main.java`): builds a `JFrame` wrapping
`GamePanel` (via `ui/GameWindow.buildContentArea`) plus `InventoryPanel`/
`CodexPanel` layered above it as popups, in a `BorderLayout` — the former
`NorthPanel`/`SouthPanel`/`EastPanel` shell around them was removed as
early scaffolding pending a proper reimplementation, replaced by minimal
inline wiring in `Main.buildGameCard`/`wirePopups` (see `docs/screens.md`'s
"UI shell" note). There is no game loop/ticker — the world only repaints
in response to key events (see `GamePanel.bindKeys`).

**`GamePanel`** is the core of the simulation: it owns the `Player`, the
active `WorldScene`, and a `Camera`, wires keyboard input directly to player
movement, and drives all rendering from `paintComponent`. The world is a
single flat layer — `paintComponent` centers the camera on the player and
makes one `scene.renderWorld(...)` call; there is no floor/depth dimension,
brightness falloff, or fog overlay. `Camera` (`Camera.java`) is a plain
offset holder — `centerOn(x, y)` sets its top-left offset to the target
position minus half the viewport, with no smoothing between calls and no
clamping to the map's bounds, so the viewport can extend past the map edge
when the player is near one. The viewport is resizable after construction
via `resizeViewport(width, height)`, tracking `GamePanel`'s live pixel size
each paint so a resizable Windowed frame reveals more or less of the map
around the player as it's resized; resizing below a 5-tile-per-dimension
minimum clamps to that floor rather than shrinking to zero or negative tiles.

**World representation** (`world/WorldScene.java`): an abstract base holding
a `Tile[width][height]` grid. The concrete scene (`TileTestScene2`)
subclasses it and populates tiles in its constructor; `GamePanel` hardcodes
`TileTestScene2` as the active scene. `Tile` (`world/Tile.java`) is a plain
glyph/color/walkability data class — no longer an enum — with instances
loaded from JSON (`mods/core/tiles/*.json`) via `ModLoader` into a
`ModRegistry`, keyed by namespaced ID (`core:grass`, etc.). `CoreTiles`
(`world/CoreTiles.java`) exposes those IDs as String constants for
production call sites (`registry.getTile(CoreTiles.GRASS)`) so they keep
some compile-time safety without `Tile` itself being an enum. Walkability
and rendering are entirely data-driven off these tile definitions, there's
no separate collision or sprite system. This is phase 2 of the
data-driven-mod-content initiative.

**Buildings** are authored as JSON blueprints under `mods/core/buildings/`
(a flat 2D array of namespaced tile IDs — `{"id", "name", "type", "width",
"height", "tiles": [...]}`) and loaded by `ModLoader.load(modsRoot)` into a
`ModRegistry`, from which a `Building` (a `Tile[][]` blueprint plus a world
X/Y offset) is looked up by its namespaced `"id"` (e.g. `core:small_house_01`)
and stamped into a scene via `WorldScene.placeBuilding`. `ModLoader` reads
content from an external `mods/` directory (resolved relative to the JVM's
working directory) rather than the classpath — `core` is itself just a mod
living at `mods/core/`, loaded through the same path any third-party mod
would use. `ModLoader` makes two full passes over mods in dependency order:
first to load all tiles from `tiles/*.json` into a registry, then to load
all buildings from `buildings/*.json` with tile references resolved against
the tile registry. This is phase 2 of a larger data-driven-mod-content
initiative.

**Player movement** (`entities/player/Player.java`): each directional move
checks whether the target tile is walkable and, if so, moves onto it —
there is no floor to step up onto or fall through, so a blocked move simply
does nothing.

**Player RPG data** (`entities/player/`): `PlayerInfo` composes `Level`,
`Stats`, and a `PlayerClass`. `PlayerClass` is a plain data holder (name +
base stat values + optional per-level growth curves) loaded from JSON via
the mod system (`mods/core/classes/warrior.json`, `mage.json`, etc.), using
the same `ModLoader` and `ModRegistry` as tiles and buildings — see "World
representation" above. `CoreClasses` exposes class IDs as String constants
(`core:warrior`, `core:mage`) for production call sites. Class growth is
defined via `calc` expressions (plain arithmetic: `+ - * /`, parentheses,
the `level` variable, and numeric literals — no embedded scripting), validated
at load time against a stat registry (`mods/core/stats.json`), and exposed
via `PlayerClass.applyStatsAtLevel(stats, level)` to compute stat values at
any level (currently level 0 only in gameplay, since no level-up trigger
exists yet). `PlayerInfo` defaults new players to `core:warrior`. Only the
eight base-stat *values* and growth curves are data-driven — `Stats`' fields
and its derived `getAttackPower`/`getDefense` formulas stay plain Java. The
eight registered stats are `strength`, `dexterity`, `constitution`,
`intelligence`, `wisdom`, `luck`, `maxHp`, `maxMana`. This data isn't wired
into gameplay yet — it used to also feed a `PlayerInfoPanel` display,
removed along with the rest of the early UI shell (see `docs/screens.md`'s
"UI shell" note).

**Items** (`entities/items/Item.java`): a plain data holder (name, glyph,
type, slot, base damage min/max, and an `effects` list of `{type, stat,
calc}` entries) loaded from JSON under `mods/core/items/*.json` via the
same `ModLoader`/`ModRegistry` mechanism as tiles/buildings/classes. Only
`stat_bonus` is a supported effect type so far. `effects[].stat` is
validated at load time against the same stat registry
(`mods/core/stats.json`) classes use, and `effects[].calc` is parsed with
`CalcExpressionParser` for syntactic validity — but, unlike `PlayerClass`,
nothing evaluates an item's `calc` to a number yet, since no equip/
inventory-management system exists to consume it. `InventoryPanel` takes
`ModRegistry.getAllItems()`'s result via `showItems(List<Item>)` — that
wiring now happens in `Main.wirePopups` rather than `EastPanel`'s
constructor, removed along with the rest of the early UI shell (see
`docs/screens.md`'s "UI shell" note). This is phase 4 of the
data-driven-mod-content initiative.

**Quests** (`entities/quests/Quest.java`): a plain data holder (name, an
`objective` — `{type, target, count}`, fixed to `"kill"` this slice — and
a `rewards` list of `{type, id?, count?, calc?}` entries, `item` or `xp`)
loaded from JSON under `mods/core/quests/*.json` via the same
`ModLoader`/`ModRegistry` mechanism as tiles/buildings/classes/items.
`ModLoader` loads quests after items within the same mod-load pass, since
`rewards[].type: "item"` entries validate their `id` against the item
registry already populated earlier in that pass; an unresolved item ID
fails loading immediately, matching the tile/class/item unregistered-
reference pattern. `rewards[].type: "xp"` entries parse their `calc` with
`CalcExpressionParser` for syntactic validity only, same as items —
nothing evaluates it to a number yet, and no combat/monster system exists
to detect a `kill` objective being satisfied either. Minimal per-player
quest state (`entities/player/QuestLog.java`, an unvalidated
`Map<String, QuestLog.State>` of not-started/offered/active/complete,
defaulting unseen quest IDs to not-started) is composed onto `PlayerInfo`
alongside `stats`/`playerClass`, with no order validation on transitions
and no persistence across restarts — no game-save/progress system exists in
the project yet (distinct from the settings-only persistence in
`com.swiftfaze.veil.config`, see `docs/screens.md`'s "Settings persistence" —
per-installation config, not per-playthrough save data, different lifecycle).
This is phase 5 of the data-driven-mod-content initiative.

**Rendering contracts**: `Positionable` (x/y) → `DrawableAsciiEntity` (adds
glyph/color/`render`) is what `GamePanel` iterates over in `entitiesToDraw`
to draw non-scene entities (currently just `Player`); `WorldScene` itself
also implements `DrawableAsciiEntity` but is rendered specially (via
`renderWorld`), not through the generic entity loop. The same
self-describing principle applies to list/table/detail Swing UI (Codex,
Inventory, and future panels like a player stats screen) — see
`docs/components.md` for that contract.

**Keyboard input** (`input/Keybindings.java`, `GamePanel.bindKeys`): all
keyboard input goes through Swing Key Bindings (`InputMap`/`ActionMap`,
`WHEN_IN_FOCUSED_WINDOW`). `Keybindings` centralizes the `KeyStroke` and
action-name constants; `GamePanel` registers one `Action` per named binding
(movement, inventory toggle) instead of a raw `KeyListener` switch. Each
`Action` notifies `GameListener`s and repaints itself, so there's no
catch-all "notify after every keypress" path — an unbound key simply never
invokes an `Action`.

**Class/stats sandbox** (`sandbox/`): a dev-only stat inspector, not
referenced from `Main.java` and not the packaged/jpackage build's entry
point (`pom.xml`'s `main.class` stays `com.swiftfaze.veil.Main`). Run it
explicitly: `mvn compile exec:java -Dexec.mainClass=com.swiftfaze.veil.sandbox.ClassSandbox`.
`ClassSandboxModel` wraps `PlayerClassLoader.loadAll()` and exposes class
names plus computed `Stats` per class (via `PlayerClass.applyBaseStats`, no
duplicated formulas); `ClassSandboxPanel` (a plain `JPanel`, inlining the
black-background/monospaced-label styling `TerminalPanel` used to provide
before that shared base was removed — see `docs/screens.md`'s "UI shell"
note) reuses
`ui/widget/ListWidget` (wrap-around left on, the framework default) via
its own Key Bindings wiring — Up/Down moves the selection and immediately
refreshes the displayed attack power/defense/HP/mana, no separate confirm
step. Editing a class's JSON and re-launching the
sandbox picks up the change with no recompile, since `PlayerClassLoader`
reads the resource fresh on every `ClassSandboxModel` construction — there
is no static caching of loaded classes anywhere in this path.

**`GameConst`** centralizes tunable gameplay constants (window/tile
dimensions, map size, player start position) — check here first before
hardcoding a magic number elsewhere. Keyboard bindings live separately in
`input/Keybindings.java`, since key mapping is a distinct, separately-
growing concern.
