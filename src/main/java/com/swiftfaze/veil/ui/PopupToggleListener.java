package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.GameListener;

/**
 * Minimal stopgap wiring so the I/X toggles keep working with EastPanel gone —
 * no sidebar, no player-info display, just open/close/mutual-exclusion between
 * the inventory and codex popups, same behavior EastPanel used to provide.
 */
public class PopupToggleListener implements GameListener {
    private final InventoryPanel inventoryPanel;
    private final CodexPanel codexPanel;

    public PopupToggleListener(InventoryPanel inventoryPanel, CodexPanel codexPanel) {
        this.inventoryPanel = inventoryPanel;
        this.codexPanel = codexPanel;
    }

    @Override
    public void updatePlayer(Player player) {
        // No player-info display right now — removed with the rest of the early UI shell.
    }

    @Override
    public void toggleInventory() {
        if (inventoryPanel.isVisible()) {
            inventoryPanel.dismiss();
        } else {
            if (codexPanel.isVisible()) {
                codexPanel.dismiss();
            }
            inventoryPanel.open();
        }
    }

    @Override
    public void toggleCodex() {
        if (codexPanel.isVisible()) {
            codexPanel.dismiss();
        } else {
            if (inventoryPanel.isVisible()) {
                inventoryPanel.dismiss();
            }
            codexPanel.open();
        }
    }
}
