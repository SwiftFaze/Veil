package com.swiftfaze.veil.game;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.input.Keybindings;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GamePanelTest {
    private static final int PAINT_WIDTH = 300;
    private static final int PAINT_HEIGHT = 225;

    @Test
    void constructorInitializes() {
        GamePanel panel = new GamePanel();

        assertEquals(GameConst.DEFAULT_PLAYER_START_X, panel.getPlayer().getX());
        assertEquals(GameConst.DEFAULT_PLAYER_START_Y, panel.getPlayer().getY());
    }

    /**
     * Paints into an off-screen image, so it runs the same with or without a display.
     * Without it, paintComponent is only covered when some other test happens to show
     * a real window, and CI (headless) scores it at 0% coverage in the CRAP gate.
     */
    @Test
    void paintingRendersTheWorldIntoTheGraphics() {
        GamePanel panel = new GamePanel();
        panel.setSize(PAINT_WIDTH, PAINT_HEIGHT);
        BufferedImage image = new BufferedImage(PAINT_WIDTH, PAINT_HEIGHT, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = image.createGraphics();
        panel.paint(graphics);
        graphics.dispose();

        assertTrue(distinctColours(image) > 1, "painting should draw tiles and glyphs, not one flat fill");
    }

    private static long distinctColours(BufferedImage image) {
        return IntStream.range(0, image.getHeight())
                .flatMap(y -> IntStream.range(0, image.getWidth()).map(x -> image.getRGB(x, y)))
                .distinct()
                .count();
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

        assertEquals(GameConst.DEFAULT_PLAYER_START_X, panel.getPlayer().getX());
        assertEquals(GameConst.DEFAULT_PLAYER_START_Y, panel.getPlayer().getY());
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

    private static void fireAction(GamePanel panel, String actionName) {
        Action action = panel.getActionMap().get(actionName);
        action.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, actionName));
    }
}
