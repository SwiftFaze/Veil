package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.GameListener;
import com.swiftfaze.veil.game.event.GameEvent;
import com.swiftfaze.veil.game.event.GameEventLog;

/**
 * Minimal stopgap wiring so the I/X toggles keep working with EastPanel gone —
 * no sidebar, no player-info display, just open/close/mutual-exclusion between
 * the inventory and codex popups, same behavior EastPanel used to provide.
 */
public class PopupToggleListener implements GameListener {
    private final InventoryPanel inventoryPanel;
    private final CodexPanel codexPanel;
    private final GameEventLog eventLog;

    public PopupToggleListener(InventoryPanel inventoryPanel, CodexPanel codexPanel) {
        this(inventoryPanel, codexPanel, new GameEventLog());
    }

    public PopupToggleListener(InventoryPanel inventoryPanel, CodexPanel codexPanel, GameEventLog eventLog) {
        this.inventoryPanel = inventoryPanel;
        this.codexPanel = codexPanel;
        this.eventLog = eventLog;
    }

    @Override
    public void updatePlayer(Player player) {
        // No player-info display right now — removed with the rest of the early UI shell.
    }

    @Override
    public void toggleInventory() {
        if (inventoryPanel.isVisible()) {
            inventoryPanel.dismiss();
            eventLog.append(new GameEvent.PopupToggled("inventory", false));
        } else {
            if (codexPanel.isVisible()) {
                codexPanel.dismiss();
            }
            inventoryPanel.open();
            eventLog.append(new GameEvent.PopupToggled("inventory", true));
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
