package com.swiftfaze.veil.game.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameEventLogTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void clearProperty() {
        System.clearProperty(GameEventLog.QA_LOG_PROPERTY);
    }

    @Test
    void noOpLogKeepsNothing() {
        GameEventLog log = GameEventLog.noOp();
        log.append(GameEvent.playerMoved(1, 2));
        assertTrue(log.getEvents().isEmpty());
    }

    @Test
    void inMemoryLogKeepsEventsInOrder() {
        GameEventLog log = GameEventLog.inMemory();
        log.append(GameEvent.menuSelectionChanged("New"));
        log.append(GameEvent.playerMoved(1, 2));
        assertEquals(List.of(new GameEvent.MenuSelectionChanged("New"), new GameEvent.PlayerMoved(1, 2)),
                log.getEvents());
    }

    @Test
    void fileLogWritesEachEventAsATypedJsonLineImmediately() throws IOException {
        Path file = tempDir.resolve("qa.jsonl");
        GameEventLog log = GameEventLog.toFile(file);
        log.append(GameEvent.screenChanged("title", "game"));
        assertEquals(List.of("{\"type\":\"ScreenChanged\",\"from\":\"title\",\"to\":\"game\"}"),
                Files.readAllLines(file));
        log.append(GameEvent.popupToggled("inventory", true));
        assertEquals("{\"type\":\"PopupToggled\",\"name\":\"inventory\",\"open\":true}",
                Files.readAllLines(file).get(1));
    }

    @Test
    void unwritablePathRecordsAFailureNamingThePathAndKeepsTheEvent() {
        Path file = tempDir.resolve("missing").resolve("qa.jsonl");
        GameEventLog log = GameEventLog.toFile(file);
        log.append(GameEvent.playerMoved(1, 2));
        log.append(GameEvent.playerMoved(2, 2));
        assertTrue(log.getWriteFailure().orElseThrow().contains(file.toString()));
        assertEquals(2, log.getEvents().size());
    }

    @Test
    void systemPropertySelectsAFileLog() throws IOException {
        Path file = tempDir.resolve("qa.jsonl");
        System.setProperty(GameEventLog.QA_LOG_PROPERTY, file.toString());
        GameEventLog.fromSystemProperties().append(GameEvent.playerMoved(3, 4));
        assertEquals(1, Files.readAllLines(file).size());
    }

    @Test
    void blankSystemPropertySelectsANoOpLog() {
        System.setProperty(GameEventLog.QA_LOG_PROPERTY, " ");
        GameEventLog log = GameEventLog.fromSystemProperties();
        log.append(GameEvent.playerMoved(3, 4));
        assertTrue(log.getEvents().isEmpty());
    }
}
