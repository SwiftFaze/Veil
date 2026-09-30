package com.swiftfaze.veil.steps;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.sandbox.ClassSandbox;
import com.swiftfaze.veil.sandbox.DevConsoleModel;
import com.swiftfaze.veil.sandbox.DevConsolePanel;
import com.swiftfaze.veil.sandbox.DevConsoleProvider;
import com.swiftfaze.veil.sandbox.QuestSandboxProvider;
import com.swiftfaze.veil.ui.DetailsPaneWidget;
import com.swiftfaze.veil.ui.widget.TableWidget;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QuestSandboxSteps {

    private Path modsRoot;
    private DevConsoleModel model;

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
        DevConsolePanel panel = SharedScenarioContext.getDevConsolePanel();
        assertNotNull(panel, "Expected a dev console panel");

        DetailsPaneWidget detailsPane = assertInstanceOf(DetailsPaneWidget.class,
                panel.getOpenedProviderPanel(),
                "Expected the opened panel to be a DetailsPaneWidget");

        assertFalse(detailsPane.isShowingPlaceholder(), "Expected quest details to be shown, not a placeholder");

        List<Map<String, String>> expectedRows = dataTable.asMaps(String.class, String.class);

        // Get the first table (the main fields table)
        TableWidget<List<String>> table = detailsPane.getTable(0);
        assertNotNull(table, "Expected at least one table in the quest detail");

        List<List<String>> rows = table.getRows();

        // Find each expected row and verify its value
        for (Map<String, String> expectedRow : expectedRows) {
            String expectedField = expectedRow.get("Field");
            String expectedValue = expectedRow.get("Value");

            boolean found = false;
            for (List<String> row : rows) {
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
        DevConsolePanel panel = SharedScenarioContext.getDevConsolePanel();
        assertNotNull(panel, "Expected a dev console panel");

        DetailsPaneWidget detailsPane = assertInstanceOf(DetailsPaneWidget.class,
                panel.getOpenedProviderPanel(),
                "Expected the opened panel to be a DetailsPaneWidget");

        // The "Rewards:" table is the second table (index 1)
        assertEquals("Rewards:", tableLabel, "Currently only supporting Rewards table verification");
        assertTrue(detailsPane.getTableCount() > 1, "Expected at least 2 tables for rewards");

        TableWidget<List<String>> table = detailsPane.getTable(1);
        assertNotNull(table, "Expected a Rewards table at index 1");

        List<Map<String, String>> expectedRows = dataTable.asMaps(String.class, String.class);
        List<List<String>> rows = table.getRows();

        assertEquals(expectedRows.size(), rows.size(), "Row count mismatch in Rewards table");

        for (int i = 0; i < expectedRows.size(); i++) {
            Map<String, String> expectedRow = expectedRows.get(i);
            List<String> row = rows.get(i);

            assertEquals(expectedRow.get("Type"), row.get(0), "Type mismatch at row " + i);
            assertEquals(expectedRow.get("ID"), row.get(1), "ID mismatch at row " + i);
            assertEquals(expectedRow.get("Count"), row.get(2), "Count mismatch at row " + i);
            assertEquals(expectedRow.get("Calc"), row.get(3), "Calc mismatch at row " + i);
        }
    }

    @Then("the quest detail has no {string} table")
    public void theQuestDetailHasNoTable(String tableLabel) {
        DevConsolePanel panel = SharedScenarioContext.getDevConsolePanel();
        assertNotNull(panel, "Expected a dev console panel");

        DetailsPaneWidget detailsPane = assertInstanceOf(DetailsPaneWidget.class,
                panel.getOpenedProviderPanel(),
                "Expected the opened panel to be a DetailsPaneWidget");

        // For a quest with no rewards, there should only be 1 table (the main fields table)
        assertEquals(1, detailsPane.getTableCount(), "Expected only the main fields table when there are no rewards");
    }

    @Given("the F1 in-game dev console is built")
    public void theF1InGameDevConsoleIsBuilt() {
        // Use Main.buildDevConsoleProviders to get the correct provider list
        List<DevConsoleProvider> providers = com.swiftfaze.veil.Main.buildDevConsoleProviders(
                () -> new com.swiftfaze.veil.entities.player.Player(0, 0)
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

        // Check if any entry belongs to the specified provider by category
        boolean found = model.allResults().stream()
                .anyMatch(result -> providerName.equals(result.entry().category()));
        assertTrue(found, "Expected to find the " + providerName + " provider (no entries in " + providerName + " category)");
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

    private void reloadConsoleWithTempQuestsAndUpdatePanel() {
        List<com.swiftfaze.veil.entities.quests.Quest> quests = ModLoader.load(modsRoot).getAllQuests();
        List<DevConsoleProvider> providers = List.of(new QuestSandboxProvider(quests));
        model = new DevConsoleModel(providers);
        DevConsolePanel panel = new DevConsolePanel(model);
        // Update the shared context panel so that DevConsoleSteps and verification steps can access it
        SharedScenarioContext.setDevConsolePanel(panel);
        SharedScenarioContext.setDevConsoleModel(model);
    }
}
