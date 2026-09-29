package com.swiftfaze.veil.testing.qa;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.swiftfaze.veil.Main;
import com.swiftfaze.veil.game.event.GameEventLog;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Replays a {@code specs/qa/<slug>.keys} script as real key events against the real game
 * (booted in-process through {@link Main#main}) and checks the game event log against
 * {@code specs/qa/<slug>.json}. Test scope, local only: it opens a window and needs focus.
 *
 * <pre>
 * mvn -q test-compile exec:java -Dexec.classpathScope=test \
 *     -Dexec.mainClass=com.swiftfaze.veil.testing.qa.QaRunner "-Dexec.args=map-movement"
 * mvn -q test-compile exec:java -Dexec.classpathScope=test \
 *     -Dexec.mainClass=com.swiftfaze.veil.testing.qa.QaRunner "-Dexec.args=--all"
 * </pre>
 *
 * Output is one PASS/FAIL line per procedure; the exit code is non-zero if any failed.
 * {@link #main} ends in {@code System.exit}: Swing's non-daemon EDT (and the game's own
 * timers) would otherwise keep the JVM, and so {@code exec:java}, alive forever.
 */
public final class QaRunner {
    static final Path QA_DIR = Path.of("specs", "qa");

    private static final int FOCUS_ATTEMPTS = 100;
    private static final int POLL_MS = 50;
    private static final int SETTLE_MS = 150;

    private QaRunner() {
    }

    public static void main(String[] args) {
        int code;
        try {
            code = run(args);
        } catch (RuntimeException e) {
            System.out.println("FAIL: " + e.getMessage());
            code = 1;
        }
        System.exit(code);
    }

    private static int run(String[] args) {
        if (args.length != 1) {
            System.out.println("usage: QaRunner <slug> | --all");
            return 2;
        }
        List<String> slugs = "--all".equals(args[0]) ? allSlugs() : List.of(args[0]);
        int failures = 0;
        for (String slug : slugs) {
            if (!runOne(slug)) {
                failures++;
            }
        }
        return failures == 0 ? 0 : 1;
    }

    private static List<String> allSlugs() {
        try (Stream<Path> files = Files.list(QA_DIR)) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".keys") || n.endsWith(".json"))
                    .map(n -> n.substring(0, n.lastIndexOf('.')))
                    .distinct().sorted().toList();
        } catch (IOException e) {
            throw new QaException("Cannot list " + QA_DIR + ": " + e.getMessage());
        }
    }

    private static boolean runOne(String slug) {
        try {
            // Loading (and so key-name validation) happens before any window opens.
            QaProcedure procedure = QaProcedure.load(QA_DIR, slug);
            EventMatcher.Result result = EventMatcher.match(procedure.expected(), replay(procedure));
            if (result.passed()) {
                System.out.println("PASS " + slug);
                return true;
            }
            System.out.println("FAIL " + slug + "\n" + result.describeFailure().indent(2).stripTrailing());
        } catch (QaException e) {
            System.out.println("FAIL " + slug + ": " + e.getMessage());
        } finally {
            disposeAllWindows();
        }
        return false;
    }

    private static List<JsonObject> replay(QaProcedure procedure) {
        Path log = newLogFile();
        System.setProperty(GameEventLog.QA_LOG_PROPERTY, log.toString());
        Main.main(new String[0]);
        if (awaitFocusOwner() == null) {
            throw new QaException("focus was never gained; no keys were dispatched"
                    + " (needs a real desktop and an OS-focused window)");
        }
        for (int key : procedure.keys()) {
            press(key);
        }
        settle();
        return readLog(log);
    }

    private static Path newLogFile() {
        try {
            Path log = Files.createTempFile("veil-qa-", ".jsonl");
            log.toFile().deleteOnExit();
            return log;
        } catch (IOException e) {
            throw new QaException("Cannot create a temp event log: " + e.getMessage());
        }
    }

    private static Component awaitFocusOwner() {
        for (int i = 0; i < FOCUS_ATTEMPTS; i++) {
            Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            if (owner != null) {
                return owner;
            }
            sleep(POLL_MS);
        }
        return null;
    }

    // The focus owner is re-read for every key: pressing ENTER on "New" moves focus to the game.
    private static void press(int keyCode) {
        Component owner = awaitFocusOwner();
        if (owner == null) {
            throw new QaException("focus was lost before key " + KeyEvent.getKeyText(keyCode) + " was dispatched");
        }
        onEdt(() -> {
            owner.dispatchEvent(keyEvent(owner, KeyEvent.KEY_PRESSED, keyCode));
            owner.dispatchEvent(keyEvent(owner, KeyEvent.KEY_RELEASED, keyCode));
        });
        settle();
    }

    private static KeyEvent keyEvent(Component source, int id, int keyCode) {
        return new KeyEvent(source, id, System.currentTimeMillis(), 0, keyCode, KeyEvent.CHAR_UNDEFINED);
    }

    // Drain the EDT, give timers/invokeLater chains a moment, drain again, so keys don't merge.
    private static void settle() {
        onEdt(() -> { });
        sleep(SETTLE_MS);
        onEdt(() -> { });
    }

    private static void onEdt(Runnable action) {
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (InvocationTargetException e) {
            throw new QaException("Error on the event thread: " + e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new QaException("Interrupted");
        }
    }

    private static List<JsonObject> readLog(Path log) {
        try {
            List<JsonObject> events = new ArrayList<>();
            for (String line : Files.readAllLines(log)) {
                if (!line.isBlank()) {
                    events.add(JsonParser.parseString(line).getAsJsonObject());
                }
            }
            return events;
        } catch (IOException e) {
            throw new QaException("Cannot read the event log " + log + ": " + e.getMessage());
        }
    }

    private static void disposeAllWindows() {
        for (Window window : Window.getWindows()) {
            window.dispose();
        }
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new QaException("Interrupted");
        }
    }
}
