package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.GameListener;
import com.swiftfaze.veil.game.GamePanel;

/**
 * Wires ESC to open/dismiss the pause menu overlay and freezes GamePanel's
 * movement input while it's open, mirroring PopupToggleListener's role for
 * Inventory/Codex.
 */
public class PauseToggleListener implements GameListener {
    private final GamePanel gamePanel;
    private final PauseMenuPopup pauseMenuPopup;

    public PauseToggleListener(GamePanel gamePanel, PauseMenuPopup pauseMenuPopup) {
        this.gamePanel = gamePanel;
        this.pauseMenuPopup = pauseMenuPopup;
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
            gamePanel.setPaused(true);
            pauseMenuPopup.open();
        }
    }
}
