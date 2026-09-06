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
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DevConsoleSteps {

    private DevConsoleModel model;
    private DevConsolePanel panel;
    private Player livePlayer;
    private PlayerDetailPanel playerDetailPanel;
    private Player identityBeforeEdit;
    private int maxHpBeforeEdit;
    private String lastEditedFieldName;
    private int lastEditedFieldValue;

    @Given("the dev console is running with the {string} provider registered")
    public void theDevConsoleIsRunningWithTheProviderRegistered(String providerName) {
        List<DevConsoleProvider> providers = List.of(providerFor(providerName));
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
        panel.getSearchField().setText(text);
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
        panel.getSearchField().setText(entryName);
        panel.confirmSelection();
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

    @When("opening the selection does nothing")
    public void openingTheSelectionDoesNothing() {
        panel.confirmSelection();
        assertFalse(panel.isProviderPanelShowing());
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
        if ("Classes".equals(name)) {
            return new ClassSandboxProvider();
        }
        throw new IllegalArgumentException("Unknown provider: " + name);
    }
}
