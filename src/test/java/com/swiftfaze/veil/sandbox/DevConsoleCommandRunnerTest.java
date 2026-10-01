package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.ui.widget.TranscriptWidget;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevConsoleCommandRunnerTest {
    private static final String CLASSES = "Classes";


    @Test
    void reloadCommandRebuildsResults() {
        FakeTranscript transcript = new FakeTranscript();
        ReloadableFakeProvider provider = new ReloadableFakeProvider(CLASSES, "Mage");
        DevConsoleModel model = new DevConsoleModel(List.of(provider));
        DevConsoleCommandRunner runner = new DevConsoleCommandRunner(model, transcript, result -> {});

        provider.addEntry(new DevConsoleEntry("core", "core:rogue", CLASSES, "Rogue"));
        runner.run("reload");

        String lastSuccess = transcript.lastSuccessText();
        assertTrue(lastSuccess.contains("Reloaded mods: 2 entries"));
    }

    @Test
    void reloadWithArgumentsIsRejected() {
        FakeTranscript transcript = new FakeTranscript();
        ReloadableFakeProvider provider = new ReloadableFakeProvider(CLASSES, "Mage");
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
        ReloadableFakeProvider provider = new ReloadableFakeProvider(CLASSES, "Mage");
        DevConsoleModel model = new DevConsoleModel(List.of(provider));
        DevConsoleCommandRunner runner = new DevConsoleCommandRunner(model, transcript, result -> {});

        provider.setNextReloadFails("Failed to load tile from file: bad.json");
        runner.run("reload");

        String lastError = transcript.lastErrorText();
        assertTrue(lastError.contains("Reload failed: Failed to load tile from file: bad.json"));
        assertEquals(1, model.allResults().size());
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
