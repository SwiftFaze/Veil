package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.ui.widget.TranscriptWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Parses one typed dev-console command line and executes it: `search <term>` filters entries
 * and writes the results into the transcript; `edit <namespace:id>` resolves an entry and hands
 * it to the caller to open. Anything else writes a specific error line instead of running
 * anything - an unrecognized verb ("Unknown command: ..."), a verb missing its required
 * argument ("Usage: ..."), or an `edit` id that resolves to no entry ("No entry found for id: ...").
 */
public class DevConsoleCommandRunner {

    private static final String SEARCH_VERB = "search";
    private static final String EDIT_VERB = "edit";
    private static final List<String> RESULT_HEADERS = List.of("#", "ID", "Name", "Category", "Mod");

    private final DevConsoleModel model;
    private final TranscriptWidget transcript;
    private final Consumer<DevConsoleModel.SearchResult> onEditResolved;

    public DevConsoleCommandRunner(DevConsoleModel model, TranscriptWidget transcript,
                                    Consumer<DevConsoleModel.SearchResult> onEditResolved) {
        this.model = model;
        this.transcript = transcript;
        this.onEditResolved = onEditResolved;
    }

    public void run(String commandLine) {
        String trimmed = commandLine.trim();
        String verb = verbOf(trimmed);
        String argument = argumentOf(trimmed);

        switch (verb) {
            case SEARCH_VERB -> runSearch(argument);
            case EDIT_VERB -> runEdit(argument);
            default -> transcript.appendError("Unknown command: " + trimmed);
        }
    }

    private void runSearch(String term) {
        if (term.isBlank()) {
            transcript.appendError("Usage: search <term>");
            return;
        }
        model.setSearchText(term);
        List<DevConsoleModel.SearchResult> results = model.filteredResults();
        transcript.appendInfo(results.size() + " Results found for " + term);
        if (!results.isEmpty()) {
            transcript.appendResultTable(RESULT_HEADERS, resultRows(results));
        }
    }

    private void runEdit(String id) {
        if (id.isBlank()) {
            transcript.appendError("Usage: edit <namespace:id>");
            return;
        }
        Optional<DevConsoleModel.SearchResult> found = model.findById(id);
        if (found.isPresent()) {
            onEditResolved.accept(found.get());
        } else {
            transcript.appendError("No entry found for id: " + id);
        }
    }

    private List<List<String>> resultRows(List<DevConsoleModel.SearchResult> results) {
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            rows.add(rowFor(results.get(i), i + 1));
        }
        return rows;
    }

    private List<String> rowFor(DevConsoleModel.SearchResult result, int rowNumber) {
        return List.of(String.valueOf(rowNumber), result.entry().id(), result.entry().name(),
                result.entry().category(), result.entry().namespace());
    }

    private String verbOf(String trimmed) {
        int spaceIndex = trimmed.indexOf(' ');
        return spaceIndex < 0 ? trimmed : trimmed.substring(0, spaceIndex);
    }

    private String argumentOf(String trimmed) {
        int spaceIndex = trimmed.indexOf(' ');
        return spaceIndex < 0 ? "" : trimmed.substring(spaceIndex + 1).trim();
    }
}
