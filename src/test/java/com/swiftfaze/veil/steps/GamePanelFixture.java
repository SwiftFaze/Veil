package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.GameConst;
import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.GamePanel;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.CodexPanel;
import com.swiftfaze.veil.ui.InventoryPanel;
import com.swiftfaze.veil.ui.PauseMenuPopup;
import com.swiftfaze.veil.ui.PauseToggleListener;
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
    private InventoryPanel inventoryPanel;
    private CodexPanel codexPanel;
    private PauseMenuPopup pauseMenuPopup;

    /** A game panel wired to {@code log} alone. */
    GamePanelFixture(GameEventLog log) {
        this.player = new Player(GameConst.DEFAULT_PLAYER_START_X, GameConst.DEFAULT_PLAYER_START_Y);
        this.scene = new TileTestScene2(GameConst.DEFAULT_MAP_WIDTH, GameConst.DEFAULT_MAP_HEIGHT);
        this.panel = new GamePanel(log, player, scene);
    }

    /** A game panel wired to {@code log}, with the inventory and codex popup listener attached. */
    static GamePanelFixture withInventory(GameEventLog log) {
        GamePanelFixture fixture = new GamePanelFixture(log);
        ControlsHintBarWidget hintBar = new ControlsHintBarWidget();
        fixture.inventoryPanel = new InventoryPanel(hintBar);
        fixture.codexPanel = new CodexPanel(hintBar);
        fixture.panel.addGameListener(new PopupToggleListener(
                fixture.inventoryPanel, fixture.codexPanel, fixture.panel.getWorldPause(), log));
        return fixture;
    }

    /** A game panel wired as Main wires it: inventory, codex and pause menu all toggle the shared world pause. */
    static GamePanelFixture withAllScreens(GameEventLog log) {
        GamePanelFixture fixture = withInventory(log);
        fixture.pauseMenuPopup = new PauseMenuPopup();
        fixture.panel.addGameListener(new PauseToggleListener(fixture.panel, fixture.pauseMenuPopup));
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
        return panel.getPlayer().getX();
    }

    void pressMoveRight() {
        fire(Keybindings.ACTION_MOVE_RIGHT);
    }

    void pressInventoryKey() {
        fire(Keybindings.ACTION_TOGGLE_INVENTORY);
    }

    void pressCodexKey() {
        fire(Keybindings.ACTION_TOGGLE_CODEX);
    }

    void pressPauseKey() {
        fire(Keybindings.ACTION_TOGGLE_PAUSE);
    }

    /** Makes (x, y) an unwalkable wall, so a move into it is blocked. */
    void blockAt(int x, int y) {
        scene.fillRegion(new Rectangle(x, y, 1, 1), WALL);
    }

    /** Reads the live player, so it is still right after {@code GamePanel.resetState()} swaps it. */
    int playerY() {
        return panel.getPlayer().getY();
    }

    GamePanel panel() {
        return panel;
    }

    InventoryPanel inventoryPanel() {
        return inventoryPanel;
    }

    CodexPanel codexPanel() {
        return codexPanel;
    }

    PauseMenuPopup pauseMenuPopup() {
        return pauseMenuPopup;
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
