Feature: Edit item base damage from the dev console
  The "Items" provider (sandbox-items-provider.feature) gains a
  DevConsoleFieldMutator, so the command bar's set/add/subtract verbs
  (dev-console-set-add-commands.feature) can target an item entry by its
  fully-qualified id. Only base damage is editable, via the tokens
  `mindmg` and `maxdmg`; values floor at 0, a result with min above max is
  rejected, and `default` restores the value loaded from mod JSON. Edits
  change the provider's own sandbox copy of the item - a new immutable Item
  swapped into its id-keyed map - so the next time the item is opened its
  detail panel shows the edited value. Edits last for the provider's
  lifetime and are never written back to mod JSON.

  Covers: set/add/subtract/default on an item's base damage, their
  transcript lines, and the edited value in the item detail panel.
  Supersedes: the "editing item fields" exclusion in
  sandbox-items-provider.feature.
  Out of scope: editing glyph, type, slot or effects; writing edits back to
  mod JSON; propagating edits to the running game's ModRegistry
  (inventory/codex); spawning items; resetting edits on `reload`.

  QA: none - no keyboard input of its own; reached through the existing dev-console command bar

  Background:
    Given the dev console is running with the "Items" provider registered

  Scenario Outline: set overwrites an item's base damage and the detail panel shows it
    When the command bar is set to "set core:iron_sword <token> <value>"
    Then the transcript's last line is a SUCCESS line for "<field>" set to <value>
    And when "Iron Sword" is opened the item detail shows "<field>" as "<value>"

    Examples:
      | token  | field             | value |
      | maxdmg | Base Damage (Max) | 12    |
      | mindmg | Base Damage (Min) | 0     |

  Scenario: add raises an item's base damage by a magnitude
    When the command bar is set to "add core:iron_sword maxdmg 3"
    Then the transcript's last line is a SUCCESS line for "Base Damage (Max)" set to 12
    And when "Iron Sword" is opened the item detail shows "Base Damage (Max)" as "12"

  Scenario: subtract lowers an item's base damage by a magnitude
    When the command bar is set to "subtract core:iron_sword mindmg 1"
    Then the transcript's last line is a SUCCESS line for "Base Damage (Min)" set to 3
    And when "Iron Sword" is opened the item detail shows "Base Damage (Min)" as "3"

  Scenario: Edits accumulate across commands
    When the command bar is set to "set core:iron_sword maxdmg 20"
    And the command bar is set to "add core:iron_sword maxdmg 5"
    Then the transcript's last line is a SUCCESS line for "Base Damage (Max)" set to 25

  Scenario: Subtracting below zero clamps to zero
    When the command bar is set to "subtract core:iron_sword mindmg 50"
    Then the transcript's last line is a SUCCESS line for "Base Damage (Min)" set to 0

  Scenario: default restores the mod-loaded value
    Given the command bar is set to "set core:iron_sword maxdmg 30"
    When the command bar is set to "set core:iron_sword maxdmg default"
    Then the transcript's last line is a SUCCESS line for "Base Damage (Max)" set to 9
    And when "Iron Sword" is opened the item detail shows "Base Damage (Max)" as "9"

  Scenario: An item without base damage can be given some
    When the command bar is set to "set core:bread_loaf maxdmg 2"
    Then the transcript's last line is a SUCCESS line for "Base Damage (Max)" set to 2
    And when "Bread Loaf" is opened the item detail shows "Base Damage (Max)" as "2"

  Scenario Outline: An edit that leaves min above max is rejected and changes nothing
    When the command bar is set to "<command>"
    Then the transcript's last line is an ERROR line for value "<value>"
    And when "Iron Sword" is opened the item detail shows "Base Damage (Min)" as "4"
    And when "Iron Sword" is opened the item detail shows "Base Damage (Max)" as "9"

    Examples:
      | command                            | value |
      | set core:iron_sword mindmg 10      | 10    |
      | set core:iron_sword maxdmg 3       | 3     |
      | subtract core:iron_sword maxdmg 6  | 6     |

  Scenario Outline: A non-damage item field is rejected as an unsupported field
    When the command bar is set to "set core:iron_sword <token> 1"
    Then the transcript's last line is an ERROR line for field "<token>"

    Examples:
      | token   |
      | glyph   |
      | type    |
      | slot    |
      | effects |

  Scenario: A non-numeric, non-"default" value is rejected
    When the command bar is set to "set core:iron_sword maxdmg lots"
    Then the transcript's last line is an ERROR line for value "lots"
    And when "Iron Sword" is opened the item detail shows "Base Damage (Max)" as "9"

  Scenario: A negative add magnitude is rejected
    When the command bar is set to "add core:iron_sword maxdmg -2"
    Then the transcript's last line is an ERROR line for value "-2"

  Scenario: Editing one item leaves the others untouched
    When the command bar is set to "set core:iron_sword maxdmg 12"
    Then when "Bread Loaf" is opened the item detail has no "Base Damage (Max)" row

  # Non-goals:
  #   - Glyph/type/slot/effects editing: DevConsoleMutationResult.Success
  #     carries an int, and effects would need new list-editing syntax.
  #   - Writing edits back to mods/ JSON.
  #   - Edits reaching the running game's inventory/codex - the provider
  #     loads its own ModLoader copy, separate from Main's ModRegistry.
  #   - Changes to DevConsolePanel or DevConsoleCommandRunner.
  #
  # Decisions (all auto-decided, see specs/intent/sandbox-edit-item-fields.md):
  #   - Item stays immutable; the provider swaps a new Item into its map.
  #   - Floor 0 with clamping, as for Player fields; min > max is an error
  #     naming the value token.
  #   - `default` is valid for both tokens (hasClassDefault -> true) and
  #     restores the mod-loaded value.
  #   - Tab completion offers mindmg/maxdmg for item entries via the
  #     existing fieldTokens() hook - no completion code changes.
  #
  # Risks:
  #   - Scenarios read real mods/core/items data (Iron Sword 4-9, Bread
  #     Loaf with no damage); changing those files breaks them.
  #   - Each scenario needs a fresh provider instance, or edits leak
  #     between scenarios.
  #
  # Open questions:
  #   - None.
