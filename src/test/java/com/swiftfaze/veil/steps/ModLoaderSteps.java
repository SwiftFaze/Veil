package com.swiftfaze.veil.steps;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.entities.player.Stats;
import com.swiftfaze.veil.entities.player.classes.PlayerClass;
import com.swiftfaze.veil.entities.quests.Quest;
import com.swiftfaze.veil.exceptions.ModLoadException;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.mods.WidgetColorTheme;
import com.swiftfaze.veil.ui.widget.WidgetTheme;
import com.swiftfaze.veil.world.Tile;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.awt.Color;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModLoaderSteps {

    private record BuildingFixture(String id, String overrides, String explicitTileId) {
    }

    private record TileFixture(String id, char symbol, int r, int g, int b, boolean walkable, String overrides) {
    }

    private record StatEntry(Integer base, String growthCalc) {
    }

    private record ClassFixture(String id, String name, Map<String, StatEntry> stats, String overrides) {
    }

    private record ItemEffectFixture(String type, String stat, String calc) {
    }

    private record ItemFixture(String id, String name, Character glyph, String type, String slot,
                                Integer baseDamageMin, Integer baseDamageMax,
                                List<ItemEffectFixture> effects, String overrides) {
    }

    private record QuestRewardFixture(String type, String itemId, Integer count, String calc) {
    }

    private record QuestFixture(String id, String name, String objectiveType, String target, Integer count,
                                 List<QuestRewardFixture> rewards, String overrides) {
    }

    private record ThemeColorFixture(int r, int g, int b) {
    }

    private record ThemeFixture(String id, Map<String, ThemeColorFixture> colors, String overrides) {
    }

    private static final Map<String, Integer> WARRIOR_STATS = Map.of(
            "strength", 15, "dexterity", 10, "constitution", 14, "intelligence", 6,
            "wisdom", 6, "luck", 8, "maxHp", 120, "maxMana", 20);

    private Path modsRoot;
    private final Map<String, List<String>> dependsOnByMod = new LinkedHashMap<>();
    private final Map<String, List<BuildingFixture>> buildingsByMod = new LinkedHashMap<>();
    private final Map<String, List<TileFixture>> tilesByMod = new LinkedHashMap<>();
    private final Map<String, List<ClassFixture>> classesByMod = new LinkedHashMap<>();
    private final Map<String, List<ItemFixture>> itemsByMod = new LinkedHashMap<>();
    private final Map<String, List<QuestFixture>> questsByMod = new LinkedHashMap<>();
    private final Map<String, List<ThemeFixture>> themesByMod = new LinkedHashMap<>();
    private String overriddenBuildingId;
    private String lastCheckedTileId;
    private String lastCheckedClassId;
    private String lastCheckedItemId;
    private String lastCheckedQuestId;
    private String lastCheckedThemeId;
    private String lastCheckedEntityKind;
    private boolean needsMarkerTiles;

    private ModRegistry registry;
    private ModLoadException thrown;
    private Map<String, Color> widgetThemeSnapshot;

    @Before
    public void createModsRoot() throws IOException {
        modsRoot = Files.createTempDirectory("veil-mods-test");
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

    // WidgetTheme's fields are mutable statics shared across the whole test JVM, so any
    // scenario that calls WidgetTheme.applyTheme() (proving the theme actually applies) must
    // not leak its mutation into unrelated tests that run later in the same fork.
    @Before
    public void snapshotWidgetTheme() {
        widgetThemeSnapshot = new LinkedHashMap<>();
        for (String key : WidgetColorTheme.REQUIRED_KEYS) {
            widgetThemeSnapshot.put(key, widgetThemeColor(key));
        }
    }

    @After
    public void restoreWidgetTheme() {
        WidgetTheme.applyTheme(new WidgetColorTheme("test:snapshot", widgetThemeSnapshot));
    }

    @Given("a mods directory containing the {string} mod with a building declaring id {string}")
    public void aModsDirectoryContainingTheModWithABuildingDeclaringId(String modId, String buildingId) {
        addBuilding(modId, buildingId, null, null);
    }

    @Given("the mods directory also contains mod {string} with a building declaring id {string}")
    public void theModsDirectoryAlsoContainsModWithABuildingDeclaringId(String modId, String buildingId) {
        addBuilding(modId, buildingId, null, null);
    }

    @Given("the mods directory also contains mod {string} with a building declaring id {string} and no {string} field")
    public void theModsDirectoryAlsoContainsModWithABuildingDeclaringIdAndNoField(String modId, String buildingId, String fieldName) {
        addBuilding(modId, buildingId, null, null);
    }

    @Given("the mods directory also contains mod {string} with a building declaring id {string} and an {string} field of {string}")
    public void theModsDirectoryAlsoContainsModWithABuildingDeclaringIdAndAnFieldOf(String modId, String buildingId, String fieldName, String overriddenId) {
        addBuilding(modId, buildingId, overriddenId, null);
        overriddenBuildingId = overriddenId;
    }

    @Given("mod {string} declares a {string} of {string}")
    public void modDeclaresADependsOnOf(String modId, String fieldName, String dependsOnId) {
        dependsOnByMod.computeIfAbsent(modId, k -> new ArrayList<>()).add(dependsOnId);
    }

    @Given("a mods directory containing mod {string} with a malformed building file")
    public void aModsDirectoryContainingModWithAMalformedBuildingFile(String modId) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir.resolve("buildings"));
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"" + modId + "\",\"dependsOn\":[]}");
        Files.writeString(modDir.resolve("buildings").resolve("broken.json"), "{ not valid json");
    }

    @Given("a mods directory containing mod {string} with a malformed mod.json file")
    public void aModsDirectoryContainingModWithAMalformedModJsonFile(String modId) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir);
        Files.writeString(modDir.resolve("mod.json"), "{ not valid json");
    }

    @Given("a mods directory containing the {string} mod with a tile declaring id {string}, symbol {string}, color \\({int}, {int}, {int}), and walkable {word}")
    public void aModsDirectoryContainingTheModWithATileDeclaringId(String modId, String tileId, String symbol,
                                                                    int r, int g, int b, String walkable) {
        addTile(modId, tileId, symbol.charAt(0), r, g, b, Boolean.parseBoolean(walkable), null);
    }

    @Given("the mods directory also contains mod {string} with a tile declaring id {string} and no {string} field")
    public void theModsDirectoryAlsoContainsModWithATileDeclaringIdAndNoField(String modId, String tileId, String fieldName) {
        addTile(modId, tileId, '?', 0, 0, 0, true, null);
    }

    @Given("the mods directory also contains mod {string} with a tile declaring id {string}, symbol {string}, color \\({int}, {int}, {int}), and walkable {word}, whose {string} field names {string}")
    public void theModsDirectoryAlsoContainsModWithATileDeclaringIdOverriding(String modId, String tileId, String symbol,
                                                                               int r, int g, int b, String walkable,
                                                                               String fieldName, String overriddenId) {
        addTile(modId, tileId, symbol.charAt(0), r, g, b, Boolean.parseBoolean(walkable), overriddenId);
    }

    @Given("the mods directory also contains a building declaring id {string} whose blueprint is a single tile {string}")
    public void theModsDirectoryAlsoContainsABuildingDeclaringIdWhoseBlueprintIsASingleTile(String buildingId, String tileId) {
        String modId = buildingId.split(":")[0];
        addBuilding(modId, buildingId, null, tileId);
    }

    @Given("a mods directory containing mod {string} with a malformed tile file")
    public void aModsDirectoryContainingModWithAMalformedTileFile(String modId) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir.resolve("tiles"));
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"" + modId + "\",\"dependsOn\":[]}");
        Files.writeString(modDir.resolve("tiles").resolve("broken.json"), "{ not valid json");
    }

    @Given("a mods directory containing the {string} mod with a theme declaring id {string} and all eleven widget colors")
    public void aModsDirectoryContainingTheModWithAThemeDeclaringIdAndAllElevenWidgetColors(String modId, String themeId) {
        addTheme(modId, themeId, defaultThemeColors(), null);
    }

    @Given("the mods directory also contains mod {string} with a theme declaring id {string} and all eleven widget colors")
    public void theModsDirectoryAlsoContainsModWithAThemeDeclaringIdAndAllElevenWidgetColors(String modId, String themeId) {
        addTheme(modId, themeId, defaultThemeColors(), null);
    }

    @Given("the mods directory also contains mod {string} with a theme declaring id {string} and no {string} field")
    public void theModsDirectoryAlsoContainsModWithAThemeDeclaringIdAndNoField(String modId, String themeId, String fieldName) {
        addTheme(modId, themeId, defaultThemeColors(), null);
    }

    @Given("the mods directory also contains mod {string} with a theme declaring id {string}, a {string} color of \\({int}, {int}, {int}), and the rest of the eleven widget colors, whose {string} field names {string}")
    public void theModsDirectoryAlsoContainsModWithAThemeOverridingOneColor(String modId, String themeId, String colorKey,
                                                                             int r, int g, int b,
                                                                             String fieldName, String overriddenId) {
        Map<String, ThemeColorFixture> colors = new LinkedHashMap<>(defaultThemeColors());
        colors.put(colorKey, new ThemeColorFixture(r, g, b));
        addTheme(modId, themeId, colors, overriddenId);
    }

    @Given("a mods directory containing mod {string} with a theme declaring id {string} that omits {string}")
    public void aModsDirectoryContainingModWithAThemeThatOmits(String modId, String themeId, String omittedKey) {
        Map<String, ThemeColorFixture> colors = new LinkedHashMap<>(defaultThemeColors());
        colors.remove(omittedKey);
        addTheme(modId, themeId, colors, null);
    }

    @Given("a mods directory containing mod {string} with a malformed theme file")
    public void aModsDirectoryContainingModWithAMalformedThemeFile(String modId) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir.resolve("themes"));
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"" + modId + "\",\"dependsOn\":[]}");
        Files.writeString(modDir.resolve("themes").resolve("broken.json"), "{ not valid json");
    }

    @When("the mods directory is loaded")
    public void theModsDirectoryIsLoaded() throws IOException {
        writeFixtures();
        try {
            registry = ModLoader.load(modsRoot);
        } catch (ModLoadException e) {
            thrown = e;
        }
    }

    @Then("a building with ID {string} is available")
    public void aBuildingWithIDIsAvailable(String id) {
        assertNotNull(registry, "loading did not complete: " + (thrown == null ? "unknown" : thrown.getMessage()));
        assertNotNull(registry.getBuilding(id), "expected building '" + id + "' to be loaded");
    }

    @Then("a tile with ID {string} is available")
    public void aTileWithIDIsAvailable(String id) {
        assertNotNull(registry, "loading did not complete: " + (thrown == null ? "unknown" : thrown.getMessage()));
        assertNotNull(registry.getTile(id), "expected tile '" + id + "' to be loaded");
        lastCheckedTileId = id;
    }

    @Then("its symbol is {string}")
    public void itsSymbolIs(String symbol) {
        assertEquals(symbol.charAt(0), registry.getTile(lastCheckedTileId).getSymbol());
    }

    @Then("its color is \\({int}, {int}, {int})")
    public void itsColorIs(int r, int g, int b) {
        assertEquals(new Color(r, g, b), registry.getTile(lastCheckedTileId).getColor());
    }

    @Then("it is walkable")
    public void itIsWalkable() {
        assertTrue(registry.getTile(lastCheckedTileId).isWalkable());
    }

    @Then("it is not walkable")
    public void itIsNotWalkable() {
        assertFalse(registry.getTile(lastCheckedTileId).isWalkable());
    }

    @Then("the building {string}'s blueprint at \\({int}, {int}) references tile {string}")
    public void theBuildingsBlueprintAtReferencesTile(String buildingId, int x, int y, String expectedTileId) {
        Tile[][] blueprint = registry.getBuilding(buildingId).getBlueprint();
        assertEquals(registry.getTile(expectedTileId), blueprint[y][x]);
    }

    @Then("loading fails with a ModLoadException naming the colliding ID {string} and both mods {string} and {string}")
    public void loadingFailsWithAModLoadExceptionNamingTheCollidingIDAndBothMods(String id, String modA, String modB) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        assertTrue(thrown.getMessage().contains(id), "expected message to name id: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains(modA), "expected message to name mod: " + modA);
        assertTrue(thrown.getMessage().contains(modB), "expected message to name mod: " + modB);
    }

    @Then("its blueprint matches the one from mod {string}, not {string}")
    public void itsBlueprintMatchesTheOneFromModNot(String overridingMod, String originalMod) {
        Tile[][] blueprint = registry.getBuilding(overriddenBuildingId).getBlueprint();
        assertEquals(registry.getTile("test:stone"), blueprint[0][0]);
    }

    @Then("mod {string} finishes loading before mod {string} starts loading")
    public void modFinishesLoadingBeforeModStartsLoading(String earlierMod, String laterMod) {
        List<String> order = registry.getModLoadOrder();
        assertTrue(order.indexOf(earlierMod) < order.indexOf(laterMod));
    }

    @Then("a ModLoadException is thrown wrapping the underlying cause")
    public void aModLoadExceptionIsThrownWrappingTheUnderlyingCause() {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        assertNotNull(thrown.getCause(), "expected the ModLoadException to wrap an underlying cause");
    }

    @Then("a theme with ID {string} is available")
    public void aThemeWithIDIsAvailable(String id) {
        assertNotNull(registry, "loading did not complete: " + (thrown == null ? "unknown" : thrown.getMessage()));
        assertNotNull(registry.getTheme(id), "expected theme '" + id + "' to be loaded");
        lastCheckedThemeId = id;
    }

    @Then("its {string} color is \\({int}, {int}, {int})")
    public void itsNamedColorIs(String colorKey, int r, int g, int b) {
        assertEquals(new Color(r, g, b), registry.getTheme(lastCheckedThemeId).color(colorKey));
    }

    @Then("WidgetTheme's colors match the {string} theme's colors exactly")
    public void widgetThemesColorsMatchTheThemesColorsExactly(String themeId) {
        assertWidgetThemeMatches(themeId);
    }

    @Then("WidgetTheme's colors still match the {string} theme's colors")
    public void widgetThemesColorsStillMatchTheThemesColors(String themeId) {
        assertWidgetThemeMatches(themeId);
    }

    @Then("loading fails with a ModLoadException naming the missing color key {string} and the file for theme {string}")
    public void loadingFailsWithAModLoadExceptionNamingTheMissingColorKeyAndTheFileForTheme(String colorKey, String themeId) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        assertTrue(thrown.getMessage().contains(colorKey), "expected message to name missing color key: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains(themeId), "expected message to name theme: " + thrown.getMessage());
    }

    private void assertWidgetThemeMatches(String themeId) {
        WidgetColorTheme theme = registry.getTheme(themeId);
        assertNotNull(theme, "expected theme '" + themeId + "' to be loaded");
        WidgetTheme.applyTheme(theme);
        for (String key : WidgetColorTheme.REQUIRED_KEYS) {
            assertEquals(theme.color(key), widgetThemeColor(key), "mismatch for key " + key);
        }
    }

    private Color widgetThemeColor(String key) {
        return switch (key) {
            case "SELECTED_HIGHLIGHT" -> WidgetTheme.SELECTED_HIGHLIGHT;
            case "SELECTED_TEXT" -> WidgetTheme.SELECTED_TEXT;
            case "NORMAL_TEXT" -> WidgetTheme.NORMAL_TEXT;
            case "DIMMED_TEXT" -> WidgetTheme.DIMMED_TEXT;
            case "BACKGROUND" -> WidgetTheme.BACKGROUND;
            case "INVALID_HIGHLIGHT" -> WidgetTheme.INVALID_HIGHLIGHT;
            case "VALID_HIGHLIGHT" -> WidgetTheme.VALID_HIGHLIGHT;
            case "TABLE_HEADER_BACKGROUND" -> WidgetTheme.TABLE_HEADER_BACKGROUND;
            case "BORDER" -> WidgetTheme.BORDER;
            case "SCROLLBAR_THUMB" -> WidgetTheme.SCROLLBAR_THUMB;
            case "ACCENT" -> WidgetTheme.ACCENT;
            case "WINDOW_BORDER" -> WidgetTheme.WINDOW_BORDER;
            default -> throw new IllegalArgumentException("Unknown WidgetTheme color key: " + key);
        };
    }

    @Given("a mods directory containing the {string} mod with a class declaring id {string}, name {string}, base strength {int}, dexterity {int}, constitution {int}, intelligence {int}, wisdom {int}, luck {int}, max HP {int}, and max mana {int}")
    public void aModsDirectoryContainingTheModWithAClassDeclaringId(String modId, String classId, String name,
                                                                   int strength, int dexterity, int constitution,
                                                                   int intelligence, int wisdom, int luck,
                                                                   int maxHp, int maxMana) {
        Map<String, StatEntry> stats = Map.of(
                "strength", new StatEntry(strength, null),
                "dexterity", new StatEntry(dexterity, null),
                "constitution", new StatEntry(constitution, null),
                "intelligence", new StatEntry(intelligence, null),
                "wisdom", new StatEntry(wisdom, null),
                "luck", new StatEntry(luck, null),
                "maxHp", new StatEntry(maxHp, null),
                "maxMana", new StatEntry(maxMana, null)
        );
        addClass(modId, classId, name, stats, null);
    }

    @Given("a mods directory containing the {string} mod with a class declaring id {string} with base {word} {int} and a {word} growth calc of {string}")
    public void aModsDirectoryContainingModWithAClassWithBaseAndGrowthCalc(String modId, String classId,
                                                                            String stat1Name, int base, String stat2Name, String growthCalc) {
        Map<String, StatEntry> stats = new LinkedHashMap<>();
        stats.put(stat1Name, new StatEntry(base, growthCalc));
        addClass(modId, classId, classId, stats, null);
    }

    @Given("a mods directory containing the {string} mod with a class declaring id {string} with base {word} {int} and no growth calc for {word}")
    public void aModsDirectoryContainingModWithAClassWithBaseAndNoGrowthCalc(String modId, String classId,
                                                                             String stat1Name, int base, String stat2Name) {
        Map<String, StatEntry> stats = new LinkedHashMap<>();
        stats.put(stat1Name, new StatEntry(base, null));
        addClass(modId, classId, classId, stats, null);
    }

    @Given("a mods directory containing the {string} mod with a class declaring id {string} with a growth calc for stat {string} of {string}")
    public void aModsDirectoryContainingModWithAClassWithGrowthCalc(String modId, String classId, String statName, String growthCalc) {
        Map<String, StatEntry> stats = new LinkedHashMap<>();
        stats.put(statName, new StatEntry(null, growthCalc));
        addClass(modId, classId, classId, stats, null);
    }

    @Given("the mods directory also contains mod {string} with a class declaring id {string} and no {string} field")
    public void theModsDirectoryAlsoContainsModWithAClassDeclaringIdAndNoField(String modId, String classId, String fieldName) {
        Map<String, StatEntry> stats = new LinkedHashMap<>();
        for (String statName : WARRIOR_STATS.keySet()) {
            Integer base = WARRIOR_STATS.get(statName);
            stats.put(statName, new StatEntry(base, null));
        }
        addClass(modId, classId, classId, stats, null);
    }

    @Given("the mods directory also contains mod {string} with a class declaring id {string}, name {string}, and the same base stats, whose {string} field names {string}")
    public void theModsDirectoryAlsoContainsModWithAClassOverriding(String modId, String classId, String name,
                                                                    String fieldName, String overriddenId) {
        Map<String, StatEntry> stats = new LinkedHashMap<>();
        for (String statName : WARRIOR_STATS.keySet()) {
            Integer base = WARRIOR_STATS.get(statName);
            stats.put(statName, new StatEntry(base, null));
        }
        addClass(modId, classId, name, stats, overriddenId);
    }

    @Given("a mods directory containing mod {string} with a malformed class file")
    public void aModsDirectoryContainingModWithAMalformedClassFile(String modId) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir.resolve("classes"));
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"" + modId + "\",\"dependsOn\":[]}");
        Files.writeString(modDir.resolve("classes").resolve("broken.json"), "{ not valid json");
    }

    @Given("a mods directory containing the {string} mod with an item declaring id {string}, name {string}, glyph {string}, type {string}, slot {string}, base damage min {int} and max {int}, and a {string} effect on stat {string} with calc {string}")
    public void aModsDirectoryContainingTheModWithAnItemDeclaringId(String modId, String itemId, String name, String glyph,
                                                                      String type, String slot, int baseDamageMin, int baseDamageMax,
                                                                      String effectType, String stat, String calc) {
        List<ItemEffectFixture> effects = List.of(new ItemEffectFixture(effectType, stat, calc));
        addItem(modId, itemId, name, glyph.charAt(0), type, slot, baseDamageMin, baseDamageMax, effects, null);
    }

    @Given("a mods directory containing the {string} mod with an item declaring id {string}, name {string}, glyph {string}, type {string}, slot {string}, base damage min {int} and max {int}, and no effects")
    public void aModsDirectoryContainingTheModWithAnItemDeclaringIdNoEffects(String modId, String itemId, String name, String glyph,
                                                                              String type, String slot, int baseDamageMin, int baseDamageMax) {
        addItem(modId, itemId, name, glyph.charAt(0), type, slot, baseDamageMin, baseDamageMax, null, null);
    }

    @Given("a mods directory containing the {string} mod with an item declaring id {string} with a {string} effect on stat {string} with calc {string}")
    public void aModsDirectoryContainingModWithAnItemWithEffect(String modId, String itemId, String effectType, String stat, String calc) {
        List<ItemEffectFixture> effects = List.of(new ItemEffectFixture(effectType, stat, calc));
        addItem(modId, itemId, null, null, null, null, null, null, effects, null);
    }

    @Given("the mods directory also contains mod {string} with an item declaring id {string} and no {string} field")
    public void theModsDirectoryAlsoContainsModWithAnItemDeclaringIdAndNoField(String modId, String itemId, String fieldName) {
        addItem(modId, itemId, null, null, null, null, null, null, null, null);
    }

    @Given("the mods directory also contains mod {string} with an item declaring id {string}, name {string}, and the same base damage and effects, whose {string} field names {string}")
    public void theModsDirectoryAlsoContainsModWithAnItemOverriding(String modId, String itemId, String name, String fieldName, String overriddenId) {
        List<ItemEffectFixture> effects = List.of(new ItemEffectFixture("stat_bonus", "strength", "level*1.5+2"));
        addItem(modId, itemId, name, '/', "weapon", "main_hand", 4, 9, effects, overriddenId);
    }

    @Given("a mods directory containing mod {string} with a malformed item file")
    public void aModsDirectoryContainingModWithAMalformedItemFile(String modId) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir.resolve("items"));
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"" + modId + "\",\"dependsOn\":[]}");
        Files.writeString(modDir.resolve("items").resolve("broken.json"), "{ not valid json");
    }

    @Given("a mods directory containing the {string} mod with a quest declaring id {string}, name {string}, objective type {string} on target {string} with count {int}, an item reward of {string} count {int}, and an xp reward with calc {string}")
    public void aModsDirectoryContainingTheModWithAQuestWithRewards(String modId, String questId, String name,
                                                                     String objectiveType, String target, int count,
                                                                     String itemId, int itemCount, String xpCalc) {
        List<QuestRewardFixture> rewards = List.of(
                new QuestRewardFixture("item", itemId, itemCount, null),
                new QuestRewardFixture("xp", null, null, xpCalc)
        );
        addQuest(modId, questId, name, objectiveType, target, count, rewards, null);
    }

    @Given("a mods directory containing the {string} mod with a quest declaring id {string}, name {string}, objective type {string} on target {string} with count {int}, and no rewards")
    public void aModsDirectoryContainingTheModWithAQuestNoRewards(String modId, String questId, String name,
                                                                    String objectiveType, String target, int count) {
        addQuest(modId, questId, name, objectiveType, target, count, List.of(), null);
    }

    @Given("a mods directory containing the {string} mod with a quest declaring id {string}, objective type {string} on target {string} with count {int}, and an item reward of {string} count {int}")
    public void aModsDirectoryContainingTheModWithAQuestNoName(String modId, String questId,
                                                               String objectiveType, String target, int count,
                                                               String itemId, int itemCount) {
        List<QuestRewardFixture> rewards = List.of(
                new QuestRewardFixture("item", itemId, itemCount, null)
        );
        addQuest(modId, questId, null, objectiveType, target, count, rewards, null);
    }

    @Given("a mods directory containing the {string} mod with a quest declaring id {string}, objective type {string} on target {string} with count {int}, and an xp reward with calc {string}")
    public void aModsDirectoryContainingTheModWithAQuestXpOnly(String modId, String questId,
                                                               String objectiveType, String target, int count,
                                                               String xpCalc) {
        List<QuestRewardFixture> rewards = List.of(
                new QuestRewardFixture("xp", null, null, xpCalc)
        );
        addQuest(modId, questId, null, objectiveType, target, count, rewards, null);
    }

    @Given("a mods directory containing the {string} mod with a quest declaring id {string}, objective type {string} on target {string} with count {int}, and no rewards")
    public void aModsDirectoryContainingTheModWithAQuestNoRewardsNoName(String modId, String questId,
                                                                         String objectiveType, String target, int count) {
        addQuest(modId, questId, null, objectiveType, target, count, List.of(), null);
    }

    @Given("the mods directory also contains the {string} mod with a quest declaring id {string}, name {string}, objective type {string} on target {string} with count {int}, an item reward of {string} count {int}, and an xp reward with calc {string}")
    public void theModsDirectoryAlsoContainsTheModWithAQuestWithRewards(String modId, String questId, String name,
                                                                         String objectiveType, String target, int count,
                                                                         String itemId, int itemCount, String xpCalc) {
        List<QuestRewardFixture> rewards = List.of(
                new QuestRewardFixture("item", itemId, itemCount, null),
                new QuestRewardFixture("xp", null, null, xpCalc)
        );
        addQuest(modId, questId, name, objectiveType, target, count, rewards, null);
    }

    @Given("the mods directory also contains mod {string} with a quest declaring id {string} and no {string} field")
    public void theModsDirectoryAlsoContainsModWithAQuestAndNoField(String modId, String questId, String fieldName) {
        addQuest(modId, questId, null, "kill", "core:goblin", 5, List.of(), null);
    }

    @Given("the mods directory also contains mod {string} with a quest declaring id {string}, name {string}, and the same objective and rewards, whose {string} field names {string}")
    public void theModsDirectoryAlsoContainsModWithAQuestOverriding(String modId, String questId, String name,
                                                                    String fieldName, String overriddenId) {
        addQuest(modId, questId, name, "kill", "core:goblin", 5, List.of(), overriddenId);
    }

    @Given("a mods directory containing mod {string} with a malformed quest file")
    public void aModsDirectoryContainingModWithAMalformedQuestFile(String modId) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir.resolve("quests"));
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"" + modId + "\",\"dependsOn\":[]}");
        Files.writeString(modDir.resolve("quests").resolve("broken.json"), "{ not valid json");
    }

    @Then("a class with ID {string} is available")
    public void aClassWithIDIsAvailable(String id) {
        assertNotNull(registry, "loading did not complete: " + (thrown == null ? "unknown" : thrown.getMessage()));
        assertNotNull(registry.getPlayerClass(id), "expected class '" + id + "' to be loaded");
        lastCheckedClassId = id;
        lastCheckedEntityKind = "class";
    }

    @Then("its name is {string}")
    public void itsNameIs(String expectedName) {
        if ("item".equals(lastCheckedEntityKind)) {
            assertEquals(expectedName, registry.getItem(lastCheckedItemId).getName());
        } else if ("quest".equals(lastCheckedEntityKind)) {
            assertEquals(expectedName, registry.getQuest(lastCheckedQuestId).getName());
        } else {
            assertEquals(expectedName, registry.getPlayerClass(lastCheckedClassId).getName());
        }
    }

    @Then("its base max HP is {int}")
    public void itsBaseMaxHpIs(int expected) {
        Stats stats = new Stats();
        registry.getPlayerClass(lastCheckedClassId).applyStatsAtLevel(stats, 0);
        assertEquals(expected, stats.getMaxHp());
    }

    @Then("its base max mana is {int}")
    public void itsBaseMaxManaIs(int expected) {
        Stats stats = new Stats();
        registry.getPlayerClass(lastCheckedClassId).applyStatsAtLevel(stats, 0);
        assertEquals(expected, stats.getMaxMana());
    }

    @Then("the class {string}'s {word} at level {int} is {int}")
    public void theClasssStatAtLevelIs(String classId, String statName, int level, int expected) {
        Stats stats = new Stats();
        registry.getPlayerClass(classId).applyStatsAtLevel(stats, level);
        int actual = getStatValue(stats, statName);
        assertEquals(expected, actual);
    }

    @Then("loading fails with a ModLoadException naming class {string}, stat {string}, and the file it came from")
    public void loadingFailsWithAModLoadExceptionNamingClassStatAndFile(String classId, String statName) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        assertTrue(thrown.getMessage().contains(classId), "expected message to name class: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains(statName), "expected message to name stat: " + thrown.getMessage());
    }

    @Then("an item with ID {string} is available")
    public void anItemWithIDIsAvailable(String id) {
        assertNotNull(registry, "loading did not complete: " + (thrown == null ? "unknown" : thrown.getMessage()));
        assertNotNull(registry.getItem(id), "expected item '" + id + "' to be loaded");
        lastCheckedItemId = id;
        lastCheckedEntityKind = "item";
    }

    @Then("its glyph is {string}")
    public void itsGlyphIs(String expected) {
        assertEquals(expected.charAt(0), registry.getItem(lastCheckedItemId).getGlyph());
    }

    @Then("its base damage is {int} to {int}")
    public void itsBaseDamageIs(int min, int max) {
        Item.BaseDamage baseDamage = registry.getItem(lastCheckedItemId).getBaseDamage();
        assertEquals(min, baseDamage.min());
        assertEquals(max, baseDamage.max());
    }

    @Then("it has one effect: a {string} on stat {string} with calc {string}")
    public void itHasOneEffect(String type, String stat, String calc) {
        List<Item.Effect> effects = registry.getItem(lastCheckedItemId).getEffects();
        assertEquals(1, effects.size());
        assertEquals(type, effects.get(0).type());
        assertEquals(stat, effects.get(0).stat());
        assertEquals(calc, effects.get(0).calc());
    }

    @Then("it has no effects")
    public void itHasNoEffects() {
        assertTrue(registry.getItem(lastCheckedItemId).getEffects().isEmpty());
    }

    @Then("loading fails with a ModLoadException naming item {string}, stat {string}, and the file it came from")
    public void loadingFailsWithAModLoadExceptionNamingItemStatAndFile(String itemId, String statName) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        assertTrue(thrown.getMessage().contains(itemId), "expected message to name item: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains(statName), "expected message to name stat: " + thrown.getMessage());
    }

    @Then("loading fails with a ModLoadException naming item {string} and the file it came from")
    public void loadingFailsWithAModLoadExceptionNamingItemAndFile(String itemId) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        assertTrue(thrown.getMessage().contains(itemId), "expected message to name item: " + thrown.getMessage());
    }

    @Then("a quest with ID {string} is available")
    public void aQuestWithIDIsAvailable(String id) {
        assertNotNull(registry, "loading did not complete: " + (thrown == null ? "unknown" : thrown.getMessage()));
        assertNotNull(registry.getQuest(id), "expected quest '" + id + "' to be loaded");
        lastCheckedQuestId = id;
        lastCheckedEntityKind = "quest";
    }

    @Then("its objective is a {string} on target {string} with count {int}")
    public void itsObjectiveIs(String type, String target, int count) {
        Quest.Objective objective = registry.getQuest(lastCheckedQuestId).getObjective();
        assertEquals(type, objective.type());
        assertEquals(target, objective.target());
        assertEquals(count, objective.count());
    }

    @Then("it has {int} rewards: an item reward of {string} count {int}, and an xp reward with calc {string}")
    public void itHasRewards(int expectedCount, String itemRewardId, int itemRewardCount, String xpCalc) {
        List<Quest.Reward> rewards = registry.getQuest(lastCheckedQuestId).getRewards();
        assertEquals(expectedCount, rewards.size());
        assertEquals("item", rewards.get(0).type());
        assertEquals(itemRewardId, rewards.get(0).id());
        assertEquals(itemRewardCount, rewards.get(0).count());
        assertEquals("xp", rewards.get(1).type());
        assertEquals(xpCalc, rewards.get(1).calc());
    }

    @Then("it has no rewards")
    public void itHasNoRewards() {
        assertTrue(registry.getQuest(lastCheckedQuestId).getRewards().isEmpty());
    }

    @Then("loading fails with a ModLoadException naming quest {string}, item {string}, and the file it came from")
    public void loadingFailsWithAModLoadExceptionNamingQuestItemAndFile(String questId, String itemId) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        assertTrue(thrown.getMessage().contains(questId), "expected message to name quest: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains(itemId), "expected message to name item: " + thrown.getMessage());
    }

    @Then("loading fails with a ModLoadException naming quest {string} and the file it came from")
    public void loadingFailsWithAModLoadExceptionNamingQuestAndFile(String questId) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        assertTrue(thrown.getMessage().contains(questId), "expected message to name quest: " + thrown.getMessage());
    }

    @Then("loading fails with a ModLoadException naming quest {string}, objective type {string}, and the file it came from")
    public void loadingFailsWithAModLoadExceptionNamingQuestObjectiveTypeAndFile(String questId, String objectiveType) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        assertTrue(thrown.getMessage().contains(questId), "expected message to name quest: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains(objectiveType), "expected message to name objective type: " + thrown.getMessage());
    }

    private int getStatValue(Stats stats, String statName) {
        Map<String, Function<Stats, Integer>> getters = Map.of(
                "strength", Stats::getStrength,
                "dexterity", Stats::getDexterity,
                "constitution", Stats::getConstitution,
                "intelligence", Stats::getIntelligence,
                "wisdom", Stats::getWisdom,
                "luck", Stats::getLuck,
                "maxHp", Stats::getMaxHp,
                "maxMana", Stats::getMaxMana
        );
        return getters.get(statName).apply(stats);
    }

    private void addClass(String modId, String classId, String name, Map<String, StatEntry> stats, String overrides) {
        dependsOnByMod.computeIfAbsent(modId, k -> new ArrayList<>());
        classesByMod.computeIfAbsent(modId, k -> new ArrayList<>())
                .add(new ClassFixture(classId, name, stats, overrides));
    }

    private void addItem(String modId, String itemId, String name, Character glyph, String type, String slot,
                          Integer baseDamageMin, Integer baseDamageMax, List<ItemEffectFixture> effects, String overrides) {
        dependsOnByMod.computeIfAbsent(modId, k -> new ArrayList<>());
        itemsByMod.computeIfAbsent(modId, k -> new ArrayList<>())
                .add(new ItemFixture(itemId, name, glyph, type, slot, baseDamageMin, baseDamageMax, effects, overrides));
    }

    private void addQuest(String modId, String questId, String name, String objectiveType, String target,
                           Integer count, List<QuestRewardFixture> rewards, String overrides) {
        dependsOnByMod.computeIfAbsent(modId, k -> new ArrayList<>());
        questsByMod.computeIfAbsent(modId, k -> new ArrayList<>())
                .add(new QuestFixture(questId, name, objectiveType, target, count, rewards, overrides));
    }

    private void addBuilding(String modId, String buildingId, String overriddenId, String explicitTileId) {
        dependsOnByMod.computeIfAbsent(modId, k -> new ArrayList<>());
        buildingsByMod.computeIfAbsent(modId, k -> new ArrayList<>())
                .add(new BuildingFixture(buildingId, overriddenId, explicitTileId));
        if (explicitTileId == null) {
            needsMarkerTiles = true;
        }
    }

    private void addTile(String modId, String tileId, char symbol, int r, int g, int b, boolean walkable, String overrides) {
        dependsOnByMod.computeIfAbsent(modId, k -> new ArrayList<>());
        tilesByMod.computeIfAbsent(modId, k -> new ArrayList<>())
                .add(new TileFixture(tileId, symbol, r, g, b, walkable, overrides));
    }

    private void addTheme(String modId, String themeId, Map<String, ThemeColorFixture> colors, String overrides) {
        dependsOnByMod.computeIfAbsent(modId, k -> new ArrayList<>());
        themesByMod.computeIfAbsent(modId, k -> new ArrayList<>())
                .add(new ThemeFixture(themeId, colors, overrides));
    }

    private static Map<String, ThemeColorFixture> defaultThemeColors() {
        Map<String, ThemeColorFixture> colors = new LinkedHashMap<>();
        colors.put("SELECTED_HIGHLIGHT", new ThemeColorFixture(1, 2, 3));
        colors.put("SELECTED_TEXT", new ThemeColorFixture(4, 5, 6));
        colors.put("NORMAL_TEXT", new ThemeColorFixture(7, 8, 9));
        colors.put("DIMMED_TEXT", new ThemeColorFixture(10, 11, 12));
        colors.put("BACKGROUND", new ThemeColorFixture(13, 14, 15));
        colors.put("INVALID_HIGHLIGHT", new ThemeColorFixture(16, 17, 18));
        colors.put("VALID_HIGHLIGHT", new ThemeColorFixture(19, 20, 21));
        colors.put("TABLE_HEADER_BACKGROUND", new ThemeColorFixture(22, 23, 24));
        colors.put("BORDER", new ThemeColorFixture(25, 26, 27));
        colors.put("SCROLLBAR_THUMB", new ThemeColorFixture(28, 29, 30));
        colors.put("ACCENT", new ThemeColorFixture(31, 32, 33));
        colors.put("WINDOW_BORDER", new ThemeColorFixture(34, 35, 36));
        return colors;
    }

    private void writeFixtures() throws IOException {
        Set<String> allMods = new LinkedHashSet<>();
        allMods.addAll(buildingsByMod.keySet());
        allMods.addAll(tilesByMod.keySet());
        allMods.addAll(classesByMod.keySet());
        allMods.addAll(itemsByMod.keySet());
        allMods.addAll(questsByMod.keySet());
        allMods.addAll(themesByMod.keySet());
        allMods.addAll(dependsOnByMod.keySet());

        if (needsMarkerTiles) {
            writeMarkerTiles();
        }

        if (!classesByMod.isEmpty() || !itemsByMod.isEmpty()) {
            writeStatsRegistryIfNeeded();
        }

        for (String modId : allMods) {
            Path modDir = modsRoot.resolve(modId);
            Files.createDirectories(modDir);
            writeManifest(modDir, modId);
            writeTiles(modDir, tilesByMod.getOrDefault(modId, List.of()));
            writeBuildings(modDir, buildingsByMod.getOrDefault(modId, List.of()));
            writeClasses(modDir, classesByMod.getOrDefault(modId, List.of()));
            writeItems(modDir, itemsByMod.getOrDefault(modId, List.of()));
            writeQuests(modDir, questsByMod.getOrDefault(modId, List.of()));
            writeThemes(modDir, themesByMod.getOrDefault(modId, List.of()));
        }
    }

    private void writeMarkerTiles() throws IOException {
        Path markerDir = modsRoot.resolve("_markers");
        Files.createDirectories(markerDir);
        Files.writeString(markerDir.resolve("mod.json"), "{\"id\":\"_markers\",\"dependsOn\":[]}");

        Path tilesDir = markerDir.resolve("tiles");
        Files.createDirectories(tilesDir);
        Files.writeString(tilesDir.resolve("test_grass.json"), tileJson("test:grass", ',', 0, 200, 0, true, null));
        Files.writeString(tilesDir.resolve("test_stone.json"), tileJson("test:stone", '#', 100, 100, 100, false, null));
    }

    private void writeManifest(Path modDir, String modId) throws IOException {
        JsonObject manifest = new JsonObject();
        manifest.addProperty("id", modId);
        JsonArray dependsOn = new JsonArray();
        dependsOnByMod.getOrDefault(modId, List.of()).forEach(dependsOn::add);
        manifest.add("dependsOn", dependsOn);
        Files.writeString(modDir.resolve("mod.json"), manifest.toString());
    }

    private void writeTiles(Path modDir, List<TileFixture> fixtures) throws IOException {
        if (fixtures.isEmpty()) {
            return;
        }
        Path tilesDir = modDir.resolve("tiles");
        Files.createDirectories(tilesDir);

        int i = 0;
        for (TileFixture fixture : fixtures) {
            String json = tileJson(fixture.id(), fixture.symbol(), fixture.r(), fixture.g(), fixture.b(),
                    fixture.walkable(), fixture.overrides());
            Files.writeString(tilesDir.resolve("tile_" + (i++) + ".json"), json);
        }
    }

    private String tileJson(String id, char symbol, int r, int g, int b, boolean walkable, String overrides) {
        JsonObject tile = new JsonObject();
        tile.addProperty("id", id);
        tile.addProperty("symbol", String.valueOf(symbol));
        JsonObject color = new JsonObject();
        color.addProperty("r", r);
        color.addProperty("g", g);
        color.addProperty("b", b);
        tile.add("color", color);
        tile.addProperty("walkable", walkable);
        if (overrides != null) {
            tile.addProperty("overrides", overrides);
        }
        return tile.toString();
    }

    private void writeBuildings(Path modDir, List<BuildingFixture> fixtures) throws IOException {
        if (fixtures.isEmpty()) {
            return;
        }
        Path buildingsDir = modDir.resolve("buildings");
        Files.createDirectories(buildingsDir);

        int i = 0;
        for (BuildingFixture fixture : fixtures) {
            String tileId = fixture.explicitTileId() != null
                    ? fixture.explicitTileId()
                    : (fixture.overrides() != null ? "test:stone" : "test:grass");

            JsonObject building = new JsonObject();
            building.addProperty("id", fixture.id());
            building.addProperty("name", fixture.id());
            building.addProperty("width", 1);
            building.addProperty("height", 1);

            JsonArray row = new JsonArray();
            row.add(tileId);
            JsonArray tiles = new JsonArray();
            tiles.add(row);
            building.add("tiles", tiles);

            if (fixture.overrides() != null) {
                building.addProperty("overrides", fixture.overrides());
            }

            Files.writeString(buildingsDir.resolve("building_" + (i++) + ".json"), building.toString());
        }
    }

    private void writeClasses(Path modDir, List<ClassFixture> fixtures) throws IOException {
        if (fixtures.isEmpty()) {
            return;
        }
        Path classesDir = modDir.resolve("classes");
        Files.createDirectories(classesDir);

        int i = 0;
        for (ClassFixture fixture : fixtures) {
            String json = classJson(fixture);
            Files.writeString(classesDir.resolve("class_" + (i++) + ".json"), json);
        }
    }

    private String classJson(ClassFixture fixture) {
        JsonObject classObj = new JsonObject();
        classObj.addProperty("id", fixture.id());
        classObj.addProperty("name", fixture.name());

        JsonObject stats = new JsonObject();
        for (Map.Entry<String, StatEntry> entry : fixture.stats().entrySet()) {
            JsonObject stat = new JsonObject();
            if (entry.getValue().base() != null) {
                stat.addProperty("base", entry.getValue().base());
            }
            if (entry.getValue().growthCalc() != null) {
                stat.addProperty("growth", entry.getValue().growthCalc());
            }
            stats.add(entry.getKey(), stat);
        }
        classObj.add("stats", stats);

        if (fixture.overrides() != null) {
            classObj.addProperty("overrides", fixture.overrides());
        }

        return classObj.toString();
    }

    private void writeItems(Path modDir, List<ItemFixture> fixtures) throws IOException {
        if (fixtures.isEmpty()) {
            return;
        }
        Path itemsDir = modDir.resolve("items");
        Files.createDirectories(itemsDir);

        int i = 0;
        for (ItemFixture fixture : fixtures) {
            Files.writeString(itemsDir.resolve("item_" + (i++) + ".json"), itemJson(fixture));
        }
    }

    private String itemJson(ItemFixture fixture) {
        JsonObject item = new JsonObject();
        item.addProperty("id", fixture.id());
        item.addProperty("name", fixture.name() != null ? fixture.name() : fixture.id());
        item.addProperty("glyph", fixture.glyph() != null ? String.valueOf(fixture.glyph()) : "?");
        item.addProperty("type", fixture.type() != null ? fixture.type() : "misc");
        item.addProperty("slot", fixture.slot() != null ? fixture.slot() : "none");

        JsonObject baseDamage = new JsonObject();
        baseDamage.addProperty("min", fixture.baseDamageMin() != null ? fixture.baseDamageMin() : 0);
        baseDamage.addProperty("max", fixture.baseDamageMax() != null ? fixture.baseDamageMax() : 0);
        item.add("baseDamage", baseDamage);

        if (fixture.effects() != null && !fixture.effects().isEmpty()) {
            JsonArray effects = new JsonArray();
            for (ItemEffectFixture effect : fixture.effects()) {
                JsonObject effectObj = new JsonObject();
                effectObj.addProperty("type", effect.type());
                effectObj.addProperty("stat", effect.stat());
                effectObj.addProperty("calc", effect.calc());
                effects.add(effectObj);
            }
            item.add("effects", effects);
        }

        if (fixture.overrides() != null) {
            item.addProperty("overrides", fixture.overrides());
        }

        return item.toString();
    }

    private void writeQuests(Path modDir, List<QuestFixture> fixtures) throws IOException {
        if (fixtures.isEmpty()) {
            return;
        }
        Path questsDir = modDir.resolve("quests");
        Files.createDirectories(questsDir);

        int i = 0;
        for (QuestFixture fixture : fixtures) {
            Files.writeString(questsDir.resolve("quest_" + (i++) + ".json"), questJson(fixture));
        }
    }

    private String questJson(QuestFixture fixture) {
        JsonObject quest = new JsonObject();
        quest.addProperty("id", fixture.id());
        quest.addProperty("name", fixture.name() != null ? fixture.name() : fixture.id());

        JsonObject objective = new JsonObject();
        objective.addProperty("type", fixture.objectiveType());
        if (fixture.target() != null) {
            objective.addProperty("target", fixture.target());
        }
        if (fixture.count() != null) {
            objective.addProperty("count", fixture.count());
        }
        quest.add("objective", objective);

        if (fixture.rewards() != null && !fixture.rewards().isEmpty()) {
            JsonArray rewards = new JsonArray();
            for (QuestRewardFixture reward : fixture.rewards()) {
                JsonObject rewardObj = new JsonObject();
                rewardObj.addProperty("type", reward.type());
                if (reward.itemId() != null) {
                    rewardObj.addProperty("id", reward.itemId());
                }
                if (reward.count() != null) {
                    rewardObj.addProperty("count", reward.count());
                }
                if (reward.calc() != null) {
                    rewardObj.addProperty("calc", reward.calc());
                }
                rewards.add(rewardObj);
            }
            quest.add("rewards", rewards);
        }

        if (fixture.overrides() != null) {
            quest.addProperty("overrides", fixture.overrides());
        }

        return quest.toString();
    }

    private void writeThemes(Path modDir, List<ThemeFixture> fixtures) throws IOException {
        if (fixtures.isEmpty()) {
            return;
        }
        Path themesDir = modDir.resolve("themes");
        Files.createDirectories(themesDir);

        int i = 0;
        for (ThemeFixture fixture : fixtures) {
            Files.writeString(themesDir.resolve("theme_" + (i++) + ".json"), themeJson(fixture));
        }
    }

    private String themeJson(ThemeFixture fixture) {
        JsonObject theme = new JsonObject();
        theme.addProperty("id", fixture.id());
        theme.add("colors", themeColorsJson(fixture.colors()));
        if (fixture.overrides() != null) {
            theme.addProperty("overrides", fixture.overrides());
        }
        return theme.toString();
    }

    private JsonObject themeColorsJson(Map<String, ThemeColorFixture> colors) {
        JsonObject colorsJson = new JsonObject();
        for (Map.Entry<String, ThemeColorFixture> entry : colors.entrySet()) {
            JsonObject color = new JsonObject();
            color.addProperty("r", entry.getValue().r());
            color.addProperty("g", entry.getValue().g());
            color.addProperty("b", entry.getValue().b());
            colorsJson.add(entry.getKey(), color);
        }
        return colorsJson;
    }

    private void writeStatsRegistryIfNeeded() throws IOException {
        Path coreDir = modsRoot.resolve("core");
        Files.createDirectories(coreDir);

        JsonObject registry = new JsonObject();
        JsonArray statNames = new JsonArray();
        statNames.add("strength");
        statNames.add("dexterity");
        statNames.add("constitution");
        statNames.add("intelligence");
        statNames.add("wisdom");
        statNames.add("luck");
        statNames.add("maxHp");
        statNames.add("maxMana");
        registry.add("stats", statNames);

        Files.writeString(coreDir.resolve("stats.json"), registry.toString());
    }
}
