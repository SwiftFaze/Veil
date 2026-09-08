package com.swiftfaze.veil.sandbox;

import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevConsoleCompletionTest {

    private static final String PLAYER_ID = "core:player";
    private static final String PLAYER_NAME = "player";
    private static final String STR_TOKEN = "str";

    @Test
    void mutationEntryCandidatesExcludeEntriesWithoutAFieldMutator() {
        DevConsoleModel model = new DevConsoleModel(List.of(
                mutatorProvider(PLAYER_ID, PLAYER_NAME),
                plainProvider("core:pete", "pete")
        ));
        DevConsoleCompletion completion = new DevConsoleCompletion(model);

        List<String> candidates = completion.candidates("set p");
        String filled = completion.apply("set p", candidates.get(0));

        assertEquals(List.of(PLAYER_NAME), candidates);
        assertEquals("set player", filled);
    }

    @Test
    void mutationEntryCandidatesExcludeNonMatchingPrefixes() {
        DevConsoleModel model = new DevConsoleModel(List.of(mutatorProvider(PLAYER_ID, PLAYER_NAME)));
        DevConsoleCompletion completion = new DevConsoleCompletion(model);

        List<String> candidates = completion.candidates("set z");

        assertTrue(candidates.isEmpty());
    }

    @Test
    void mutationEntryLocalIdFallsBackToTheWholeIdWhenThereIsNoNamespaceColon() {
        DevConsoleModel model = new DevConsoleModel(List.of(mutatorProvider("standalone", "Standalone")));
        DevConsoleCompletion completion = new DevConsoleCompletion(model);

        List<String> candidates = completion.candidates("set stand");

        assertEquals(List.of("standalone"), candidates);
    }

    @Test
    void trailingSpaceStartsANewEmptyPrefixArgumentRatherThanReSuggestingTheJustTypedWord() {
        DevConsoleModel model = new DevConsoleModel(List.of(
                mutatorProviderWithFields(PLAYER_ID, PLAYER_NAME, List.of(STR_TOKEN, "dex"))
        ));
        DevConsoleCompletion completion = new DevConsoleCompletion(model);

        List<String> candidates = completion.candidates("set player ");

        assertEquals(List.of("dex", STR_TOKEN), candidates, "Expected every field token, not a re-suggestion of \"player\"");
    }

    @Test
    void acceptingAfterATrailingSpaceAppendsTheCandidateAsANewToken() {
        DevConsoleModel model = new DevConsoleModel(List.of(
                mutatorProviderWithFields(PLAYER_ID, PLAYER_NAME, List.of(STR_TOKEN))
        ));
        DevConsoleCompletion completion = new DevConsoleCompletion(model);

        String filled = completion.apply("set player ", STR_TOKEN);

        assertEquals("set player str", filled);
    }

    private DevConsoleProvider mutatorProviderWithFields(String id, String localName, List<String> fieldTokens) {
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
            public Optional<DevConsoleFieldMutator> fieldMutator(String entryId) {
                return Optional.of(new DevConsoleFieldMutator() {
                    @Override
                    public DevConsoleMutationResult apply(DevConsoleMutationVerb verb, String fieldToken, String rawValue) {
                        return new DevConsoleMutationResult.Failure(fieldToken);
                    }

                    @Override
                    public List<String> fieldTokens() {
                        return fieldTokens;
                    }

                    @Override
                    public boolean hasClassDefault(String fieldToken) {
                        return false;
                    }
                });
            }
        };
    }

    private DevConsoleProvider mutatorProvider(String id, String localName) {
        return mutatorProviderWithFields(id, localName, List.of());
    }

    private DevConsoleProvider plainProvider(String id, String localName) {
        return new DevConsoleProvider() {
            @Override
            public List<DevConsoleEntry> entries() {
                return List.of(new DevConsoleEntry("core", id, "Classes", localName));
            }

            @Override
            public JComponent createPanel(String entryId) {
                return new JPanel();
            }
        };
    }
}
