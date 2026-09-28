package com.swiftfaze.veil.game.event;

/**
 * Domain events in the game simulation. Sealed interface allowing engine/ui code
 * to record what happened without tight coupling to any specific logging mechanism.
 */
public sealed interface GameEvent {
    /**
     * A main card switch (title, settings, keybinds, game).
     */
    record ScreenChanged(String from, String to) implements GameEvent {
    }

    /**
     * The title-screen menu's selection moved.
     */
    record MenuSelectionChanged(String to) implements GameEvent {
    }

    /**
     * The player's position actually changed.
     */
    record PlayerMoved(int x, int y) implements GameEvent {
    }

    /**
     * An inventory or other popup opened or closed.
     */
    record PopupToggled(String name, boolean open) implements GameEvent {
    }
}
