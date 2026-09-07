package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.ui.widget.TranscriptWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Parses one typed dev-console command line and executes it: `search <term>` filters entries
 * and writes the results into the transcript; `edit <namespace:id>` resolves an entry and hands
 * it to the caller to open. `set`/`add`/`subtract <entry> <field> <value>` mutate a live entity's
 * field via {@link DevConsoleFieldMutator} and write a SUCCESS/ERROR transcript line - only
 * entities whose provider returns one from {@link DevConsoleProvider#fieldMutator} support this.
 * Anything else writes a specific error line instead of running anything - an unrecognized verb
 * ("Unknown command: ..."), a verb missing its required argument ("Usage: ..."), or an `edit` id
 * that resolves to no entry ("No entry found for id: ...").
 */
public class DevConsoleCommandRunner {

    private static final String SEARCH_VERB = "search";
    private static final String EDIT_VERB = "edit";
    private static final String SET_VERB = "set";
    private static final String ADD_VERB = "add";
    private static final String SUBTRACT_VERB = "subtract";
    private static final int MUTATION_MIN_PARTS = 3;
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
            case SET_VERB -> runMutation(DevConsoleMutationVerb.SET, argument);
            case ADD_VERB -> runMutation(DevConsoleMutationVerb.ADD, argument);
            case SUBTRACT_VERB -> runMutation(DevConsoleMutationVerb.SUBTRACT, argument);
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

    private void runMutation(DevConsoleMutationVerb verb, String argument) {
        String[] parts = argument.split("\\s+", MUTATION_MIN_PARTS);
        if (parts.length < MUTATION_MIN_PARTS) {
            transcript.appendError("Usage: " + verb.name().toLowerCase(java.util.Locale.ROOT) + " <entry> <field> <value>");
            return;
        }
        Optional<DevConsoleModel.SearchResult> found = model.findByEntryToken(parts[0]);
        if (found.isEmpty()) {
            transcript.appendError("No entry found for id: " + parts[0]);
            return;
        }
        Optional<DevConsoleFieldMutator> mutator = found.get().provider().fieldMutator(found.get().entry().id());
        if (mutator.isEmpty()) {
            transcript.appendError("Entry does not support field mutation: " + parts[0]);
            return;
        }
        applyMutation(mutator.get(), verb, parts[1], parts[2]);
    }

    private void applyMutation(DevConsoleFieldMutator mutator, DevConsoleMutationVerb verb, String field, String value) {
        DevConsoleMutationResult result = mutator.apply(verb, field, value);
        if (result instanceof DevConsoleMutationResult.Success success) {
            transcript.appendSuccess(success.fieldName() + " set to " + success.newValue());
        } else if (result instanceof DevConsoleMutationResult.Failure failure) {
            transcript.appendError("Invalid input: " + failure.token());
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
