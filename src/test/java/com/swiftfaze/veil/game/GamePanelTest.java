package com.swiftfaze.veil.game;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.input.Keybindings;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import java.awt.event.ActionEvent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GamePanelTest {

    @Test
    void constructorInitializes() {
        GamePanel panel = new GamePanel();

        assertEquals(com.swiftfaze.veil.GameConst.DEFAULT_PLAYER_START_X, panel.getPlayer().getX());
        assertEquals(com.swiftfaze.veil.GameConst.DEFAULT_PLAYER_START_Y, panel.getPlayer().getY());
    }

    @Test
    void toggleInventoryActionInvokesListener() {
        GamePanel panel = new GamePanel();
        boolean[] listenerCalled = {false};
        GameListener listener = new GameListener() {
            @Override
            public void updatePlayer(Player player) {
                // Not needed for this test
            }

            @Override
            public void toggleInventory() {
                listenerCalled[0] = true;
            }
        };
        panel.addGameListener(listener);

        fireAction(panel, Keybindings.ACTION_TOGGLE_INVENTORY);

        assertTrue(listenerCalled[0]);
    }

    @Test
    void startGameLoopWorks() {
        GamePanel panel = new GamePanel();

        assertDoesNotThrow(panel::startGameLoop);
    }

    @Test
    void pausedFlagPreventMovement() {
        GamePanel panel = new GamePanel();
        Player player = panel.getPlayer();
        int originalX = player.getX();
        int originalY = player.getY();

        panel.setPaused(true);
        fireAction(panel, Keybindings.ACTION_MOVE_UP);

        assertEquals(originalX, player.getX());
        assertEquals(originalY, player.getY());
    }

    @Test
    void togglePauseActionInvokesListener() {
        GamePanel panel = new GamePanel();
        boolean[] listenerCalled = {false};
        GameListener listener = new GameListener() {
            @Override
            public void updatePlayer(Player player) {
                // Not needed for this test
            }

            @Override
            public void togglePause() {
                listenerCalled[0] = true;
            }
        };
        panel.addGameListener(listener);

        fireAction(panel, Keybindings.ACTION_TOGGLE_PAUSE);

        assertTrue(listenerCalled[0]);
    }

    @Test
    void resetStateRestoresDefaultPlayerPositionAndUnpauses() {
        GamePanel panel = new GamePanel();
        panel.getPlayer().setPosition(1, 1);
        panel.setPaused(true);

        panel.resetState();

        assertEquals(com.swiftfaze.veil.GameConst.DEFAULT_PLAYER_START_X, panel.getPlayer().getX());
        assertEquals(com.swiftfaze.veil.GameConst.DEFAULT_PLAYER_START_Y, panel.getPlayer().getY());
        assertFalse(panel.isPaused());
    }

    @Test
    void movementStillWorksAfterResetState() {
        GamePanel panel = new GamePanel();
        panel.resetState();
        int startY = panel.getPlayer().getY();

        fireAction(panel, Keybindings.ACTION_MOVE_UP);

        assertEquals(startY - 1, panel.getPlayer().getY());
    }

    @Test
    void toggleDevConsoleActionInvokesListener() {
        GamePanel panel = new GamePanel();
        boolean[] listenerCalled = {false};
        GameListener listener = new GameListener() {
            @Override
            public void updatePlayer(Player player) {
                // Not needed for this test
            }

            @Override
            public void toggleDevConsole() {
                listenerCalled[0] = true;
            }
        };
        panel.addGameListener(listener);

        fireAction(panel, Keybindings.ACTION_TOGGLE_DEV_CONSOLE);

        assertTrue(listenerCalled[0]);
    }

    private void fireAction(GamePanel panel, String actionName) {
        Action action = panel.getActionMap().get(actionName);
        action.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, actionName));
    }
}
