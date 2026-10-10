package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.GameListener;
import com.swiftfaze.veil.game.GamePanel;
import com.swiftfaze.veil.game.WorldPause;

/**
 * Wires ESC to open/dismiss the pause menu overlay. Opening holds the pause menu's id
 * in the shared {@link WorldPause}, and any dismiss of the popup releases it, mirroring
 * PopupToggleListener's role for Inventory/Codex.
 */
public class PauseToggleListener implements GameListener {
    private final GamePanel gamePanel;
    private final PauseMenuPopup pauseMenuPopup;

    public PauseToggleListener(GamePanel gamePanel, PauseMenuPopup pauseMenuPopup) {
        this.gamePanel = gamePanel;
        this.pauseMenuPopup = pauseMenuPopup;
        pauseMenuPopup.addDismissListener(() -> gamePanel.getWorldPause().release(WorldPause.PAUSE_MENU));
    }

    @Override
    public void updatePlayer(Player player) {
        // No player-info display right now, same as PopupToggleListener.
    }

    @Override
    public void togglePause() {
        if (pauseMenuPopup.isVisible()) {
            pauseMenuPopup.dismiss();
        } else {
            gamePanel.getWorldPause().hold(WorldPause.PAUSE_MENU);
            pauseMenuPopup.open();
        }
    }
}
