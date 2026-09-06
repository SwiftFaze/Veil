package com.swiftfaze.veil.game;

import com.swiftfaze.veil.entities.player.Player;

public interface GameListener {
    void updatePlayer(Player player);

    default void toggleInventory() {
    }

    default void toggleCodex() {
    }

    default void toggleDevConsole() {
    }

    default void togglePause() {
    }
}
