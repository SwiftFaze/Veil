# Architecture: engine

The engine core: window assembly, the `GamePanel` render path and camera,
player movement, rendering contracts, keyboard input, the QA event log, and
tunable constants. Overview and layering: [`architecture.md`](architecture.md).

**Entry point / window assembly** (`Main.java`): builds a `JFrame` wrapping
`GamePanel` (via `ui/GameWindow.buildContentArea`) plus `InventoryPanel`/
`CodexPanel` layered above it as popups, in a `BorderLayout` — the former
`NorthPanel`/`SouthPanel`/`EastPanel` shell around them was removed as
early scaffolding pending a proper reimplementation, replaced by minimal
inline wiring in `Main.buildGameCard`/`wirePopups` (see `docs/screens.md`'s
"UI shell" note). There is no game loop/ticker — the world only repaints
in response to key events (see `GamePanel.bindKeys`).

`Main` also sets the window icon (`AppIcon`, from `/icons/veil.png`) on the
game frame and the dev console frame; if the resource is missing the
default icon stays and a warning is logged.

**`GamePanel`** is the core of the simulation: it owns the `Player`, the
active `WorldScene`, and a `Camera`, wires keyboard input directly to player
movement, and drives all rendering from `paintComponent`. The world is a
single flat layer — `paintComponent` centers the camera on the player and
makes one `scene.renderWorld(...)` call; there is no floor/depth dimension,
brightness falloff, or fog overlay. `Camera` (`render/Camera.java`) is a plain
offset holder — `centerOn(x, y)` sets its top-left offset to the target
position minus half the viewport, with no smoothing between calls and no
clamping to the map's bounds, so the viewport can extend past the map edge
when the player is near one. The viewport is resizable after construction
via `resizeViewport(width, height)`, tracking `GamePanel`'s live pixel size
each paint so a resizable Windowed frame reveals more or less of the map
around the player as it's resized; resizing below a 5-tile-per-dimension
minimum clamps to that floor rather than shrinking to zero or negative tiles.

**Player movement** (`entities/player/Player.java`): each directional move
checks whether the target tile is walkable and, if so, moves onto it —
there is no floor to step up onto or fall through, so a blocked move simply
does nothing.

**Rendering contracts** (package `com.swiftfaze.veil.render`, which depends on
nothing else in the project so any package can use it without closing a
cycle through `Main`'s root package): `Positionable` (x/y) → `DrawableAsciiEntity` (adds
glyph/color/`render(Graphics2D, int tileWidth, int tileHeight, Camera)`) is
what `GamePanel` iterates over in `entitiesToDraw` to draw non-scene entities
(currently just `Player`); `WorldScene` itself also implements
`DrawableAsciiEntity` but is rendered specially (via `renderWorld`), not
through the generic entity loop. `render` takes the existing `Camera` object
rather than separate camera-X/Y ints — issue #173 changed this signature (the
only case so far of a parameter-count fix reshaping a project-owned public
interface instead of decomposing a free-standing method or suppressing a
JDK-interface override, see `docs/testing-quality-gates.md`'s "Code quality
gates"). The same
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

**Game event log** (`game/event/`): sealed `GameEvent` records (`ScreenChanged`,
`MenuSelectionChanged`, `PlayerMoved`, `PopupToggled`) recorded into a
`GameEventLog` for QA replays. It lives in the engine so the UI may depend on
it. `Main` builds one and injects it into `GamePanel`, `TitleScreenPanel`,
`PopupToggleListener` and `ui/ScreenNavigator`, which owns every main-card
switch: one `ScreenChanged` per real change, plus focus and hint refresh, so
`Main` never calls `CardLayout.show`. A no-op by default; `mvn compile
exec:java -Dveil.qaLog=<path>` writes each event as one JSON line, flushed on
append, so a killed run keeps it. A write failure is logged, never thrown.

**`GameConst`** centralizes tunable gameplay constants (window/tile
dimensions, map size, player start position) — check here first before
hardcoding a magic number elsewhere. Keyboard bindings live separately in
`input/Keybindings.java`, since key mapping is a distinct, separately-
growing concern.
