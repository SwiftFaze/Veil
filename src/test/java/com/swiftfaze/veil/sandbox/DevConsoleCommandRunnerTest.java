package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.exceptions.ModLoadException;
import com.swiftfaze.veil.ui.widget.TranscriptWidget;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevConsoleCommandRunnerTest {

    @Test
    void reloadCommandRebuildsResults() {
        FakeTranscript transcript = new FakeTranscript();
        MutableProvider provider = new MutableProvider("Classes", "Mage");
        DevConsoleModel model = new DevConsoleModel(List.of(provider));
        DevConsoleCommandRunner runner = new DevConsoleCommandRunner(model, transcript, result -> {});

        provider.addEntry(new DevConsoleEntry("core", "core:rogue", "Classes", "Rogue"));
        runner.run("reload");

        String lastSuccess = transcript.lastSuccessText();
        assertTrue(lastSuccess.contains("Reloaded mods: 2 entries"));
    }

    @Test
    void reloadWithArgumentsIsRejected() {
        FakeTranscript transcript = new FakeTranscript();
        MutableProvider provider = new MutableProvider("Classes", "Mage");
        DevConsoleModel model = new DevConsoleModel(List.of(provider));
        DevConsoleCommandRunner runner = new DevConsoleCommandRunner(model, transcript, result -> {});

        runner.run("reload now");

        String lastError = transcript.lastErrorText();
        assertTrue(lastError.contains("Usage: reload"));
        assertEquals(0, provider.reloadCallCount());
    }

    @Test
    void reloadOnExceptionWritesErrorMessage() {
        FakeTranscript transcript = new FakeTranscript();
        MutableProvider provider = new MutableProvider("Classes", "Mage");
        DevConsoleModel model = new DevConsoleModel(List.of(provider));
        DevConsoleCommandRunner runner = new DevConsoleCommandRunner(model, transcript, result -> {});

        provider.setNextReloadFails("Failed to load tile from file: bad.json");
        runner.run("reload");

        String lastError = transcript.lastErrorText();
        assertTrue(lastError.contains("Reload failed: Failed to load tile from file: bad.json"));
        assertEquals(1, model.allResults().size());
    }

    static class MutableProvider implements DevConsoleProvider {
        private final String category;
        private final List<DevConsoleEntry> entries = new ArrayList<>();
        private int reloadCount = 0;
        private String nextReloadError = null;

        MutableProvider(String category, String... names) {
            this.category = category;
            for (String name : names) {
                this.entries.add(new DevConsoleEntry("core", "core:" + name.toLowerCase(java.util.Locale.ROOT), category, name));
            }
        }

        @Override
        public List<DevConsoleEntry> entries() {
            return new ArrayList<>(entries);
        }

        @Override
        public JComponent createPanel(String id) {
            return new JPanel();
        }

        @Override
        public void reload() {
            if (nextReloadError != null) {
                String error = nextReloadError;
                nextReloadError = null;
                throw new ModLoadException(error);
            }
            reloadCount++;
        }

        void addEntry(DevConsoleEntry entry) {
            entries.add(entry);
        }

        int reloadCallCount() {
            return reloadCount;
        }

        void setNextReloadFails(String errorMessage) {
            this.nextReloadError = errorMessage;
        }
    }

    static class FakeTranscript extends TranscriptWidget {
        String lastSuccessText() {
            for (int i = entries().size() - 1; i >= 0; i--) {
                TranscriptWidget.TranscriptEntry entry = entries().get(i);
                if (entry.level() == TranscriptWidget.Level.SUCCESS) {
                    return entry.text();
                }
            }
            return "";
        }

        String lastErrorText() {
            for (int i = entries().size() - 1; i >= 0; i--) {
                TranscriptWidget.TranscriptEntry entry = entries().get(i);
                if (entry.level() == TranscriptWidget.Level.ERROR) {
                    return entry.text();
                }
            }
            return "";
        }
    }
}
