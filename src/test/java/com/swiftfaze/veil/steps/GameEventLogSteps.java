package com.swiftfaze.veil.steps;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.swiftfaze.veil.game.GamePanel;
import com.swiftfaze.veil.game.event.GameEvent;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.CodexPanel;
import com.swiftfaze.veil.ui.InventoryPanel;
import com.swiftfaze.veil.ui.PopupToggleListener;
import com.swiftfaze.veil.ui.ScreenNavigator;
import com.swiftfaze.veil.ui.TitleScreenPanel;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import com.swiftfaze.veil.world.Tile;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the real emitters (TitleScreenPanel, ScreenNavigator, GamePanel's key
 * actions, PopupToggleListener) and asserts on what they recorded. No step appends
 * to the log itself.
 */
public class GameEventLogSteps {
    private static final Tile FLOOR = new Tile("test:floor", '.', Color.GRAY, true);
    private static final Tile WALL = new Tile("test:wall", '#', Color.WHITE, false);

    private GameEventLog eventLog;
    private TitleScreenPanel titleScreen;
    private GamePanel gamePanel;
    private Path logFile;

    @After
    public void clearQaLogProperty() {
        System.clearProperty(GameEventLog.QA_LOG_PROPERTY);
    }

    @Given("an enabled game event log")
    public void anEnabledGameEventLog() {
        eventLog = GameEventLog.inMemory();
    }

    @Given("the title screen is showing")
    public void theTitleScreenIsShowing() {
        CardLayout cardLayout = new CardLayout();
        JPanel cardPanel = new JPanel(cardLayout);
        Map<String, JComponent> cards = new HashMap<>();
        ScreenNavigator navigator = new ScreenNavigator(cardLayout, cardPanel, cards, eventLog);
        // Mirrors Main's title-menu wiring: "New" switches to the game card.
        titleScreen = new TitleScreenPanel(item -> {
            if ("New".equals(item)) {
                navigator.navigateTo("game");
            }
        }, new ControlsHintBarWidget(), eventLog);
        cardPanel.add(titleScreen, "title");
        cardPanel.add(new JPanel(), "game");
        cards.put("title", titleScreen);
        navigator.showInitial("title");
    }

    @Given("the title screen is showing with the first option selected")
    public void theTitleScreenIsShowingWithTheFirstOptionSelected() {
        theTitleScreenIsShowing();
        assertEquals("Continue", titleScreen.getHighlightedMenuItem());
    }

    @When("the player chooses \"New Game\"")
    public void thePlayerChoosesNewGame() {
        // The title menu labels this item "New".
        while (!"New".equals(titleScreen.getHighlightedMenuItem())) {
            titleScreen.moveDown();
        }
        titleScreen.confirm();
    }

    @When("the player presses Down twice")
    public void thePlayerPressesDownTwice() {
        titleScreen.moveDown();
        titleScreen.moveDown();
    }

    @Given("the player is at \\({int}, {int}) on an open floor")
    public void thePlayerIsAtOnAnOpenFloor(int x, int y) {
        placePlayer(x, y, FLOOR);
    }

    @Given("the player is at \\({int}, {int}) with a wall to the right")
    public void thePlayerIsAtWithAWallToTheRight(int x, int y) {
        placePlayer(x, y, WALL);
    }

    @When("the player presses the move-right key")
    public void thePlayerPressesTheMoveRightKey() {
        fire(Keybindings.ACTION_MOVE_RIGHT);
    }

    @Given("the game screen is showing with the inventory closed")
    public void theGameScreenIsShowingWithTheInventoryClosed() {
        gamePanel = gamePanelWithInventory(eventLog);
    }

    @When("the player presses the inventory key twice")
    public void thePlayerPressesTheInventoryKeyTwice() {
        fire(Keybindings.ACTION_TOGGLE_INVENTORY);
        fire(Keybindings.ACTION_TOGGLE_INVENTORY);
    }

