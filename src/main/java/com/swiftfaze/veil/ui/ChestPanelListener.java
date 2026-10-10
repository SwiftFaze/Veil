package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.GameListener;
import com.swiftfaze.veil.world.object.Chest;

import java.util.function.Consumer;

/** Shows the chest panel when the player interacts with a chest. */
public class ChestPanelListener implements GameListener {
    private final Consumer<Chest> onOpen;

    public ChestPanelListener(Consumer<Chest> onOpen) {
        this.onOpen = onOpen;
    }

    @Override
    public void updatePlayer(Player player) {
        // No player-info display right now, same as PopupToggleListener.
    }

    @Override
    public void openChest(Chest chest) {
        onOpen.accept(chest);
    }
}
