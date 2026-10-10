package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.GameConst;
import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.GamePanel;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.render.Positionable;
import com.swiftfaze.veil.ui.ChestPanel;
import com.swiftfaze.veil.ui.ChestPanelListener;
import com.swiftfaze.veil.ui.CodexPanel;
import com.swiftfaze.veil.ui.InventoryPanel;
import com.swiftfaze.veil.ui.PopupToggleListener;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.TileTestScene2;
import com.swiftfaze.veil.world.WorldScene;
import com.swiftfaze.veil.world.object.InteractableObject;

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

/**
 * A real {@link GamePanel} for step definitions, with its player and scene resolved once
 * and the few things a scenario does to it (place the player, press a key) as methods.
 */
final class GamePanelFixture {
    private static final Tile FLOOR = new Tile("test:floor", '.', Color.GRAY, true);
    private static final Tile WALL = new Tile("test:wall", '#', Color.WHITE, false);
    private static final int FLAT_FLOOR_SIZE = 20;

    /** An entity that is not the player, standing on a tile. */
    record Occupant(int x, int y) implements Positionable {
        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }
    }

    private final GamePanel panel;
    private final Player player;
    private final WorldScene scene;
    private final ChestPanel chestPanel;

    /** A game panel wired to {@code log} alone. */
    GamePanelFixture(GameEventLog log) {
        this(log, new Player(GameConst.DEFAULT_PLAYER_START_X, GameConst.DEFAULT_PLAYER_START_Y),
                new TileTestScene2(GameConst.DEFAULT_MAP_WIDTH, GameConst.DEFAULT_MAP_HEIGHT));
    }

    private GamePanelFixture(GameEventLog log, Player player, WorldScene scene) {
        this.player = player;
        this.scene = scene;
        this.panel = new GamePanel(log, player, scene);
        this.chestPanel = new ChestPanel(new ControlsHintBarWidget());
        panel.addGameListener(new ChestPanelListener(chestPanel));
    }

    /** A game panel on an open floor with the player at (5, 5), wired to {@code log} and a chest panel. */
    static GamePanelFixture onFlatFloor(GameEventLog log) {
        WorldScene floor = new WorldScene(FLAT_FLOOR_SIZE, FLAT_FLOOR_SIZE) {
        };
        floor.fillAll(FLOOR);
        return new GamePanelFixture(log, new Player(5, 5), floor);
    }

    /** A game panel wired to {@code log}, with the inventory popup listener attached. */
    static GamePanelFixture withInventory(GameEventLog log) {
        GamePanelFixture fixture = new GamePanelFixture(log);
        ControlsHintBarWidget hintBar = new ControlsHintBarWidget();
        fixture.panel.addGameListener(new PopupToggleListener(new InventoryPanel(hintBar), new CodexPanel(hintBar), log));
        return fixture;
    }

    void placePlayerOnOpenFloor(int x, int y) {
        placePlayer(x, y, FLOOR);
    }

    void placePlayerBesideWall(int x, int y) {
        placePlayer(x, y, WALL);
    }

    void openFloorAt(int x, int y) {
        scene.fillRegion(new Rectangle(x, y, 1, 1), FLOOR);
    }

    void movePlayerTo(int x, int y) {
        player.setPosition(x, y);
    }

    void placeObject(int x, int y, InteractableObject object) {
        scene.placeObject(x, y, object);
    }

    void addOccupant(int x, int y) {
        panel.addEntity(new Occupant(x, y));
    }

    WorldScene scene() {
        return scene;
    }

    ChestPanel chestPanel() {
        return chestPanel;
    }

    int playerX() {
        return player.getX();
    }

    int playerY() {
        return player.getY();
    }

    void pressMoveRight() {
        fire(Keybindings.ACTION_MOVE_RIGHT);
    }

    void pressMoveUp() {
        fire(Keybindings.ACTION_MOVE_UP);
    }

    void pressInventoryKey() {
        fire(Keybindings.ACTION_TOGGLE_INVENTORY);
    }

    /**
     * Presses a key the way Swing would: an open popup's own bindings are consulted first, then
     * the game panel's window-level bindings.
     */
    void pressKey(KeyStroke key) {
        if (chestPanel.isVisible() && fireBound(chestPanel, chestPanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT), key)) {
            return;
        }
        fireBound(panel, panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW), key);
    }

    private void placePlayer(int x, int y, Tile tileToTheRight) {
        player.setPosition(x, y);
        openFloorAt(x, y);
        scene.fillRegion(new Rectangle(x + 1, y, 1, 1), tileToTheRight);
    }

    private void fire(String actionName) {
        panel.getActionMap().get(actionName)
                .actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, actionName));
    }

    private static boolean fireBound(JComponent owner, InputMap inputMap, KeyStroke key) {
        Object actionName = inputMap.get(key);
        if (actionName == null) {
            return false;
        }
        owner.getActionMap().get(actionName)
                .actionPerformed(new ActionEvent(owner, ActionEvent.ACTION_PERFORMED, actionName.toString()));
        return true;
    }
}
