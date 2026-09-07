package com.swiftfaze.veil.sandbox;

import javax.swing.JComponent;
import java.util.List;
import java.util.Optional;

/**
 * A source of individually-searchable {@link DevConsoleEntry} items the dev
 * console's top-level results table lists directly (e.g. every player class,
 * not a single "Classes" row). A future provider (items, monsters, quests,
 * ...) only needs to implement this and be registered - no changes to the
 * search/results/keybinding shell in {@link DevConsolePanel}.
 */
public interface DevConsoleProvider {

    List<DevConsoleEntry> entries();

    /**
     * Opens the detail panel for one entry, pre-selected to it (e.g. jumping
     * straight to a specific class's stats, not a generic "browse everything"
     * view).
     *
     * @param id the {@link DevConsoleEntry#id()} - fully-qualified id (e.g. "core:mage")
     */
    JComponent createPanel(String id);

    /**
     * Opt-in hook for a provider whose entries support live field mutation via the command bar's
     * set/add/subtract verbs. Empty by default - only {@link PlayerSandboxProvider} overrides it
     * for v1.
     *
     * @param id the {@link DevConsoleEntry#id()} the mutation targets
     */
    default Optional<DevConsoleFieldMutator> fieldMutator(String id) {
        return Optional.empty();
    }
}
