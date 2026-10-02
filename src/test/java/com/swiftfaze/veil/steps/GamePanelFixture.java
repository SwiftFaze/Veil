package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.GameConst;
import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.GamePanel;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.CodexPanel;
import com.swiftfaze.veil.ui.InventoryPanel;
import com.swiftfaze.veil.ui.PopupToggleListener;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.TileTestScene2;
import com.swiftfaze.veil.world.WorldScene;

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

    private final GamePanel panel;
    private final Player player;
    private final WorldScene scene;

    /** A game panel wired to {@code log} alone. */
    GamePanelFixture(GameEventLog log) {
        this.player = new Player(GameConst.DEFAULT_PLAYER_START_X, GameConst.DEFAULT_PLAYER_START_Y);
        this.scene = new TileTestScene2(GameConst.DEFAULT_MAP_WIDTH, GameConst.DEFAULT_MAP_HEIGHT);
        this.panel = new GamePanel(log, player, scene);
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

    int playerX() {
        return player.getX();
    }

    void pressMoveRight() {
        fire(Keybindings.ACTION_MOVE_RIGHT);
    }

    void pressInventoryKey() {
        fire(Keybindings.ACTION_TOGGLE_INVENTORY);
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
}
