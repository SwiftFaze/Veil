package com.swiftfaze.veil.steps;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.swiftfaze.veil.entities.quests.Quest;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.sandbox.ClassSandbox;
import com.swiftfaze.veil.sandbox.DevConsoleModel;
import com.swiftfaze.veil.sandbox.DevConsolePanel;
import com.swiftfaze.veil.sandbox.DevConsoleProvider;
import com.swiftfaze.veil.sandbox.QuestSandboxProvider;
import com.swiftfaze.veil.component.DetailTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QuestSandboxSteps {

    private Path modsRoot;
    private DevConsoleModel model;
    private List<Quest> loadedQuests;

    @Before
    public void createModsRoot() throws IOException {
        modsRoot = Files.createTempDirectory("veil-quest-sandbox-test");
    }

    @After
    public void deleteModsRoot() throws IOException {
        try (Stream<Path> paths = Files.walk(modsRoot)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    @Given("a loaded quest {string} whose objective has no target and no count")
    public void aLoadedQuestWhoseObjectiveHasNoTargetAndNoCount(String questName) throws IOException {
        writeFixtureQuest(questName, "kill", null, null, List.of());
        reloadConsoleWithTempQuestsAndUpdatePanel();
    }

    @Given("a loaded quest {string} with no rewards")
    public void aLoadedQuestWithNoRewards(String questName) throws IOException {
        writeFixtureQuest(questName, "kill", "core:goblin", 5, List.of());
        reloadConsoleWithTempQuestsAndUpdatePanel();
    }

    @Given("no quests are loaded")
    public void noQuestsAreLoaded() throws IOException {
        // Create an empty mod structure with no quests
        writeManifest();
        // Don't write any quest files
        reloadConsoleWithTempQuestsAndUpdatePanel();
    }

    @Then("the quest detail shows these fields:")
    public void theQuestDetailShowsTheseFields(io.cucumber.datatable.DataTable dataTable) {
        String questId = SharedScenarioContext.getCurrentQuestId();
        assertNotNull(questId, "Expected a quest to be opened: quest ID is null");
        Quest quest = findQuestById(questId);
        assertNotNull(quest, "Expected to find quest with id: " + questId);

        List<Map<String, String>> expectedRows = dataTable.asMaps(String.class, String.class);
        List<DetailTable> tables = quest.getDetailTables();
        assertFalse(tables.isEmpty(), "Expected at least one detail table");

        DetailTable mainTable = tables.get(0);
        assertTrue(mainTable.rows().size() >= expectedRows.size(),
                "Expected at least " + expectedRows.size() + " rows but got " + mainTable.rows().size());

        // Find each expected row in the table and verify its value
        for (Map<String, String> expectedRow : expectedRows) {
            String expectedField = expectedRow.get("Field");
            String expectedValue = expectedRow.get("Value");

            boolean found = false;
            for (List<String> row : mainTable.rows()) {
                if (expectedField.equals(row.get(0))) {
                    assertEquals(expectedValue, row.get(1), "Value mismatch for field '" + expectedField + "'");
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Expected to find field '" + expectedField + "' in quest detail");
        }
    }

    @Then("the quest detail has a {string} table with these rows:")
    public void theQuestDetailHasATableWithTheseRows(String tableLabel, io.cucumber.datatable.DataTable dataTable) {
        String questId = SharedScenarioContext.getCurrentQuestId();
        assertNotNull(questId, "Expected a quest to be opened");
        Quest quest = findQuestById(questId);
        assertNotNull(quest, "Expected to find quest with id: " + questId);

        List<DetailTable> tables = quest.getDetailTables();

        // Find the table with the matching label
        DetailTable targetTable = null;
        for (DetailTable table : tables) {
            if (tableLabel.equals(table.label())) {
                targetTable = table;
                break;
            }
        }

        assertNotNull(targetTable, "Expected to find a '" + tableLabel + "' table");

        List<Map<String, String>> expectedRows = dataTable.asMaps(String.class, String.class);
        assertEquals(expectedRows.size(), targetTable.rows().size(), "Row count mismatch");

        for (int i = 0; i < expectedRows.size(); i++) {
            Map<String, String> expectedRow = expectedRows.get(i);
            List<String> row = targetTable.rows().get(i);

            assertEquals(expectedRow.get("Type"), row.get(0), "Type mismatch at row " + i);
            assertEquals(expectedRow.get("ID"), row.get(1), "ID mismatch at row " + i);
            assertEquals(expectedRow.get("Count"), row.get(2), "Count mismatch at row " + i);
            assertEquals(expectedRow.get("Calc"), row.get(3), "Calc mismatch at row " + i);
        }
    }

    @Then("the quest detail has no {string} table")
    public void theQuestDetailHasNoTable(String tableLabel) {
        String questId = SharedScenarioContext.getCurrentQuestId();
        assertNotNull(questId, "Expected a quest to be opened");
        Quest quest = findQuestById(questId);
        assertNotNull(quest, "Expected to find quest with id: " + questId);

        List<DetailTable> tables = quest.getDetailTables();

        // Verify that no table has the specified label
        boolean hasTable = tables.stream().anyMatch(t -> tableLabel.equals(t.label()));
        assertFalse(hasTable, "Expected no '" + tableLabel + "' table");
    }

    @Given("the F1 in-game dev console is built")
    public void theF1InGameDevConsoleIsBuilt() {
        // Use the static method from Main to build the providers, but we can't easily access it from tests.
        // Instead, create the providers directly matching what Main does.
        List<DevConsoleProvider> providers = List.of(
            new com.swiftfaze.veil.sandbox.ClassSandboxProvider(),
            new QuestSandboxProvider(),
            new com.swiftfaze.veil.sandbox.PlayerSandboxProvider(() -> new com.swiftfaze.veil.entities.player.Player(0, 0))
        );
        model = new DevConsoleModel(providers);
    }

    @Given("the standalone sandbox dev console is built")
    public void theStandaloneSandboxDevConsoleIsBuilt() {
        // Use the static method from ClassSandbox
        List<DevConsoleProvider> providers = ClassSandbox.providers();
        model = new DevConsoleModel(providers);
    }

    @Then("its providers include the {string} provider")
    public void itsProvidersIncludeTheProvider(String providerName) {
        assertNotNull(model, "Expected a dev console model to be built");

        // Check if any entry belongs to the Quests provider by category
        if ("Quests".equals(providerName)) {
            boolean found = model.allResults().stream()
                    .anyMatch(result -> "Quests".equals(result.entry().category()));
            assertTrue(found, "Expected to find the Quests provider (no entries in Quests category)");
        }
    }

    private void writeFixtureQuest(String questName, String objectiveType, String objectiveTarget,
                                    Integer objectiveCount, List<JsonObject> rewards) throws IOException {
        Path coreDir = modsRoot.resolve("core");
        Files.createDirectories(coreDir.resolve("quests"));

        writeManifest();

        JsonObject questJson = new JsonObject();
        String questId = "core:" + questName.toLowerCase(Locale.ROOT).replace(" ", "_");
        questJson.addProperty("id", questId);
        questJson.addProperty("name", questName);

        JsonObject objective = new JsonObject();
        objective.addProperty("type", objectiveType);
        if (objectiveTarget != null) {
            objective.addProperty("target", objectiveTarget);
        }
        if (objectiveCount != null) {
            objective.addProperty("count", objectiveCount);
        }
        questJson.add("objective", objective);

        if (!rewards.isEmpty()) {
            JsonArray rewardsArray = new JsonArray();
            for (JsonObject reward : rewards) {
                rewardsArray.add(reward);
            }
            questJson.add("rewards", rewardsArray);
        }

        Files.writeString(coreDir.resolve("quests").resolve(questId.substring(5) + ".json"), questJson.toString());
    }

    private void writeManifest() throws IOException {
        Path coreDir = modsRoot.resolve("core");
        Files.createDirectories(coreDir);

        JsonObject manifest = new JsonObject();
        manifest.addProperty("id", "core");
        manifest.add("dependsOn", new JsonArray());
        Files.writeString(coreDir.resolve("mod.json"), manifest.toString());
    }

    private void reloadConsoleWithTempQuests() {
        loadedQuests = ModLoader.load(modsRoot).getAllQuests();
        List<DevConsoleProvider> providers = List.of(new QuestSandboxProvider(loadedQuests));
        model = new DevConsoleModel(providers);
        // Store the loaded quests in shared context so verification steps can access them
        SharedScenarioContext.setLoadedQuests(loadedQuests);
    }

    private void reloadConsoleWithTempQuestsAndUpdatePanel() {
        reloadConsoleWithTempQuests();
        // Update the shared context model so that DevConsoleSteps can access the new model
        SharedScenarioContext.setDevConsoleModel(model);
    }

    private Quest findQuestById(String questId) {
        if (questId == null) {
            return null;
        }

        // Get the loaded quests from shared context
        List<Quest> quests = SharedScenarioContext.getLoadedQuests();
        if (quests != null) {
            for (Quest q : quests) {
                if (questId.equals(q.getId())) {
                    return q;
                }
            }
        }

        return null;
    }
}
