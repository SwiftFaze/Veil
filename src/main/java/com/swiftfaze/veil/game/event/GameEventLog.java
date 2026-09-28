package com.swiftfaze.veil.game.event;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Append-only log of game events. Records to memory and optionally to a file
 * as JSON lines (one event per line). Each event is written and flushed immediately
 * upon append, so a killed process preserves all events written so far.
 */
public class GameEventLog {
    private static final Logger logger = LoggerFactory.getLogger(GameEventLog.class);
    private static final Gson gson = new Gson();

    private final List<GameEvent> events = new ArrayList<>();
    private final Writer fileWriter;
    private final boolean isEnabled;

    /**
     * Creates a no-op log that records nothing and writes nowhere.
     */
    public GameEventLog() {
        this.fileWriter = null;
        this.isEnabled = false;
    }

    /**
     * Creates a log that writes to a file.
     *
     * @param path the file to write events to; parent directories must exist
     * @throws IOException if the file cannot be created or written to
     */
    public GameEventLog(Path path) throws IOException {
        this.fileWriter = Files.newBufferedWriter(
                path,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND
        );
        this.isEnabled = true;
    }

    /**
     * Creates a log that writes to an existing writer.
     *
     * @param writer a writer to write events to; ownership passes to this log
     */
    public GameEventLog(Writer writer) {
        this.fileWriter = writer;
        this.isEnabled = true;
    }

    /**
     * Creates a GameEventLog from system properties. If `-Dveil.qaLog=<path>`
     * is set and the path is writable, returns an enabled log. Otherwise returns
     * a no-op log. If the path cannot be written, logs an error and returns a no-op.
     *
     * @return a GameEventLog, either enabled or no-op
     */
    public static GameEventLog fromSystemProperties() {
        String qaLogPath = System.getProperty("veil.qaLog");
        if (qaLogPath == null || qaLogPath.isEmpty()) {
            return new GameEventLog();
        }

        try {
            Path path = Path.of(qaLogPath);
            return new GameEventLog(path);
        } catch (IOException e) {
            logger.error("Failed to create/open QA log at {}", qaLogPath, e);
            return new GameEventLog();
        }
    }

    /**
     * Appends an event to the log. If file writing is enabled, immediately
     * writes and flushes a JSON line. If writing fails, logs an error but
     * keeps the game running.
     *
     * @param event the event to record
     */
    public void append(GameEvent event) {
        if (!isEnabled) {
            return;
        }

        events.add(event);

        if (fileWriter == null) {
            return;
        }

        try {
            String json = gson.toJson(event);
            fileWriter.write(json);
            fileWriter.write('\n');
            fileWriter.flush();
        } catch (IOException e) {
            logger.error("Failed to write QA event to log", e);
        }
    }

    /**
     * Returns an unmodifiable view of all recorded events.
     *
     * @return an unmodifiable list of events
     */
    public List<GameEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Returns the number of events recorded.
     *
     * @return event count
     */
    public int size() {
        return events.size();
    }

    /**
     * Closes the underlying file writer if one exists.
     *
     * @throws IOException if closing fails
     */
    public void close() throws IOException {
        if (fileWriter != null) {
            fileWriter.close();
        }
    }
}
