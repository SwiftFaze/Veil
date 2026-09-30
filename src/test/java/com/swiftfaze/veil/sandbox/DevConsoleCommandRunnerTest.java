package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.ui.widget.TranscriptWidget;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevConsoleCommandRunnerTest {

    @Test
    void snapshotSuccessWritesSUCCESSLine() {
        TranscriptWidget transcript = new TranscriptWidget();
        TestSnapshotter snapshotter = new TestSnapshotter();
        DevConsoleProvider provider = providerWithSnapshotter("core:player", "player", snapshotter);
        DevConsoleModel model = new DevConsoleModel(List.of(provider));
        DevConsoleCommandRunner runner = new DevConsoleCommandRunner(model, transcript, (r) -> {});

        runner.run("snapshot player boss");

        var lastEntry = transcript.entries().get(transcript.entries().size() - 1);
        assertEquals(TranscriptWidget.Level.SUCCESS, lastEntry.level());
        assertTrue(lastEntry.text().contains("Snapshot boss saved"));
    }

    @Test
    void restoreSuccessWritesSUCCESSLine() {
        TranscriptWidget transcript = new TranscriptWidget();
        TestSnapshotter snapshotter = new TestSnapshotter();
        DevConsoleProvider provider = providerWithSnapshotter("core:player", "player", snapshotter);
        DevConsoleModel model = new DevConsoleModel(List.of(provider));
        DevConsoleCommandRunner runner = new DevConsoleCommandRunner(model, transcript, (r) -> {});

        runner.run("snapshot player boss");
        int snapshotEntryCount = transcript.entries().size();
        runner.run("restore player boss");

        // The last entry should be the SUCCESS line from restore
        var lastEntry = transcript.entries().get(transcript.entries().size() - 1);
        assertEquals(TranscriptWidget.Level.SUCCESS, lastEntry.level());
        assertTrue(lastEntry.text().contains("Snapshot boss restored"));
    }

    @Test
    void snapshotWithoutNameWritesUsageError() {
        TranscriptWidget transcript = new TranscriptWidget();
        DevConsoleModel model = new DevConsoleModel(List.of());
        DevConsoleCommandRunner runner = new DevConsoleCommandRunner(model, transcript, (r) -> {});

        runner.run("snapshot player");

        var lastEntry = transcript.entries().get(transcript.entries().size() - 1);
        assertEquals(TranscriptWidget.Level.ERROR, lastEntry.level());
        assertEquals("Usage: snapshot <entry> <name>", lastEntry.text());
    }

    @Test
    void restoreWithoutNameWritesUsageError() {
        TranscriptWidget transcript = new TranscriptWidget();
        DevConsoleModel model = new DevConsoleModel(List.of());
        DevConsoleCommandRunner runner = new DevConsoleCommandRunner(model, transcript, (r) -> {});

        runner.run("restore player");

        var lastEntry = transcript.entries().get(transcript.entries().size() - 1);
        assertEquals(TranscriptWidget.Level.ERROR, lastEntry.level());
        assertEquals("Usage: restore <entry> <name>", lastEntry.text());
    }

    @Test
    void snapshotWithoutEntryWritesUsageError() {
        TranscriptWidget transcript = new TranscriptWidget();
        DevConsoleModel model = new DevConsoleModel(List.of());
        DevConsoleCommandRunner runner = new DevConsoleCommandRunner(model, transcript, (r) -> {});

        runner.run("snapshot");

        var lastEntry = transcript.entries().get(transcript.entries().size() - 1);
        assertEquals(TranscriptWidget.Level.ERROR, lastEntry.level());
        assertEquals("Usage: snapshot <entry> <name>", lastEntry.text());
    }

    @Test
    void snapshotWithUnknownEntryWritesError() {
        TranscriptWidget transcript = new TranscriptWidget();
        DevConsoleModel model = new DevConsoleModel(List.of());
        DevConsoleCommandRunner runner = new DevConsoleCommandRunner(model, transcript, (r) -> {});

        runner.run("snapshot nosuchentry boss");

        var lastEntry = transcript.entries().get(transcript.entries().size() - 1);
        assertEquals(TranscriptWidget.Level.ERROR, lastEntry.level());
        assertTrue(lastEntry.text().contains("No entry found for id: nosuchentry"));
    }

    @Test
    void snapshotWithEntryWithoutSnapshotSupportWritesError() {
        TranscriptWidget transcript = new TranscriptWidget();
        DevConsoleProvider noSnapshotter = new DevConsoleProvider() {
            @Override
            public List<DevConsoleEntry> entries() {
                return List.of(new DevConsoleEntry("core", "core:warrior", "Classes", "warrior"));
            }

            @Override
            public JComponent createPanel(String id) {
                return new JPanel();
            }
        };
        DevConsoleModel model = new DevConsoleModel(List.of(noSnapshotter));
        DevConsoleCommandRunner runner = new DevConsoleCommandRunner(model, transcript, (r) -> {});

        runner.run("snapshot warrior boss");

        var lastEntry = transcript.entries().get(transcript.entries().size() - 1);
        assertEquals(TranscriptWidget.Level.ERROR, lastEntry.level());
        assertEquals("Entry does not support snapshots: warrior", lastEntry.text());
    }

    @Test
    void restoreWithUnknownSnapshotWritesError() {
        TranscriptWidget transcript = new TranscriptWidget();
        TestSnapshotter snapshotter = new TestSnapshotter();
        DevConsoleProvider provider = providerWithSnapshotter("core:player", "player", snapshotter);
        DevConsoleModel model = new DevConsoleModel(List.of(provider));
        DevConsoleCommandRunner runner = new DevConsoleCommandRunner(model, transcript, (r) -> {});

        runner.run("restore player nosuchslot");

        var lastEntry = transcript.entries().get(transcript.entries().size() - 1);
        assertEquals(TranscriptWidget.Level.ERROR, lastEntry.level());
        assertEquals("No snapshot named: nosuchslot", lastEntry.text());
    }

    private DevConsoleProvider providerWithSnapshotter(String id, String localName, DevConsoleSnapshotter snapshotter) {
        return new DevConsoleProvider() {
            @Override
            public List<DevConsoleEntry> entries() {
                return List.of(new DevConsoleEntry("core", id, "Player", localName));
            }

            @Override
            public JComponent createPanel(String entryId) {
                return new JPanel();
            }

            @Override
            public Optional<DevConsoleSnapshotter> snapshotter(String entryId) {
                return Optional.of(snapshotter);
            }
        };
    }

    private static class TestSnapshotter implements DevConsoleSnapshotter {
        private java.util.Map<String, String> snapshots = new java.util.HashMap<>();

        @Override
        public String takeSnapshot(String name) {
            snapshots.put(name, "snapshot_" + name);
            return "Snapshot " + name + " saved";
        }

        @Override
        public Optional<String> restoreSnapshot(String name) {
            if (snapshots.containsKey(name)) {
                return Optional.of("Snapshot " + name + " restored");
            }
            return Optional.empty();
        }
    }
}