    @Given("a log that has recorded two events")
    public void aLogThatHasRecordedTwoEvents() {
        placePlayer(5, 5, FLOOR);
        gamePanel.getScene().fillRegion(new Rectangle(7, 5, 1, 1), FLOOR);
        fire(Keybindings.ACTION_MOVE_RIGHT);
        fire(Keybindings.ACTION_MOVE_RIGHT);
        assertEquals(2, eventLog.getEvents().size());
    }

    @Then("its events cannot be removed or replaced through the log's API")
    public void itsEventsCannotBeRemovedOrReplacedThroughTheLogsApi() {
        List<GameEvent> events = eventLog.getEvents();
        GameEvent replacement = GameEvent.playerMoved(0, 0);
        assertThrows(UnsupportedOperationException.class, () -> events.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> events.set(0, replacement));
        assertThrows(UnsupportedOperationException.class, events::clear);
        assertEquals(2, eventLog.getEvents().size());
    }

    @Given("the game starts without `-Dveil.qaLog`")
    public void theGameStartsWithoutQaLog() {
        System.clearProperty(GameEventLog.QA_LOG_PROPERTY);
        eventLog = GameEventLog.fromSystemProperties();
    }

    @When("the player moves and toggles the inventory")
    public void thePlayerMovesAndTogglesTheInventory() {
        gamePanel = gamePanelWithInventory(eventLog);
        gamePanel.getPlayer().setPosition(5, 5);
        gamePanel.getScene().fillRegion(new Rectangle(5, 5, 2, 1), FLOOR);
        fire(Keybindings.ACTION_MOVE_RIGHT);
        fire(Keybindings.ACTION_TOGGLE_INVENTORY);
        assertEquals(6, gamePanel.getPlayer().getX());
    }

    @Then("no events are kept and no log file is written")
    public void noEventsAreKeptAndNoLogFileIsWritten() {
        assertTrue(eventLog.getEvents().isEmpty());
        assertTrue(eventLog.getWriteFailure().isEmpty());
    }

    @Given("the game starts with `-Dveil.qaLog` pointing at a file")
    public void theGameStartsWithQaLogPointingAtAFile() throws IOException {
        logFile = Files.createTempDirectory("veil-qa-").resolve("qa.jsonl");
        startWithQaLog(logFile);
    }

    @When("the player moves right once")
    public void thePlayerMovesRightOnce() {
        placePlayer(5, 5, FLOOR);
        fire(Keybindings.ACTION_MOVE_RIGHT);
    }

    @Then("the file already has one line for that `PlayerMoved` before the game exits")
    public void theFileAlreadyHasOneLineForThatPlayerMoved() throws IOException {
        assertEquals(1, Files.readAllLines(logFile).size());
    }

    @Then("the line is a JSON object with its event type, `x` and `y`")
    public void theLineIsAJsonObjectWithItsEventTypeXAndY() throws IOException {
        JsonObject line = JsonParser.parseString(Files.readAllLines(logFile).get(0)).getAsJsonObject();
        assertEquals("PlayerMoved", line.get("type").getAsString());
        assertEquals(6, line.get("x").getAsInt());
        assertEquals(5, line.get("y").getAsInt());
    }

    @Given("the player has moved right twice")
    public void thePlayerHasMovedRightTwice() {
        placePlayer(5, 5, FLOOR);
        gamePanel.getScene().fillRegion(new Rectangle(7, 5, 1, 1), FLOOR);
        fire(Keybindings.ACTION_MOVE_RIGHT);
        fire(Keybindings.ACTION_MOVE_RIGHT);
    }

    @When("the game process is killed without a clean shutdown")
    public void theGameProcessIsKilledWithoutACleanShutdown() {
        // Nothing to do: the log is never closed or flushed at shutdown, so reading
        // the file now sees exactly what a killed process would have left behind.
    }

