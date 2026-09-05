package com.swiftfaze.veil.game;

import com.swiftfaze.veil.Camera;
import com.swiftfaze.veil.DrawableAsciiEntity;
import com.swiftfaze.veil.Positionable;
import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.world.TileTestScene2;
import com.swiftfaze.veil.world.WorldScene;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.swiftfaze.veil.GameConst.*;

public class GamePanel extends JPanel {

    private Player player = new Player(DEFAULT_PLAYER_START_X, DEFAULT_PLAYER_START_Y);
    private TileTestScene2 scene = new TileTestScene2(DEFAULT_MAP_WIDTH, DEFAULT_MAP_HEIGHT);
    private final Camera camera = new Camera(GAME_WINDOW_WIDTH, GAME_WINDOW_HEIGHT);
    private final List<Positionable> entitiesToDraw = new ArrayList<>();
    private final List<GameListener> listeners = new ArrayList<>();
    private boolean paused = false;

    public GamePanel() {
        setPreferredSize(new Dimension(GAME_WINDOW_WIDTH * TILE_WIDTH, GAME_WINDOW_HEIGHT * TILE_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        player.setPosition(DEFAULT_PLAYER_START_X, DEFAULT_PLAYER_START_Y);

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
        inputMap.put(Keybindings.MENU_CANCEL, Keybindings.ACTION_TOGGLE_PAUSE);

        actionMap.put(Keybindings.ACTION_MOVE_UP, new MoveAction(worldScene -> player.moveUp(worldScene)));
        actionMap.put(Keybindings.ACTION_MOVE_DOWN, new MoveAction(worldScene -> player.moveDown(worldScene)));
        actionMap.put(Keybindings.ACTION_MOVE_LEFT, new MoveAction(worldScene -> player.moveLeft(worldScene)));
        actionMap.put(Keybindings.ACTION_MOVE_RIGHT, new MoveAction(worldScene -> player.moveRight(worldScene)));
        actionMap.put(Keybindings.ACTION_TOGGLE_INVENTORY, new ToggleInventoryAction());
        actionMap.put(Keybindings.ACTION_TOGGLE_CODEX, new ToggleCodexAction());
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
        player = new Player(DEFAULT_PLAYER_START_X, DEFAULT_PLAYER_START_Y);
        scene = new TileTestScene2(DEFAULT_MAP_WIDTH, DEFAULT_MAP_HEIGHT);
        entitiesToDraw.clear();
        addEntity(scene);
        addEntity(player);
        paused = false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        camera.resizeViewport(getWidth() / TILE_WIDTH, getHeight() / TILE_HEIGHT);
        camera.centerOn(player.getX(), player.getY());

        scene.renderWorld(
                g2d,
                TILE_WIDTH,
                TILE_HEIGHT,
                camera.getX(),
                camera.getY()
        );

        for (Positionable entity : entitiesToDraw) {
            if (entity == scene) continue;

            if (entity instanceof DrawableAsciiEntity ascii) {
                ascii.render(
                        g2d,
                        TILE_WIDTH,
                        TILE_HEIGHT,
                        camera.getX(),
                        camera.getY()
                );
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
            move.accept(scene);
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

    private class TogglePauseAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            for (GameListener l : listeners) {
                l.togglePause();
            }
        }
    }
}
