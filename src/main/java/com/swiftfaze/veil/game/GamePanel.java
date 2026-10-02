package com.swiftfaze.veil.game;

import com.swiftfaze.veil.GameConst;
import com.swiftfaze.veil.render.Camera;
import com.swiftfaze.veil.render.DrawableAsciiEntity;
import com.swiftfaze.veil.render.Positionable;
import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.event.GameEvent;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.world.TileTestScene2;
import com.swiftfaze.veil.world.WorldScene;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;


public final class GamePanel extends JPanel {

    private Player player;
    private WorldScene scene;
    private final Camera camera = new Camera(GameConst.GAME_WINDOW_WIDTH, GameConst.GAME_WINDOW_HEIGHT);
    private final List<Positionable> entitiesToDraw = new ArrayList<>();
    private final List<GameListener> listeners = new ArrayList<>();
    private final GameEventLog eventLog;
    private boolean paused = false;

    public GamePanel() {
        this(GameEventLog.noOp());
    }

    public GamePanel(GameEventLog eventLog) {
        this(eventLog, new Player(GameConst.DEFAULT_PLAYER_START_X, GameConst.DEFAULT_PLAYER_START_Y),
                new TileTestScene2(GameConst.DEFAULT_MAP_WIDTH, GameConst.DEFAULT_MAP_HEIGHT));
    }

    public GamePanel(GameEventLog eventLog, Player player, WorldScene scene) {
        this.player = player;
        this.scene = scene;
        this.eventLog = eventLog;
        setPreferredSize(new Dimension(GameConst.GAME_WINDOW_WIDTH * GameConst.TILE_WIDTH, GameConst.GAME_WINDOW_HEIGHT * GameConst.TILE_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        addEntity(scene);
        addEntity(player);

        bindKeys();
    }

    private void bindKeys() {
        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        inputMap.put(Keybindings.MOVE_UP_Z, Keybindings.ACTION_MOVE_UP);
        inputMap.put(Keybindings.MOVE_UP_ARROW, Keybindings.ACTION_MOVE_UP);
        inputMap.put(Keybindings.MOVE_DOWN_S, Keybindings.ACTION_MOVE_DOWN);
        inputMap.put(Keybindings.MOVE_DOWN_ARROW, Keybindings.ACTION_MOVE_DOWN);
        inputMap.put(Keybindings.MOVE_LEFT_Q, Keybindings.ACTION_MOVE_LEFT);
        inputMap.put(Keybindings.MOVE_LEFT_ARROW, Keybindings.ACTION_MOVE_LEFT);
        inputMap.put(Keybindings.MOVE_RIGHT_D, Keybindings.ACTION_MOVE_RIGHT);
        inputMap.put(Keybindings.MOVE_RIGHT_ARROW, Keybindings.ACTION_MOVE_RIGHT);
        inputMap.put(Keybindings.TOGGLE_INVENTORY, Keybindings.ACTION_TOGGLE_INVENTORY);
        inputMap.put(Keybindings.TOGGLE_CODEX, Keybindings.ACTION_TOGGLE_CODEX);
        inputMap.put(Keybindings.TOGGLE_DEV_CONSOLE, Keybindings.ACTION_TOGGLE_DEV_CONSOLE);
        inputMap.put(Keybindings.MENU_CANCEL, Keybindings.ACTION_TOGGLE_PAUSE);

        actionMap.put(Keybindings.ACTION_MOVE_UP, new MoveAction(worldScene -> player.moveUp(worldScene)));
        actionMap.put(Keybindings.ACTION_MOVE_DOWN, new MoveAction(worldScene -> player.moveDown(worldScene)));
        actionMap.put(Keybindings.ACTION_MOVE_LEFT, new MoveAction(worldScene -> player.moveLeft(worldScene)));
        actionMap.put(Keybindings.ACTION_MOVE_RIGHT, new MoveAction(worldScene -> player.moveRight(worldScene)));
        actionMap.put(Keybindings.ACTION_TOGGLE_INVENTORY, new ToggleInventoryAction());
        actionMap.put(Keybindings.ACTION_TOGGLE_CODEX, new ToggleCodexAction());
        actionMap.put(Keybindings.ACTION_TOGGLE_DEV_CONSOLE, new ToggleDevConsoleAction());
        actionMap.put(Keybindings.ACTION_TOGGLE_PAUSE, new TogglePauseAction());
    }

    private void notifyPlayerUpdated() {
        for (GameListener l : listeners) {
            l.updatePlayer(player);
        }
        repaint();
    }

    public void addEntity(Positionable entity) {
        entitiesToDraw.add(entity);
    }

    public void startGameLoop() {
        requestFocusInWindow();
    }

    public void addGameListener(GameListener listener) {
        listeners.add(listener);
    }

    public Player getPlayer() {
        return player;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    /**
     * Resets Player/WorldScene state back to a fresh session, so a subsequent
     * New/Continue doesn't inherit stale state from an exited game (e.g. via
     * the pause menu's Exit to Main Menu). Replaces the old dispose-and-
     * reload-everything approach (removed with the F5 hot-reset feature) with
     * an in-place reset of just this panel's own state.
     */
    public void resetState() {
        player = new Player(GameConst.DEFAULT_PLAYER_START_X, GameConst.DEFAULT_PLAYER_START_Y);
        scene = new TileTestScene2(GameConst.DEFAULT_MAP_WIDTH, GameConst.DEFAULT_MAP_HEIGHT);
        entitiesToDraw.clear();
        addEntity(scene);
        addEntity(player);
        paused = false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        camera.resizeViewport(getWidth() / GameConst.TILE_WIDTH, getHeight() / GameConst.TILE_HEIGHT);
        camera.centerOn(player.getX(), player.getY());

        scene.renderWorld(g2d, GameConst.TILE_WIDTH, GameConst.TILE_HEIGHT, camera);

        for (Positionable entity : entitiesToDraw) {
            if (Objects.equals(entity, scene)) {
                continue;
            }

            if (entity instanceof DrawableAsciiEntity ascii) {
                ascii.render(g2d, GameConst.TILE_WIDTH, GameConst.TILE_HEIGHT, camera);
            }
        }
    }

    private class MoveAction extends AbstractAction {
        private final Consumer<WorldScene> move;

        MoveAction(Consumer<WorldScene> move) {
            this.move = move;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (paused) {
                return;
            }
            int beforeX = player.getX();
            int beforeY = player.getY();
            move.accept(scene);
            int afterX = player.getX();
            int afterY = player.getY();
            if (beforeX != afterX || beforeY != afterY) {
                eventLog.recordEvent(GameEvent.playerMoved(afterX, afterY));
            }
            notifyPlayerUpdated();
        }
    }

    private class ToggleInventoryAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            for (GameListener l : listeners) {
                l.toggleInventory();
            }
        }
    }

    private class ToggleCodexAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            for (GameListener l : listeners) {
                l.toggleCodex();
            }
        }
    }

    private class ToggleDevConsoleAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            for (GameListener l : listeners) {
                l.toggleDevConsole();
            }
        }
    }

    private class TogglePauseAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            for (GameListener l : listeners) {
                l.togglePause();
            }
        }
    }
}
