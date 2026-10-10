package com.swiftfaze.veil.game;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.game.worldgen.GeneratedWorldScene;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.testing.FlatFloorWorld;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.WorldScene;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
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
    private static final int START_X = 5;
    private static final int START_Y = 5;
    private static final int TILE_RIGHT_X = START_X + 1;
    private static final int ELSEWHERE = 8;
    private static final Tile FLOOR = new Tile("test:floor", '.', Color.GRAY, true);
    private static final Tile WALL = new Tile("test:wall", '#', Color.WHITE, false);

    @Test
    void constructorInitializes() {
        GamePanel panel = new GamePanel();

        assertPlayerOnSpawn(panel);
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
        GamePanel panel = FlatFloorWorld.panel();
        Player player = playerOf(panel);
        int originalX = player.getX();
        int originalY = player.getY();

        WorldPause worldPause = worldPauseOf(panel);
        worldPause.hold("inventory");
        fireAction(panel, Keybindings.ACTION_MOVE_UP);

        assertEquals(originalX, player.getX());
        assertEquals(originalY, player.getY());
    }

    @Test
    void successfulMoveAdvancesTheTurnByOne() {
        GamePanel panel = panelWithTileToTheRight(FLOOR);
        TurnScheduler scheduler = schedulerOf(panel);
        CountingTickable counter = new CountingTickable();
        scheduler.register(counter);

        fireAction(panel, Keybindings.ACTION_MOVE_RIGHT);

        assertEquals(1, scheduler.getTurn());
        assertEquals(1, counter.steps);
    }

    @Test
    void blockedMoveDoesNotAdvanceTheTurn() {
        GamePanel panel = panelWithTileToTheRight(WALL);
        TurnScheduler scheduler = schedulerOf(panel);
        CountingTickable counter = new CountingTickable();
        scheduler.register(counter);

        fireAction(panel, Keybindings.ACTION_MOVE_RIGHT);

        assertEquals(0, scheduler.getTurn());
        assertEquals(0, counter.steps);
    }

    @Test
    void movementWhilePausedDoesNotAdvanceTheTurn() {
        GamePanel panel = panelWithTileToTheRight(FLOOR);
        Player player = playerOf(panel);
        TurnScheduler scheduler = schedulerOf(panel);
        WorldPause worldPause = worldPauseOf(panel);
        worldPause.hold("pause");

        fireAction(panel, Keybindings.ACTION_MOVE_RIGHT);

        assertEquals(START_X, player.getX());
        assertEquals(0, scheduler.getTurn());
    }

    @Test
    void repositioningThePlayerDirectlyDoesNotAdvanceTheTurn() {
        GamePanel panel = panelWithTileToTheRight(FLOOR);

        Player player = playerOf(panel);
        TurnScheduler scheduler = schedulerOf(panel);

        player.setPosition(ELSEWHERE, ELSEWHERE);

        assertEquals(0, scheduler.getTurn());
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
        Player player = playerOf(panel);
        WorldPause worldPause = worldPauseOf(panel);
        player.setPosition(1, 1);
        worldPause.hold("pause");

        panel.resetState();

        assertPlayerOnSpawn(panel);
        assertFalse(panel.isPaused());
    }

    @Test
    void resetStateClearsTheTurnSchedulerAndItsRegistrations() {
        GamePanel panel = panelWithTileToTheRight(FLOOR);
        TurnScheduler scheduler = schedulerOf(panel);
        scheduler.register(new CountingTickable());
        fireAction(panel, Keybindings.ACTION_MOVE_RIGHT);

        panel.resetState();

        assertEquals(0, scheduler.getTurn());
        assertEquals(0, scheduler.registeredCount());
    }

    @Test
    void movementStillWorksAfterResetState() {
        GamePanel panel = new GamePanel();
        panel.resetState();
        Player player = panel.getPlayer();
        int startY = player.getY();
        // Generated terrain may block the tile above the spawn, so expect the move only when it is open.
        boolean upIsOpen = panel.getScene().isWalkable(player.getX(), startY - 1);

        fireAction(panel, Keybindings.ACTION_MOVE_UP);

        assertEquals(upIsOpen ? startY - 1 : startY, player.getY());
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

    private static void assertPlayerOnSpawn(GamePanel panel) {
        GeneratedWorldScene world = (GeneratedWorldScene) panel.getScene();
        assertEquals(world.getSpawnX(), panel.getPlayer().getX());
        assertEquals(world.getSpawnY(), panel.getPlayer().getY());
    }

    /** A panel with the player at (START_X, START_Y) on floor, and {@code tileToTheRight} beside it. */
    private static GamePanel panelWithTileToTheRight(Tile tileToTheRight) {
        WorldScene scene = FlatFloorWorld.create();
        scene.fillRegion(new Rectangle(TILE_RIGHT_X, START_Y, 1, 1), tileToTheRight);
        return new GamePanel(GameEventLog.noOp(), new Player(START_X, START_Y), scene);
    }

    private static final class CountingTickable implements Tickable {
        int steps;

        @Override
        public void onTurn(int turn) {
            steps++;
        }
    }

    private static Player playerOf(GamePanel panel) {
        return panel.getPlayer();
    }

    private static TurnScheduler schedulerOf(GamePanel panel) {
        return panel.getTurnScheduler();
    }

    private static WorldPause worldPauseOf(GamePanel panel) {
        return panel.getWorldPause();
    }

    private static void fireAction(GamePanel panel, String actionName) {
        Action action = panel.getActionMap().get(actionName);
        action.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, actionName));
    }
}
