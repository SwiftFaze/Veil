# Docs index

- [`architecture.md`](architecture.md) — the game engine and data model: entry point/window assembly, the `GamePanel` render loop, the world/tile model, mod-loaded content (buildings, classes, items, quests), rendering contracts, keyboard input, and the class/stats sandbox.
- [`components.md`](components.md) — general rules for how a UI component receives its data and reports player actions (self-describing types, data-only contracts, opt-in adoption, internal-vs-cross-component state), with the Codex/Inventory details pane as a worked example.
- [`ui-widgets.md`](ui-widgets.md) — the reusable Swing widget framework in `ui/widget/` (`ListWidget`, `TableWidget`, `PopupWidget`, etc.) and the mod-driven theming system that colors it.
- [`ui-styling.md`](ui-styling.md) — concrete layout/spacing, typography, and color rules any panel or widget should follow (outer padding, component gap, h1/h2/p sizes, when a new theme color is actually justified).
- [`screens.md`](screens.md) — how `Main.java` assembles and navigates between screens, and how each screen panel (Title, Settings, Keybinds, Inventory, Codex) composes the widgets from `ui-widgets.md`.
- [`testing.md`](testing.md) — the three test layers (unit, acceptance, integration), where each lives, and how to run them.
- [`release.md`](release.md) — the two release channels (`master` stable, `develop` beta) and why versioning/changelog generation is fully automatic.
- [`ui-verification.md`](ui-verification.md) — how to visually verify a Swing UI change actually renders correctly, since this project's tests don't assert on pixel layout or rendered text.
- [`wiki.md`](wiki.md) — the split between `docs/` (for contributors) and the player-facing [GitHub wiki](https://github.com/SwiftFaze/Veil/wiki) (for game content/numbers).
