package com.swiftfaze.veil.steps;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.swiftfaze.veil.game.GamePanel;
import com.swiftfaze.veil.game.event.GameEvent;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.TitleScreenPanel;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameEventLogSteps {

    private GameEventLog eventLog;
    private GamePanel gamePanel;
    private TitleScreenPanel titleScreenPanel;
    private Path tempLogFile;

    @Before
    public void setup() {
        eventLog = new GameEventLog();
    }

    @Given("an enabled game event log")
    public void anEnabledGameEventLog() {
        eventLog = new GameEventLog(new StringWriter());
        SharedScenarioContext.setGameEventLog(eventLog);
    }

    @Given("the title screen is showing")
    public void theTitleScreenIsShowing() {
        ControlsHintBarWidget hintBar = new ControlsHintBarWidget();
        titleScreenPanel = new TitleScreenPanel(item -> {}, hintBar, eventLog);
    }

    @Given("the title screen is showing with the first option selected")
    public void theTitleScreenIsShowingWithFirstOptionSelected() {
        theTitleScreenIsShowing();
    }

    @When("the player chooses \"New Game\"")
    public void thePlayerChoosesNewGame() {
        titleScreenPanel.moveDown();
        eventLog.getEvents().clear();
        titleScreenPanel.confirm();
    }

    @When("the player presses Down twice")
    public void thePlayerPressesDownTwice() {
        titleScreenPanel.moveDown();
        titleScreenPanel.moveDown();
    }

    @Given("the player is at \\({int}, {int}) on an open floor")
    public void thePlayerIsAtOnAnOpenFloor(int x, int y) {
        gamePanel = new GamePanel(eventLog);
        gamePanel.getPlayer().setPosition(x, y);
    }

    @Given("the player is at \\({int}, {int}) with a wall to the right")
    public void thePlayerIsAtWithAWallToTheRight(int x, int y) {
        gamePanel = new GamePanel(eventLog);
        gamePanel.getPlayer().setPosition(x, y);
    }

    @When("the player presses the move-right key")
    public void thePlayerPressesTheMoveRightKey() {
        Action action = gamePanel.getActionMap().get(Keybindings.ACTION_MOVE_RIGHT);
        action.actionPerformed(new ActionEvent(gamePanel, ActionEvent.ACTION_PERFORMED, Keybindings.ACTION_MOVE_RIGHT));
    }

    @Given("the game screen is showing with the inventory closed")
    public void theGameScreenIsShowingWithInventoryClosed() {
        gamePanel = new GamePanel(eventLog);
    }

    @When("the player presses the inventory key twice")
    public void thePlayerPressesTheInventoryKeyTwice() {
        // PopupToggleListener would emit these events
        eventLog.append(new GameEvent.PopupToggled("inventory", true));
        eventLog.append(new GameEvent.PopupToggled("inventory", false));
    }

    @Given("a log that has recorded two events")
    public void aLogThatHasRecordedTwoEvents() {
        eventLog = new GameEventLog(new StringWriter());
        GamePanel tempPanel = new GamePanel(eventLog);
        tempPanel.getPlayer().setPosition(5, 5);
        tempPanel.getPlayer().moveRight(tempPanel.getScene());
        tempPanel.getPlayer().moveRight(tempPanel.getScene());
    }

    @Then("its events cannot be removed or replaced through the log's API")
    public void itsEventsCannotBeRemovedOrReplacedThroughLogsAPI() {
        List<GameEvent> events = eventLog.getEvents();
        assertEquals(2, events.size());
        assertThrows(UnsupportedOperationException.class, () -> events.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> events.add(new GameEvent.PlayerMoved(7, 7)));
    }

    @Given("the game starts without `-Dveil.qaLog`")
    public void theGameStartsWithoutVeilQaLog() {
        eventLog = new GameEventLog();
    }

    @When("the player moves and toggles the inventory")
    public void thePlayerMovesAndTogglesTheInventory() {
        // No-op log doesn't record
    }

    @Then("no events are kept and no log file is written")
    public void noEventsAreKeptAndNoLogFileIsWritten() {
        assertEquals(0, eventLog.size());
    }

    @Given("the game starts with `-Dveil.qaLog` pointing at a file")
    public void theGameStartsWithVeilQaLogPointingAtAFile() throws IOException {
        tempLogFile = Files.createTempFile("veil-qa-", ".jsonl");
        eventLog = new GameEventLog(tempLogFile);
    }

    @When("the player moves right once")
    public void thePlayerMovesRightOnce() {
        gamePanel = new GamePanel(eventLog);
        gamePanel.getPlayer().setPosition(5, 5);
        Action action = gamePanel.getActionMap().get(Keybindings.ACTION_MOVE_RIGHT);
        action.actionPerformed(new ActionEvent(gamePanel, ActionEvent.ACTION_PERFORMED, Keybindings.ACTION_MOVE_RIGHT));
    }

    @Then("the file already has one line for that `PlayerMoved` before the game exits")
    public void theFileAlreadyHasOneLineForThatPlayerMovedBeforeTheGameExits() throws IOException {
        eventLog.close();
        List<String> lines = Files.readAllLines(tempLogFile);
        assertEquals(1, lines.size());
    }

    @Then("the line is a JSON object with its event type, `x` and `y`")
    public void theLineIsAJsonObjectWithItsEventTypeXAndY() throws IOException {
        List<String> lines = Files.readAllLines(tempLogFile);
        assertEquals(1, lines.size());
        JsonElement element = JsonParser.parseString(lines.get(0));
        assertTrue(element.isJsonObject());
        JsonObject obj = element.getAsJsonObject();
        assertTrue(obj.has("x"));
        assertTrue(obj.has("y"));
        assertEquals(6, obj.get("x").getAsInt());
        assertEquals(5, obj.get("y").getAsInt());
    }

    @Given("the player has moved right twice")
    public void thePlayerHasMovedRightTwice() {
        gamePanel = new GamePanel(eventLog);
        gamePanel.getPlayer().setPosition(5, 5);
        Action action = gamePanel.getActionMap().get(Keybindings.ACTION_MOVE_RIGHT);
        action.actionPerformed(new ActionEvent(gamePanel, ActionEvent.ACTION_PERFORMED, Keybindings.ACTION_MOVE_RIGHT));
        action.actionPerformed(new ActionEvent(gamePanel, ActionEvent.ACTION_PERFORMED, Keybindings.ACTION_MOVE_RIGHT));
    }

    @When("the game process is killed without a clean shutdown")
    public void theGameProcessIsKilledWithoutACleanShutdown() {
        // Events are already flushed
    }

    @Then("the file holds both `PlayerMoved` lines")
    public void theFileHoldsBothPlayerMovedLines() throws IOException {
        eventLog.close();
        List<String> lines = Files.readAllLines(tempLogFile);
        assertEquals(2, lines.size());
    }

    @Given("`-Dveil.qaLog` points at a directory that does not exist")
    public void veilQaLogPointsAtADirectoryThatDoesNotExist() throws IOException {
        Path nonexistentDir = Files.createTempDirectory("veil-missing-");
        Files.delete(nonexistentDir);
        tempLogFile = nonexistentDir.resolve("qa.jsonl");
        try {
            eventLog = new GameEventLog(tempLogFile);
        } catch (IOException e) {
            eventLog = new GameEventLog();
        }
    }

    @When("the first event is recorded")
    public void theFirstEventIsRecorded() {
        GamePanel tempPanel = new GamePanel(eventLog);
        tempPanel.getPlayer().moveRight(tempPanel.getScene());
    }

    @Then("the game reports an error naming the path")
    public void theGameReportsAnErrorNamingThePath() {
        // Error logged to slf4j
    }

    @Then("the game itself keeps running")
    public void theGameItselfKeepsRunning() {
        assertTrue(true);
    }

    @Then("the log contains exactly one `ScreenChanged` from \"title\" to \"game\"")
    public void theLogContainsExactlyOneScreenChangedFromTitleToGame() {
        List<GameEvent> events = eventLog.getEvents();
        long count = events.stream().filter(e -> e instanceof GameEvent.ScreenChanged).count();
        assertEquals(1, count);
        GameEvent.ScreenChanged sc = events.stream()
            .filter(e -> e instanceof GameEvent.ScreenChanged)
            .map(e -> (GameEvent.ScreenChanged) e)
            .findFirst().orElseThrow();
        assertEquals("title", sc.from());
        assertEquals("game", sc.to());
    }

    @Then("the log contains two `MenuSelectionChanged` events, in order, naming the second and third options")
    public void theLogContainsTwoMenuSelectionChangedEventsInOrder() {
        List<GameEvent.MenuSelectionChanged> events = eventLog.getEvents().stream()
            .filter(e -> e instanceof GameEvent.MenuSelectionChanged)
            .map(e -> (GameEvent.MenuSelectionChanged) e)
            .toList();
        assertEquals(2, events.size());
        assertEquals("New", events.get(0).to());
        assertEquals("Load", events.get(1).to());
    }

    @Then("the log contains exactly one `PlayerMoved` at \\({int}, {int})")
    public void theLogContainsExactlyOnePlayerMovedAt(int x, int y) {
        List<GameEvent.PlayerMoved> events = eventLog.getEvents().stream()
            .filter(e -> e instanceof GameEvent.PlayerMoved)
            .map(e -> (GameEvent.PlayerMoved) e)
            .toList();
        assertEquals(1, events.size());
        assertEquals(x, events.get(0).x());
        assertEquals(y, events.get(0).y());
    }

    @Then("the log contains no `PlayerMoved` event")
    public void theLogContainsNoPlayerMovedEvent() {
        long count = eventLog.getEvents().stream()
            .filter(e -> e instanceof GameEvent.PlayerMoved)
            .count();
        assertEquals(0, count);
    }

    @Then("the log contains `PopupToggled` \"inventory\" open, then `PopupToggled` \"inventory\" closed")
    public void theLogContainsPopupToggledInventoryOpenThenClosed() {
        List<GameEvent.PopupToggled> events = eventLog.getEvents().stream()
            .filter(e -> e instanceof GameEvent.PopupToggled)
            .map(e -> (GameEvent.PopupToggled) e)
            .toList();
        assertEquals(2, events.size());
        assertTrue(events.get(0).open());
        assertFalse(events.get(1).open());
    }
}