    @Then("the file holds both `PlayerMoved` lines")
    public void theFileHoldsBothPlayerMovedLines() throws IOException {
        List<String> lines = Files.readAllLines(logFile);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("\"x\":6"));
        assertTrue(lines.get(1).contains("\"x\":7"));
    }

    @Given("`-Dveil.qaLog` points at a directory that does not exist")
    public void qaLogPointsAtADirectoryThatDoesNotExist() throws IOException {
        Path missingDir = Files.createTempDirectory("veil-missing-");
        Files.delete(missingDir);
        logFile = missingDir.resolve("qa.jsonl");
        startWithQaLog(logFile);
    }

    @When("the first event is recorded")
    public void theFirstEventIsRecorded() {
        placePlayer(5, 5, FLOOR);
        assertDoesNotThrow(() -> fire(Keybindings.ACTION_MOVE_RIGHT));
    }

    @Then("the game reports an error naming the path")
    public void theGameReportsAnErrorNamingThePath() {
        String failure = eventLog.getWriteFailure().orElseThrow();
        assertTrue(failure.contains(logFile.toString()), failure);
    }

    @Then("the game itself keeps running")
    public void theGameItselfKeepsRunning() {
        assertEquals(6, gamePanel.getPlayer().getX());
        assertFalse(Files.exists(logFile));
        placePlayer(6, 5, FLOOR);
        assertDoesNotThrow(() -> fire(Keybindings.ACTION_MOVE_RIGHT));
        assertEquals(7, gamePanel.getPlayer().getX());
    }

    @Then("the log contains exactly one `ScreenChanged` from {string} to {string}")
    public void theLogContainsExactlyOneScreenChanged(String from, String to) {
        List<GameEvent.ScreenChanged> changes = eventsOf(GameEvent.ScreenChanged.class);
        assertEquals(List.of(new GameEvent.ScreenChanged(from, to)), changes);
    }

    @Then("the log contains two `MenuSelectionChanged` events, in order, naming the second and third options")
    public void theLogContainsTwoMenuSelectionChangedEvents() {
        assertEquals(List.of(new GameEvent.MenuSelectionChanged("New"), new GameEvent.MenuSelectionChanged("Load")),
                eventsOf(GameEvent.MenuSelectionChanged.class));
    }

    @Then("the log contains exactly one `PlayerMoved` at \\({int}, {int})")
    public void theLogContainsExactlyOnePlayerMovedAt(int x, int y) {
        assertEquals(List.of(new GameEvent.PlayerMoved(x, y)), eventsOf(GameEvent.PlayerMoved.class));
    }

    @Then("the log contains no `PlayerMoved` event")
    public void theLogContainsNoPlayerMovedEvent() {
        assertTrue(eventsOf(GameEvent.PlayerMoved.class).isEmpty());
        assertEquals(5, gamePanel.getPlayer().getX());
    }

    @Then("the log contains `PopupToggled` {string} open, then `PopupToggled` {string} closed")
    public void theLogContainsPopupToggledOpenThenClosed(String opened, String closed) {
        assertEquals(List.of(new GameEvent.PopupToggled(opened, true), new GameEvent.PopupToggled(closed, false)),
                eventsOf(GameEvent.PopupToggled.class));
    }

    private void startWithQaLog(Path path) {
        System.setProperty(GameEventLog.QA_LOG_PROPERTY, path.toString());
        eventLog = GameEventLog.fromSystemProperties();
    }

    private void placePlayer(int x, int y, Tile tileToTheRight) {
        if (gamePanel == null) {
            gamePanel = new GamePanel(eventLog);
        }
        gamePanel.getPlayer().setPosition(x, y);
        gamePanel.getScene().fillRegion(new Rectangle(x, y, 1, 1), FLOOR);
        gamePanel.getScene().fillRegion(new Rectangle(x + 1, y, 1, 1), tileToTheRight);
    }

    private static GamePanel gamePanelWithInventory(GameEventLog log) {
        GamePanel panel = new GamePanel(log);
        ControlsHintBarWidget hintBar = new ControlsHintBarWidget();
        panel.addGameListener(new PopupToggleListener(new InventoryPanel(hintBar), new CodexPanel(hintBar), log));
        return panel;
    }

    private void fire(String actionName) {
        gamePanel.getActionMap().get(actionName)
                .actionPerformed(new ActionEvent(gamePanel, ActionEvent.ACTION_PERFORMED, actionName));
    }

    private <T extends GameEvent> List<T> eventsOf(Class<T> type) {
        return eventLog.getEvents().stream().filter(type::isInstance).map(type::cast).toList();
    }
}
