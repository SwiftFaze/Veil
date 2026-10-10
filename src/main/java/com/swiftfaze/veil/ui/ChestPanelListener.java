package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.GameListener;
import com.swiftfaze.veil.world.object.Chest;

/** Shows the chest panel when the player interacts with a chest. */
public class ChestPanelListener implements GameListener {
    private final ChestPanel chestPanel;

    public ChestPanelListener(ChestPanel chestPanel) {
        this.chestPanel = chestPanel;
    }

    @Override
    public void updatePlayer(Player player) {
        // No player-info display right now, same as PopupToggleListener.
    }

    @Override
    public void openChest(Chest chest) {
        chestPanel.show(chest);
    }
}
