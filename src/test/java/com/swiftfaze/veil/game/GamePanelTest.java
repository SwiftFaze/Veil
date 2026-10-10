package com.swiftfaze.veil.game;

import com.swiftfaze.veil.GameConst;
import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.TileTestScene2;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GamePanelTest {
    private static final int PAINT_WIDTH = 300;
    private static final int PAINT_HEIGHT = 225;
    private static final Tile FLOOR = new Tile("test:floor", '.', Color.GRAY, true);
    private static final Tile WALL = new Tile("test:wall", '#', Color.WHITE, false);

    @Test
    void constructorInitializes() {
        GamePanel panel = new GamePanel();

        assertPlayerAtDefaultStart(panel::getPlayer);
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
    void heldWorldPausePreventsMovement() {
        GamePanel panel = new GamePanel();
        Player player = panel.getPlayer();
        int originalX = player.getX();
        int originalY = player.getY();

        panel.getWorldPause().hold("inventory");
        fireAction(panel, Keybindings.ACTION_MOVE_UP);

        assertEquals(originalX, player.getX());
        assertEquals(originalY, player.getY());
    }

    @Test
    void successfulMoveAdvancesTheTurnByOne() {
        GamePanel panel = panelWithTileToTheRight(FLOOR);
        CountingTickable counter = new CountingTickable();
        panel.getTurnScheduler().register(counter);

        fireAction(panel, Keybindings.ACTION_MOVE_RIGHT);

        assertEquals(1, panel.getTurnScheduler().getTurn());
        assertEquals(1, counter.steps);
    }

    @Test
    void blockedMoveDoesNotAdvanceTheTurn() {
        GamePanel panel = panelWithTileToTheRight(WALL);
        CountingTickable counter = new CountingTickable();
        panel.getTurnScheduler().register(counter);

        fireAction(panel, Keybindings.ACTION_MOVE_RIGHT);

        assertEquals(0, panel.getTurnScheduler().getTurn());
        assertEquals(0, counter.steps);
    }

    @Test
    void movementWhilePausedDoesNotAdvanceTheTurn() {
        GamePanel panel = panelWithTileToTheRight(FLOOR);
        panel.getWorldPause().hold("pause");

        fireAction(panel, Keybindings.ACTION_MOVE_RIGHT);

        assertEquals(5, panel.getPlayer().getX());
        assertEquals(0, panel.getTurnScheduler().getTurn());
    }

    @Test
    void repositioningThePlayerDirectlyDoesNotAdvanceTheTurn() {
        GamePanel panel = panelWithTileToTheRight(FLOOR);

        panel.getPlayer().setPosition(8, 8);

        assertEquals(0, panel.getTurnScheduler().getTurn());
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
        panel.getWorldPause().hold("pause");

        panel.resetState();

        assertPlayerAtDefaultStart(panel::getPlayer);
        assertFalse(panel.isPaused());
    }

    @Test
    void resetStateClearsTheTurnSchedulerAndItsRegistrations() {
        GamePanel panel = panelWithTileToTheRight(FLOOR);
        panel.getTurnScheduler().register(new CountingTickable());
        fireAction(panel, Keybindings.ACTION_MOVE_RIGHT);

        panel.resetState();

        assertEquals(0, panel.getTurnScheduler().getTurn());
        assertEquals(0, panel.getTurnScheduler().registeredCount());
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

    /** A panel with the player at (5, 5) on floor, and {@code tileToTheRight} at (6, 5). */
    private static GamePanel panelWithTileToTheRight(Tile tileToTheRight) {
        TileTestScene2 scene = new TileTestScene2(GameConst.DEFAULT_MAP_WIDTH, GameConst.DEFAULT_MAP_HEIGHT);
        scene.fillRegion(new Rectangle(0, 0, GameConst.DEFAULT_MAP_WIDTH, GameConst.DEFAULT_MAP_HEIGHT), FLOOR);
        scene.fillRegion(new Rectangle(6, 5, 1, 1), tileToTheRight);
        return new GamePanel(GameEventLog.noOp(), new Player(5, 5), scene);
    }

    private static final class CountingTickable implements Tickable {
        int steps;

        @Override
        public void onTurn(int turn) {
            steps++;
        }
    }

    private static void assertPlayerAtDefaultStart(Supplier<Player> playerSource) {
        Player player = playerSource.get();
        assertEquals(GameConst.DEFAULT_PLAYER_START_X, player.getX());
        assertEquals(GameConst.DEFAULT_PLAYER_START_Y, player.getY());
    }

    private static void fireAction(GamePanel panel, String actionName) {
        Action action = panel.getActionMap().get(actionName);
        action.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, actionName));
    }
}
