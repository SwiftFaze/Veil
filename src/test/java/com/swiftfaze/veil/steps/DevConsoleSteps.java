package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.entities.player.Stats;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.sandbox.ClassSandboxProvider;
import com.swiftfaze.veil.sandbox.DevConsoleModel;
import com.swiftfaze.veil.sandbox.DevConsolePanel;
import com.swiftfaze.veil.sandbox.DevConsoleProvider;
import com.swiftfaze.veil.sandbox.PlayerDetailPanel;
import com.swiftfaze.veil.sandbox.PlayerSandboxProvider;
import com.swiftfaze.veil.ui.widget.TableWidget;
import com.swiftfaze.veil.ui.widget.TranscriptWidget;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DevConsoleSteps {

    // Column order matches DevConsoleCommandRunner.RESULT_HEADERS: "#", "ID", "Name", "Category", "Mod".
    private static final int ID_COLUMN = 1;
    private static final int NAME_COLUMN = 2;
    private static final int CATEGORY_COLUMN = 3;
    private static final int MOD_COLUMN = 4;
    private static final String TRANSCRIPT_SHOULD_HAVE_ENTRIES = "Transcript should have entries";
    private static final String CLASSES_PROVIDER_NAME = "Classes";
    private static final String PLAYER_PROVIDER_NAME = "Player";

    private DevConsoleModel model;
    private DevConsolePanel panel;
    private Player livePlayer;
    private PlayerDetailPanel playerDetailPanel;
    private Player identityBeforeEdit;
    private int maxHpBeforeEdit;
    private String lastEditedFieldName;
    private int lastEditedFieldValue;
    private Stats statsSnapshot;
    private String classSnapshot;

    @Given("the dev console is running with the {string} provider registered")
    public void theDevConsoleIsRunningWithTheProviderRegistered(String providerName) {
        List<DevConsoleProvider> providers = List.of(providerFor(providerName));
        model = new DevConsoleModel(providers);
        panel = new DevConsolePanel(model);
    }

    @Given("the dev console is running with the {string} and {string} providers registered")
    public void theDevConsoleIsRunningWithTheProvidersRegistered(String provider1, String provider2) {
        livePlayer = new Player(0, 0);
        List<DevConsoleProvider> providers = List.of(providerFor(provider1), providerFor(provider2));
        model = new DevConsoleModel(providers);
        panel = new DevConsolePanel(model);
    }

    @Given("the dev console is running with the {string} provider attached to the running player")
    public void theDevConsoleIsRunningWithThePlayerProviderAttached(String providerName) {
        livePlayer = new Player(0, 0);
        List<DevConsoleProvider> providers = List.of(new PlayerSandboxProvider(() -> livePlayer));
        model = new DevConsoleModel(providers);
        panel = new DevConsolePanel(model);
    }

    @When("the search text is set to {string}")
    public void theSearchTextIsSetTo(String text) {
        model.setSearchText(text);
    }

    @When("the command {string} is entered")
    public void theCommandIsEntered(String command) {
        panel.getSearchField().setText(command);
        panel.runCommand();
    }

    @When("the command {string} has been entered")
    public void theCommandHasBeenEntered(String command) {
        theCommandIsEntered(command);
    }

    @Given("the command field contains {string}")
    public void theCommandFieldContains(String text) {
        panel.getSearchField().setText(text);
    }

    @Then("the command field text is {string}")
    public void theCommandFieldTextIs(String text) {
        assertEquals(text, panel.getSearchField().getText(),
                "Expected command field to contain: " + text);
    }

    @When("Tab is pressed")
    public void tabIsPressed() {
        fireCommandAction(Keybindings.ACTION_DEV_CONSOLE_COMPLETE);
    }

    @When("Up is pressed")
    public void upIsPressed() {
        fireCommandAction(Keybindings.ACTION_DEV_CONSOLE_HISTORY_UP);
    }

    @When("Down is pressed")
    public void downIsPressed() {
        fireCommandAction(Keybindings.ACTION_DEV_CONSOLE_HISTORY_DOWN);
    }

    @When("Enter is pressed")
    public void enterIsPressed() {
        fireCommandAction(Keybindings.ACTION_MENU_CONFIRM);
    }

    @When("Escape is pressed")
    public void escapeIsPressed() {
        fireCommandAction(Keybindings.ACTION_DEV_CONSOLE_DISMISS_OVERLAY);
    }

    @Then("the suggestion overlay is showing with candidates {string}")
    public void theSuggestionOverlayIsShowingWithCandidates(String candidatesStr) {
        assertTrue(panel.getSuggestionOverlay().isShowing(), "Expected the suggestion overlay to be showing");
        Set<String> expected = Set.of(candidatesStr.split(", "));
        Set<String> actual = Set.copyOf(panel.getSuggestionOverlay().candidates());
        assertEquals(expected, actual, "Expected candidates " + expected + " but got " + actual);
    }

    @Then("the suggestion overlay is not showing")
    public void theSuggestionOverlayIsNotShowing() {
        assertFalse(panel.getSuggestionOverlay().isShowing(), "Expected the suggestion overlay to not be showing");
    }

    @Then("the transcript has no entries")
    public void assertTranscriptHasNoEntries() {
        assertTrue(panel.getTranscript().entries().isEmpty(),
                "Expected transcript to have no entries");
    }

    @Then("the command history size is {int}")
    public void assertCommandHistorySize(int expectedSize) {
        assertEquals(expectedSize, panel.getHistory().size(),
                "Expected command history size to be " + expectedSize);
    }

    @Then("the transcript's last entry is an info line reporting {int} results for {string}")
    public void theTranscriptsLastEntryIsAnInfoLineReporting(int count, String term) {
        assertFalse(panel.getTranscript().entries().isEmpty(), TRANSCRIPT_SHOULD_HAVE_ENTRIES);
        var lastEntry = panel.getTranscript().entries().get(panel.getTranscript().entries().size() - 1);
        assertEquals(TranscriptWidget.Level.INFO, lastEntry.level());
        assertTrue(lastEntry.text().contains(String.valueOf(count)));
        assertTrue(lastEntry.text().contains(term));
    }

    @Then("the transcript's first entry is a command line for {string}")
    public void theTranscriptsFirstEntryIsACommandLineFor(String command) {
        assertFalse(panel.getTranscript().entries().isEmpty(), TRANSCRIPT_SHOULD_HAVE_ENTRIES);
        var firstEntry = panel.getTranscript().entries().get(0);
        assertEquals(TranscriptWidget.Level.COMMAND, firstEntry.level());
        assertEquals(command, firstEntry.text());
    }

    @Then("the transcript's last entry is an error line for {string}")
    public void theTranscriptsLastEntryIsAnErrorLineFor(String input) {
        assertFalse(panel.getTranscript().entries().isEmpty(), "Transcript should have entries after: " + input);
        var lastEntry = panel.getTranscript().entries().get(panel.getTranscript().entries().size() - 1);
        assertEquals(TranscriptWidget.Level.ERROR, lastEntry.level(), "Expected an error line after: " + input);
    }

    @Then("the transcript's most recent result table includes a row with id {string}, name {string}, category {string}, and mod {string}")
    public void theTranscriptsMostRecentResultTableIncludesARow(String id, String name, String category, String mod) {
        assertTrue(panel.getTranscript().lastResultTable().isPresent());
        var rows = panel.getTranscript().lastResultTable().get();
        boolean found = rows.stream().anyMatch(row ->
            row.size() > MOD_COLUMN &&
            id.equals(row.get(ID_COLUMN)) &&
            name.equals(row.get(NAME_COLUMN)) &&
            category.equals(row.get(CATEGORY_COLUMN)) &&
            mod.equals(row.get(MOD_COLUMN))
        );
        assertTrue(found, "No result table row found matching: " + id + ", " + name + ", " + category + ", " + mod);
    }

    @Then("the transcript has no result table")
    public void theTranscriptHasNoResultTable() {
        assertTrue(panel.getTranscript().lastResultTable().isEmpty());
    }

    @Then("the console view is shown")
    public void theConsoleViewIsShown() {
        // The console view (transcript) is shown when the provider panel is not visible
        assertFalse(panel.isProviderPanelShowing(), "Provider panel should not be showing");
    }

    @Then("the transcript still contains an info line reporting {int} results for {string}")
    public void theTranscriptStillContainsAnInfoLineReporting(int count, String term) {
        boolean found = panel.getTranscript().entries().stream().anyMatch(entry ->
            entry.level() == TranscriptWidget.Level.INFO &&
            entry.text().contains(String.valueOf(count)) &&
            entry.text().contains(term)
        );
        assertTrue(found);
    }

    @Then("the transcript contains an info line reporting {int} results for {string}")
    public void theTranscriptContainsAnInfoLineReporting(int count, String term) {
        theTranscriptStillContainsAnInfoLineReporting(count, term);
    }

    @Then("the transcript contains an error line for {string}")
    public void theTranscriptContainsAnErrorLineFor(String input) {
        boolean found = panel.getTranscript().entries().stream()
                .anyMatch(entry -> entry.level() == TranscriptWidget.Level.ERROR);
        assertTrue(found, "Expected an error line in the transcript after: " + input);
    }

    @Then("the results include an entry named {string}")
    public void theResultsIncludeAnEntryNamed(String name) {
        assertTrue(resultNames().contains(name));
    }

    @Then("the results do not include an entry named {string}")
    public void theResultsDoNotIncludeAnEntryNamed(String name) {
        assertFalse(resultNames().contains(name));
    }

    @Then("the results are empty")
    public void theResultsAreEmpty() {
        assertTrue(model.filteredResults().isEmpty());
    }

    @Then("the {string} result has namespace {string} and category {string}")
    public void theResultHasNamespaceAndCategory(String name, String namespace, String category) {
        DevConsoleModel.SearchResult result = findResult(name);
        assertEquals(namespace, result.entry().namespace());
        assertEquals(category, result.entry().category());
    }

    @When("{string} is opened")
    public void isOpened(String entryName) {
        DevConsoleModel.SearchResult result = findResult(entryName);
        panel.getSearchField().setText("edit " + result.entry().id());
        panel.runCommand();
    }

    @Then("the opened detail panel is shown")
    public void theOpenedDetailPanelIsShown() {
        assertTrue(panel.isProviderPanelShowing());
    }

    @When("the back action is triggered")
    public void theBackActionIsTriggered() {
        panel.showSearchView();
        playerDetailPanel = null;
    }

    @Then("the table includes editable rows {string}, {string}, {string}, {string}, {string}, {string}, {string}, {string}, {string}, {string}, {string}")
    public void theTableIncludesEditableRows(String r1, String r2, String r3, String r4, String r5,
                                             String r6, String r7, String r8, String r9, String r10, String r11) {
        capturePlayerDetailPanel();
        List<String> rowNames = List.of(r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11);
        for (int i = 0; i < rowNames.size(); i++) {
            assertEquals(rowNames.get(i), playerDetailPanel.getStatsTable().getRowCount() > i ?
                getRowFieldName(i) : "missing row " + i);
        }
    }

    @Then("the table includes read-only rows {string}, {string}")
    public void theTableIncludesReadOnlyRows(String r1, String r2) {
        capturePlayerDetailPanel();
        assertEquals(r1, getRowFieldName(11));
        assertEquals(r2, getRowFieldName(12));
    }

    @Given("the running player's {string} is {int}")
    public void theRunningPlayerSFieldIsValue(String fieldName, int value) {
        Stats stats = livePlayer.getPlayerInfo().getStats();
        switch (fieldName) {
            case "Strength" -> stats.setStrength(value);
            case "Dexterity" -> stats.setDexterity(value);
            case "Constitution" -> stats.setConstitution(value);
            case "Intelligence" -> stats.setIntelligence(value);
            case "Wisdom" -> stats.setWisdom(value);
            case "Luck" -> stats.setLuck(value);
            case "Max HP" -> stats.setMaxHp(value);
            case "Max Mana" -> stats.setMaxMana(value);
            case "Current HP" -> stats.setCurrentHp(value);
            case "Current Mana" -> stats.setCurrentMana(value);
        }
        refreshDisplayedRows();
    }

    @Then("the running player's {string} value is {int}")
    public void theRunningPlayerSFieldValueIsAsserted(String fieldName, int expectedValue) {
        Stats stats = livePlayer.getPlayerInfo().getStats();
        assertEquals(expectedValue, getStatValue(stats, fieldName),
            "Expected " + fieldName + " to be " + expectedValue);
    }

    @Then("the displayed {string} value is {int}")
    public void theDisplayedFieldValueIsAsserted(String fieldName, int expectedValue) {
        capturePlayerDetailPanel();
        for (int i = 0; i < playerDetailPanel.getStatsTable().getRowCount(); i++) {
            if (getRowFieldName(i).equals(fieldName)) {
                List<String> row = getRow(i);
                if (row != null && row.size() > 1) {
                    assertEquals(String.valueOf(expectedValue), row.get(1));
                }
                return;
            }
        }
        fail("Field not found: " + fieldName);
    }

    @Given("the running player's class is {string}")
    public void theRunningPlayerClassIs(String className) {
        com.swiftfaze.veil.entities.player.classes.PlayerClass cls = findPlayerClass(className);
        if (cls != null) {
            livePlayer.getPlayerInfo().setPlayerClass(cls);
        }
        refreshDisplayedRows();
    }

    @Then("the running player's class value is {string}")
    public void theRunningPlayerClassValueIsAsserted(String expectedClassName) {
        assertEquals(expectedClassName, livePlayer.getPlayerInfo().getPlayerClass().getName());
    }

    @Then("the running player's {string} is Mage's level-0 base strength")
    public void fieldIsMageLevelZeroBaseStrength(String fieldName) {
        com.swiftfaze.veil.entities.player.classes.PlayerClass mage = findPlayerClass("Mage");
        assertNotNull(mage, "Class not found: Mage");

        Stats baseStats = new Stats();
        mage.applyStatsAtLevel(baseStats, 0);

        int expectedValue = getStatValue(baseStats, fieldName);
        Stats actualStats = livePlayer.getPlayerInfo().getStats();
        int actualValue = getStatValue(actualStats, fieldName);

        assertEquals(expectedValue, actualValue,
            fieldName + " should match Mage's level-0 base value");
    }

    @Given("the running player's {string} has been changed from its class default")
    public void fieldHasBeenChangedFromDefault(String fieldName) {
        Stats stats = livePlayer.getPlayerInfo().getStats();
        switch (fieldName) {
            case "Strength" -> stats.setStrength(stats.getStrength() + 1);
            case "Max HP" -> stats.setMaxHp(stats.getMaxHp() + 1);
        }
        refreshDisplayedRows();
    }

    @Given("the running player's {string} has been edited away from its class default")
    public void fieldEditedAwayFromDefault(String fieldName) {
        Stats stats = livePlayer.getPlayerInfo().getStats();
        switch (fieldName) {
            case "Strength" -> stats.setStrength(Math.max(0, stats.getStrength() - 1));
        }
        lastEditedFieldName = fieldName;
        lastEditedFieldValue = getStatValue(stats, fieldName);
        refreshDisplayedRows();
    }

    @Then("the running player's {string} still reflects the edit")
    public void fieldStillReflectsTheEdit(String fieldName) {
        assertEquals(fieldName, lastEditedFieldName);
        Stats stats = livePlayer.getPlayerInfo().getStats();
        assertEquals(lastEditedFieldValue, getStatValue(stats, fieldName));
    }

    @When("the command bar is set to {string}")
    public void theCommandBarIsSetTo(String command) {
        statsSnapshot = snapshot(livePlayer.getPlayerInfo().getStats());
        classSnapshot = livePlayer.getPlayerInfo().getPlayerClass().getName();
        panel.getSearchField().setText(command);
        panel.runCommand();
    }

    @Then("the transcript's last line is a SUCCESS line for {string} set to {int}")
    public void transcriptsLastLineIsSuccessForFieldSetTo(String fieldName, int value) {
        var lastEntry = lastTranscriptEntry();
        assertEquals(TranscriptWidget.Level.SUCCESS, lastEntry.level());
        assertTrue(lastEntry.text().contains(fieldName));
        assertTrue(lastEntry.text().contains(String.valueOf(value)));
    }

    @Then("the transcript's last line is an ERROR line for {word} {string}")
    public void transcriptsLastLineIsErrorFor(String category, String token) {
        var lastEntry = lastTranscriptEntry();
        assertEquals(TranscriptWidget.Level.ERROR, lastEntry.level());
        assertTrue(lastEntry.text().contains(token), "Expected error text to mention: " + token);
    }

    @Then("the running player's {string} value is unchanged")
    public void theRunningPlayerSFieldValueIsUnchanged(String fieldName) {
        int before = getStatValue(statsSnapshot, fieldName);
        int after = getStatValue(livePlayer.getPlayerInfo().getStats(), fieldName);
        assertEquals(before, after, fieldName + " should be unchanged");
    }

    @Then("the running player's class value is unchanged")
    public void theRunningPlayerSClassValueIsUnchanged() {
        assertEquals(classSnapshot, livePlayer.getPlayerInfo().getPlayerClass().getName());
    }

    @Then("the running player's {string} is Warrior's level-0 base Strength")
    public void fieldIsWarriorLevelZeroBaseStrength(String fieldName) {
        assertFieldIsWarriorBase(fieldName);
    }

    @Then("the running player's {string} is Warrior's level-0 base Max HP")
    public void fieldIsWarriorLevelZeroBaseMaxHp(String fieldName) {
        assertFieldIsWarriorBase(fieldName);
    }

    private void assertFieldIsWarriorBase(String fieldName) {
        com.swiftfaze.veil.entities.player.classes.PlayerClass warrior = findPlayerClass("Warrior");
        assertNotNull(warrior, "Class not found: Warrior");
        Stats baseStats = new Stats();
        warrior.applyStatsAtLevel(baseStats, 0);
        int expectedValue = getStatValue(baseStats, fieldName);
        int actualValue = getStatValue(livePlayer.getPlayerInfo().getStats(), fieldName);
        assertEquals(expectedValue, actualValue, fieldName + " should match Warrior's level-0 base value");
    }

    @Given("the game's live player is the same object identity before and after opening the provider")
    public void theGameSLivePlayerIsSameIdentityBeforeAndAfter() {
        identityBeforeEdit = livePlayer;
        maxHpBeforeEdit = livePlayer.getPlayerInfo().getStats().getMaxHp();
    }

    @Then("the game's live player is still the same object identity")
    public void theGameSLivePlayerIsStillSameIdentity() {
        assertSame(identityBeforeEdit, livePlayer);
    }

    @Then("the game's live player's {string} reflects the edit")
    public void theGameSLivePlayerSFieldReflectsTheEdit(String fieldName) {
        if ("Max HP".equals(fieldName)) {
            assertEquals(maxHpBeforeEdit + 1, livePlayer.getPlayerInfo().getStats().getMaxHp());
        }
    }

    @When("{string} is armed")
    public void fieldIsArmed(String fieldName) {
        capturePlayerDetailPanel();
        selectRow(fieldName);
        fireAction(Keybindings.ACTION_MENU_CONFIRM);
    }

    @When("left is pressed")
    public void leftIsPressed() {
        fireAction(Keybindings.ACTION_MENU_LEFT);
    }

    @When("right is pressed")
    public void rightIsPressed() {
        fireAction(Keybindings.ACTION_MENU_RIGHT);
    }

    @When("{string} is decreased to {int}")
    public void fieldIsDecreasedTo(String fieldName, int target) {
        Stats stats = livePlayer.getPlayerInfo().getStats();
        int guard = 0;
        while (getStatValue(stats, fieldName) > target && guard < 1000) {
            fireAction(Keybindings.ACTION_MENU_LEFT);
            guard++;
        }
    }

    @When("{string} is opened again")
    public void isOpenedAgain(String entryName) {
        isOpened(entryName);
    }

    @Then("{string} cannot be armed")
    public void fieldCannotBeArmed(String fieldName) {
        // A real user can't navigate to a different row while one is still armed (Up/Down
        // are blocked in that state) - disarm first, same as Escape would, so this check
        // reflects reachable UI state instead of a test-only shortcut around that rule.
        fireAction(Keybindings.ACTION_MENU_CANCEL);
        Stats stats = livePlayer.getPlayerInfo().getStats();
        int before = getStatValue(stats, fieldName);
        selectRow(fieldName);
        fireAction(Keybindings.ACTION_MENU_CONFIRM);
        fireAction(Keybindings.ACTION_MENU_RIGHT);
        int after = getStatValue(stats, fieldName);
        assertEquals(before, after, fieldName + " should not be editable");
    }

    private void refreshDisplayedRows() {
        capturePlayerDetailPanel();
        if (playerDetailPanel != null) {
            playerDetailPanel.refreshAllRows();
        }
    }

    private void selectRow(String fieldName) {
        TableWidget<List<String>> table = playerDetailPanel.getStatsTable();
        int targetIndex = rowIndexOf(fieldName);
        table.moveToStart();
        for (int i = 0; i < targetIndex; i++) {
            table.moveDown();
        }
    }

    private void fireAction(String actionName) {
        Action action = playerDetailPanel.getActionMap().get(actionName);
        if (action != null) {
            action.actionPerformed(new ActionEvent(playerDetailPanel, ActionEvent.ACTION_PERFORMED, ""));
        }
    }

    private void fireCommandAction(String actionName) {
        Action action = panel.getSearchField().getActionMap().get(actionName);
        if (action != null) {
            action.actionPerformed(new ActionEvent(panel.getSearchField(), ActionEvent.ACTION_PERFORMED, ""));
        }
    }

    private int rowIndexOf(String fieldName) {
        return switch (fieldName) {
            case "Class" -> 0;
            case "Strength" -> 1;
            case "Dexterity" -> 2;
            case "Constitution" -> 3;
            case "Intelligence" -> 4;
            case "Wisdom" -> 5;
            case "Luck" -> 6;
            case "Max HP" -> 7;
            case "Max Mana" -> 8;
            case "Current HP" -> 9;
            case "Current Mana" -> 10;
            case "Attack Power" -> 11;
            case "Defense" -> 12;
            default -> throw new IllegalArgumentException("Unknown field: " + fieldName);
        };
    }

    private void capturePlayerDetailPanel() {
        if (playerDetailPanel == null && panel.isProviderPanelShowing()
                && panel.getOpenedProviderPanel() instanceof PlayerDetailPanel opened) {
            playerDetailPanel = opened;
        }
    }

    private String getRowFieldName(int rowIndex) {
        List<String> row = getRow(rowIndex);
        return row != null && !row.isEmpty() ? row.get(0) : "";
    }

    private List<String> getRow(int rowIndex) {
        TableWidget<List<String>> table = playerDetailPanel.getStatsTable();
        if (rowIndex >= table.getRowCount()) {
            return null;
        }
        table.moveToStart();
        for (int i = 0; i < rowIndex; i++) {
            table.moveDown();
        }
        return table.getSelectedRow();
    }

    private int getStatValue(Stats stats, String fieldName) {
        return switch (fieldName) {
            case "Strength" -> stats.getStrength();
            case "Dexterity" -> stats.getDexterity();
            case "Constitution" -> stats.getConstitution();
            case "Intelligence" -> stats.getIntelligence();
            case "Wisdom" -> stats.getWisdom();
            case "Luck" -> stats.getLuck();
            case "Max HP" -> stats.getMaxHp();
            case "Max Mana" -> stats.getMaxMana();
            case "Current HP" -> stats.getCurrentHp();
            case "Current Mana" -> stats.getCurrentMana();
            case "Attack Power" -> stats.getAttackPower();
            case "Defense" -> stats.getDefense();
            default -> 0;
        };
    }

    private TranscriptWidget.TranscriptEntry lastTranscriptEntry() {
        var entries = panel.getTranscript().entries();
        assertFalse(entries.isEmpty(), TRANSCRIPT_SHOULD_HAVE_ENTRIES);
        return entries.get(entries.size() - 1);
    }

    private Stats snapshot(Stats stats) {
        Stats copy = new Stats();
        copy.setStrength(stats.getStrength());
        copy.setDexterity(stats.getDexterity());
        copy.setConstitution(stats.getConstitution());
        copy.setIntelligence(stats.getIntelligence());
        copy.setWisdom(stats.getWisdom());
        copy.setLuck(stats.getLuck());
        copy.setMaxHp(stats.getMaxHp());
        copy.setMaxMana(stats.getMaxMana());
        copy.setCurrentHp(stats.getCurrentHp());
        copy.setCurrentMana(stats.getCurrentMana());
        return copy;
    }

    private com.swiftfaze.veil.entities.player.classes.PlayerClass findPlayerClass(String name) {
        ModRegistry mods = ModLoader.load(java.nio.file.Paths.get("mods"));
        return mods.getAllPlayerClasses().stream()
            .filter(cls -> name.equals(cls.getName()))
            .findFirst()
            .orElse(null);
    }

    private static void fail(String message) {
        throw new AssertionError(message);
    }

    private List<String> resultNames() {
        return model.filteredResults().stream().map(result -> result.entry().name()).toList();
    }

    private DevConsoleModel.SearchResult findResult(String name) {
        return model.filteredResults().stream()
                .filter(result -> result.entry().name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No result named: " + name));
    }

    private DevConsoleProvider providerFor(String name) {
        if (CLASSES_PROVIDER_NAME.equals(name)) {
            return new ClassSandboxProvider();
        }
        if (PLAYER_PROVIDER_NAME.equals(name)) {
            return new PlayerSandboxProvider(() -> livePlayer);
        }
        throw new IllegalArgumentException("Unknown provider: " + name);
    }
}
