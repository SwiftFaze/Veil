package com.swiftfaze.veil.mods;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.entities.player.classes.PlayerClass;
import com.swiftfaze.veil.entities.quests.Quest;
import com.swiftfaze.veil.exceptions.ModLoadException;
import com.swiftfaze.veil.world.Tile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ModLoader {
    public record RegistrationContext<T>(
            Map<String, T> registry,
            Map<String, String> owningModById,
            boolean overrides,
            String contentType
    ) {
    }

    private static final Logger logger = LoggerFactory.getLogger(ModLoader.class);

    private ModLoader() {
    }

    public static ModRegistry load(Path modsRoot) {
        List<ModManifest> loadOrder = orderByDependencies(readManifests(modsRoot));

        Map<String, Tile> tilesById = new LinkedHashMap<>();
        Map<String, String> owningTileModById = new LinkedHashMap<>();
        for (ModManifest manifest : loadOrder) {
            loadTiles(modsRoot, manifest, tilesById, owningTileModById);
        }

        Map<String, Building> buildingsById = new LinkedHashMap<>();
        Map<String, String> owningBuildingModById = new LinkedHashMap<>();
        Set<String> validStatNames = loadStatRegistry(modsRoot);

        Map<String, PlayerClass> classesById = new LinkedHashMap<>();
        Map<String, String> owningClassModById = new LinkedHashMap<>();
        Map<String, Item> itemsById = new LinkedHashMap<>();
        Map<String, String> owningItemModById = new LinkedHashMap<>();
        Map<String, Quest> questsById = new LinkedHashMap<>();
        Map<String, String> owningQuestModById = new LinkedHashMap<>();
        Map<String, WidgetColorTheme> themesById = new LinkedHashMap<>();
        Map<String, String> owningThemeModById = new LinkedHashMap<>();
        List<String> modLoadOrder = new ArrayList<>();
        for (ModManifest manifest : loadOrder) {
            modLoadOrder.add(manifest.id());
            loadBuildings(modsRoot, manifest, tilesById, buildingsById, owningBuildingModById);
            loadClasses(modsRoot, manifest, validStatNames, classesById, owningClassModById);
            loadItems(modsRoot, manifest, validStatNames, itemsById, owningItemModById);
            loadQuests(modsRoot, manifest, itemsById, questsById, owningQuestModById);
            loadThemes(modsRoot, manifest, themesById, owningThemeModById);
        }

        logger.info("Loaded {} mod(s) [{}]: {} tiles, {} buildings, {} classes, {} items, {} quests, {} themes",
                modLoadOrder.size(), String.join(", ", modLoadOrder), tilesById.size(), buildingsById.size(),
                classesById.size(), itemsById.size(), questsById.size(), themesById.size());

        ModRegistry.RegistryMaps maps = new ModRegistry.RegistryMaps(buildingsById, tilesById, classesById, itemsById, questsById, themesById);
        return new ModRegistry(maps, modLoadOrder);
    }

    private static List<ModManifest> readManifests(Path modsRoot) {
        List<ModManifest> manifests = new ArrayList<>();

        if (!Files.isDirectory(modsRoot)) {
            return manifests;
        }

        try (DirectoryStream<Path> modDirs = Files.newDirectoryStream(modsRoot, Files::isDirectory)) {
            for (Path modDir : modDirs) {
                Path manifestFile = modDir.resolve("mod.json");
                if (Files.exists(manifestFile)) {
                    manifests.add(readManifest(manifestFile));
                }
            }
        } catch (IOException e) {
            throw new ModLoadException("Failed to scan mods directory: " + modsRoot, e);
        }

        return manifests;
    }

    private static ModManifest readManifest(Path manifestFile) {
        try (Reader reader = Files.newBufferedReader(manifestFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String id = json.get("id").getAsString();

            List<String> dependsOn = new ArrayList<>();
            if (json.has("dependsOn")) {
                for (var element : json.getAsJsonArray("dependsOn")) {
                    dependsOn.add(element.getAsString());
                }
            }

            return new ModManifest(id, dependsOn);
        } catch (Exception e) {
            throw new ModLoadException("Failed to load mod manifest: " + manifestFile, e);
        }
    }

    private static List<ModManifest> orderByDependencies(List<ModManifest> manifests) {
        List<ModManifest> ordered = new ArrayList<>();
        List<ModManifest> remaining = new ArrayList<>(manifests);

        remaining.stream()
                .filter(m -> m.id().equals("core"))
                .findFirst()
                .ifPresent(core -> {
                    ordered.add(core);
                    remaining.remove(core);
                });

        while (!remaining.isEmpty()) {
            List<String> orderedIds = ordered.stream().map(ModManifest::id).toList();

            ModManifest next = remaining.stream()
                    .filter(m -> orderedIds.containsAll(m.dependsOn()))
                    .findFirst()
                    .orElseThrow(() -> new ModLoadException(
                            "Unresolved or cyclic mod dependency among: "
                                    + remaining.stream().map(ModManifest::id).toList()));

            ordered.add(next);
            remaining.remove(next);
        }

        return ordered;
    }

    private static void loadTiles(Path modsRoot, ModManifest manifest,
                                   Map<String, Tile> tilesById,
                                   Map<String, String> owningModById) {
        Path tilesDir = modsRoot.resolve(manifest.id()).resolve("tiles");
        if (!Files.isDirectory(tilesDir)) {
            return;
        }

        try (DirectoryStream<Path> files = Files.newDirectoryStream(tilesDir, "*.json")) {
            for (Path file : files) {
                loadTile(file, manifest.id(), tilesById, owningModById);
            }
        } catch (IOException e) {
            throw new ModLoadException("Failed to scan tiles for mod: " + manifest.id(), e);
        }
    }

    private static void loadTile(Path file, String modId,
                                  Map<String, Tile> tilesById,
                                  Map<String, String> owningModById) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String id = json.get("id").getAsString();
            char symbol = json.get("symbol").getAsString().charAt(0);
            Color color = readColor(json.getAsJsonObject("color"));
            boolean walkable = json.get("walkable").getAsBoolean();

            RegistrationContext<Tile> tileContext = new RegistrationContext<>(tilesById, owningModById, json.has("overrides"), "Tile");
            registerWithCollisionCheck(id, new Tile(id, symbol, color, walkable), modId, tileContext);
        } catch (ModLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new ModLoadException("Failed to load tile from file: " + file, e);
        }
    }

    private static Color readColor(JsonObject color) {
        return new Color(color.get("r").getAsInt(), color.get("g").getAsInt(), color.get("b").getAsInt());
    }

    private static void loadBuildings(Path modsRoot, ModManifest manifest,
                                       Map<String, Tile> tilesById,
                                       Map<String, Building> buildingsById,
                                       Map<String, String> owningModById) {
        Path buildingsDir = modsRoot.resolve(manifest.id()).resolve("buildings");
        if (!Files.isDirectory(buildingsDir)) {
            return;
        }

        try (DirectoryStream<Path> files = Files.newDirectoryStream(buildingsDir, "*.json")) {
            for (Path file : files) {
                loadBuilding(file, manifest.id(), tilesById, buildingsById, owningModById);
            }
        } catch (IOException e) {
            throw new ModLoadException("Failed to scan buildings for mod: " + manifest.id(), e);
        }
    }

    private static void loadBuilding(Path file, String modId,
                                      Map<String, Tile> tilesById,
                                      Map<String, Building> buildingsById,
                                      Map<String, String> owningModById) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String id = json.get("id").getAsString();
            Tile[][] blueprint = readBlueprint(json.getAsJsonArray("tiles"), tilesById, id);

            RegistrationContext<Building> buildingContext = new RegistrationContext<>(buildingsById, owningModById, json.has("overrides"), "Building");
            registerWithCollisionCheck(id, new Building(blueprint), modId, buildingContext);
        } catch (ModLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new ModLoadException("Failed to load building from file: " + file, e);
        }
    }

    private static Tile[][] readBlueprint(JsonArray rows, Map<String, Tile> tilesById, String buildingId) {
        int height = rows.size();
        int width = rows.get(0).getAsJsonArray().size();
        Tile[][] blueprint = new Tile[height][width];

        for (int y = 0; y < height; y++) {
            JsonArray row = rows.get(y).getAsJsonArray();
            for (int x = 0; x < width; x++) {
                String tileId = row.get(x).getAsString();
                Tile tile = tilesById.get(tileId);
                if (tile == null) {
                    throw new ModLoadException("Building '" + buildingId
                            + "' references unknown tile ID: " + tileId);
                }
                blueprint[y][x] = tile;
            }
        }

        return blueprint;
    }

    private static <T> void registerWithCollisionCheck(String id, T value, String modId,
                                                         RegistrationContext<T> context) {
        if (context.registry().containsKey(id) && !context.overrides()) {
            throw new ModLoadException(context.contentType() + " ID '" + id + "' from mod '" + modId
                    + "' collides with existing content from mod '" + context.owningModById().get(id)
                    + "'; add an \"overrides\" field to confirm this is intentional.");
        }

        if (context.registry().containsKey(id)) {
            logger.info("Mod '{}' overrides {} '{}' previously provided by mod '{}'",
                    modId, context.contentType().toLowerCase(), id, context.owningModById().get(id));
        }

        context.registry().put(id, value);
        context.owningModById().put(id, modId);
        logger.debug("Loaded {} '{}' from mod '{}'", context.contentType().toLowerCase(), id, modId);
    }

    private static Set<String> loadStatRegistry(Path modsRoot) {
        Path statsFile = modsRoot.resolve("core").resolve("stats.json");
        if (!Files.exists(statsFile)) {
            return Set.of();
        }

        try (Reader reader = Files.newBufferedReader(statsFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("stats")) {
                Set<String> result = new HashSet<>();
                for (var element : json.getAsJsonArray("stats")) {
                    result.add(element.getAsString());
                }
                return Set.copyOf(result);
            }
            return Set.of();
        } catch (IOException e) {
            throw new ModLoadException("Failed to load stat registry: " + statsFile, e);
        }
    }

    private static void loadClasses(Path modsRoot, ModManifest manifest,
                                     Set<String> validStatNames,
                                     Map<String, PlayerClass> classesById,
                                     Map<String, String> owningModById) {
        Path classesDir = modsRoot.resolve(manifest.id()).resolve("classes");
        if (!Files.isDirectory(classesDir)) {
            return;
        }

        try (DirectoryStream<Path> files = Files.newDirectoryStream(classesDir, "*.json")) {
            for (Path file : files) {
                loadClass(file, manifest.id(), validStatNames, classesById, owningModById);
            }
        } catch (IOException e) {
            throw new ModLoadException("Failed to scan classes for mod: " + manifest.id(), e);
        }
    }

    private static void loadClass(Path file, String modId,
                                   Set<String> validStatNames,
                                   Map<String, PlayerClass> classesById,
                                   Map<String, String> owningModById) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String id = json.get("id").getAsString();
            String name = json.get("name").getAsString();
            Map<String, PlayerClass.StatCurve> statsByName = parseClassStats(json, id, validStatNames, file);
            RegistrationContext<PlayerClass> classContext = new RegistrationContext<>(classesById, owningModById, json.has("overrides"), "PlayerClass");
            registerWithCollisionCheck(id, new PlayerClass(id, name, statsByName), modId, classContext);
        } catch (ModLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new ModLoadException("Failed to load class from file: " + file, e);
        }
    }

    private static Map<String, PlayerClass.StatCurve> parseClassStats(JsonObject json, String id, Set<String> validStatNames, Path file) {
        Map<String, PlayerClass.StatCurve> statsByName = new LinkedHashMap<>();
        if (json.has("stats")) {
            JsonObject statsObj = json.getAsJsonObject("stats");
            for (String statName : statsObj.keySet()) {
                if (!validStatNames.contains(statName)) {
                    throw new ModLoadException("Class '" + id + "' references unregistered stat '" + statName + "' in file: " + file);
                }
                JsonObject statObj = statsObj.getAsJsonObject(statName);
                int base = statObj.has("base") ? statObj.get("base").getAsInt() : 0;
                String growthCalc = statObj.has("growth") ? statObj.get("growth").getAsString() : null;
                statsByName.put(statName, new PlayerClass.StatCurve(base, growthCalc));
            }
        }
        return statsByName;
    }

    private static void loadItems(Path modsRoot, ModManifest manifest,
                                   Set<String> validStatNames,
                                   Map<String, Item> itemsById,
                                   Map<String, String> owningModById) {
        Path itemsDir = modsRoot.resolve(manifest.id()).resolve("items");
        if (!Files.isDirectory(itemsDir)) {
            return;
        }

        try (DirectoryStream<Path> files = Files.newDirectoryStream(itemsDir, "*.json")) {
            for (Path file : files) {
                loadItem(file, manifest.id(), validStatNames, itemsById, owningModById);
            }
        } catch (IOException e) {
            throw new ModLoadException("Failed to scan items for mod: " + manifest.id(), e);
        }
    }

    private static void loadItem(Path file, String modId,
                                  Set<String> validStatNames,
                                  Map<String, Item> itemsById,
                                  Map<String, String> owningModById) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String id = json.get("id").getAsString();
            String name = json.get("name").getAsString();
            char glyph = json.get("glyph").getAsString().charAt(0);
            String type = json.get("type").getAsString();
            String slot = json.get("slot").getAsString();
            Item.BaseDamage baseDamage = parseItemBaseDamage(json);
            List<Item.Effect> effects = parseItemEffects(json, id, validStatNames, file);
            Item.ItemAttributes attributes = new Item.ItemAttributes(glyph, type, slot, baseDamage, effects);
            RegistrationContext<Item> itemContext = new RegistrationContext<>(itemsById, owningModById, json.has("overrides"), "Item");
            registerWithCollisionCheck(id, new Item(id, name, attributes), modId, itemContext);
        } catch (ModLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new ModLoadException("Failed to load item from file: " + file, e);
        }
    }

    private static Item.BaseDamage parseItemBaseDamage(JsonObject json) {
        if (!json.has("baseDamage")) return new Item.BaseDamage(0, 0);
        JsonObject bd = json.getAsJsonObject("baseDamage");
        int min = bd.has("min") ? bd.get("min").getAsInt() : 0;
        int max = bd.has("max") ? bd.get("max").getAsInt() : 0;
        return new Item.BaseDamage(min, max);
    }

    private static List<Item.Effect> parseItemEffects(JsonObject json, String id, Set<String> validStatNames, Path file) {
        List<Item.Effect> effects = new ArrayList<>();
        if (!json.has("effects")) return effects;
        for (var element : json.getAsJsonArray("effects")) {
            JsonObject effectObj = element.getAsJsonObject();
            String effectType = effectObj.get("type").getAsString();
            String stat = effectObj.get("stat").getAsString();
            String calc = effectObj.get("calc").getAsString();
            if (!validStatNames.contains(stat)) throw new ModLoadException("Item '" + id + "' references unregistered stat '" + stat + "' in file: " + file);
            validateCalcExpression(calc, id, file);
            effects.add(new Item.Effect(effectType, stat, calc));
        }
        return effects;
    }

    private static void validateCalcExpression(String calc, String itemId, Path file) {
        try {
            CalcExpressionParser.evaluate(calc, 0);
        } catch (IllegalArgumentException e) {
            throw new ModLoadException("Item '" + itemId + "' has an invalid calc expression in file: " + file, e);
        }
    }

    private static final Set<String> SUPPORTED_QUEST_OBJECTIVE_TYPES = Set.of("kill");

    private static void loadQuests(Path modsRoot, ModManifest manifest,
                                    Map<String, Item> itemsById,
                                    Map<String, Quest> questsById,
                                    Map<String, String> owningModById) {
        Path questsDir = modsRoot.resolve(manifest.id()).resolve("quests");
        if (!Files.isDirectory(questsDir)) {
            return;
        }

        try (DirectoryStream<Path> files = Files.newDirectoryStream(questsDir, "*.json")) {
            for (Path file : files) {
                loadQuest(file, manifest.id(), itemsById, questsById, owningModById);
            }
        } catch (IOException e) {
            throw new ModLoadException("Failed to scan quests for mod: " + manifest.id(), e);
        }
    }

    private static void loadQuest(Path file, String modId,
                                   Map<String, Item> itemsById,
                                   Map<String, Quest> questsById,
                                   Map<String, String> owningModById) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String id = json.get("id").getAsString();
            String name = json.get("name").getAsString();
            Quest.Objective objective = readQuestObjective(json.getAsJsonObject("objective"), id, file);
            List<Quest.Reward> rewards = readQuestRewards(json, id, file, itemsById);

            RegistrationContext<Quest> questContext = new RegistrationContext<>(questsById, owningModById, json.has("overrides"), "Quest");
            registerWithCollisionCheck(id, new Quest(id, name, objective, rewards), modId, questContext);
        } catch (ModLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new ModLoadException("Failed to load quest from file: " + file, e);
        }
    }

    private static Quest.Objective readQuestObjective(JsonObject objectiveObj, String questId, Path file) {
        String type = objectiveObj.get("type").getAsString();
        if (!SUPPORTED_QUEST_OBJECTIVE_TYPES.contains(type)) {
            throw new ModLoadException("Quest '" + questId + "' has unsupported objective type '"
                    + type + "' in file: " + file);
        }
        String target = objectiveObj.has("target") ? objectiveObj.get("target").getAsString() : null;
        int count = objectiveObj.has("count") ? objectiveObj.get("count").getAsInt() : 0;
        return new Quest.Objective(type, target, count);
    }

    private static List<Quest.Reward> readQuestRewards(JsonObject json, String questId, Path file,
                                                         Map<String, Item> itemsById) {
        List<Quest.Reward> rewards = new ArrayList<>();
        if (!json.has("rewards")) {
            return rewards;
        }
        for (var element : json.getAsJsonArray("rewards")) {
            rewards.add(readQuestReward(element.getAsJsonObject(), questId, file, itemsById));
        }
        return rewards;
    }

    private static Quest.Reward readQuestReward(JsonObject rewardObj, String questId, Path file,
                                                  Map<String, Item> itemsById) {
        String type = rewardObj.get("type").getAsString();
        if ("item".equals(type)) return parseItemReward(rewardObj, questId, file, itemsById);
        if ("xp".equals(type)) return parseXpReward(rewardObj, questId, file);
        throw new ModLoadException("Quest '" + questId + "' has unsupported reward type '" + type + "' in file: " + file);
    }

    private static Quest.Reward parseItemReward(JsonObject rewardObj, String questId, Path file, Map<String, Item> itemsById) {
        String itemId = rewardObj.get("id").getAsString();
        int count = rewardObj.has("count") ? rewardObj.get("count").getAsInt() : 1;
        if (!itemsById.containsKey(itemId)) throw new ModLoadException("Quest '" + questId + "' references unknown item '" + itemId + "' in file: " + file);
        return new Quest.Reward("item", itemId, count, null);
    }

    private static Quest.Reward parseXpReward(JsonObject rewardObj, String questId, Path file) {
        String calc = rewardObj.get("calc").getAsString();
        try {
            CalcExpressionParser.evaluate(calc, 0);
        } catch (IllegalArgumentException e) {
            throw new ModLoadException("Quest '" + questId + "' has an invalid calc expression in file: " + file, e);
        }
        return new Quest.Reward("xp", null, null, calc);
    }

    private static void loadThemes(Path modsRoot, ModManifest manifest,
                                    Map<String, WidgetColorTheme> themesById,
                                    Map<String, String> owningModById) {
        Path themesDir = modsRoot.resolve(manifest.id()).resolve("themes");
        if (!Files.isDirectory(themesDir)) {
            return;
        }

        try (DirectoryStream<Path> files = Files.newDirectoryStream(themesDir, "*.json")) {
            for (Path file : files) {
                loadTheme(file, manifest.id(), themesById, owningModById);
            }
        } catch (IOException e) {
            throw new ModLoadException("Failed to scan themes for mod: " + manifest.id(), e);
        }
    }

    private static void loadTheme(Path file, String modId,
                                   Map<String, WidgetColorTheme> themesById,
                                   Map<String, String> owningModById) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            String id = json.get("id").getAsString();
            Map<String, Color> colorsByKey = readThemeColors(json.getAsJsonObject("colors"), id, file);

            RegistrationContext<WidgetColorTheme> themeContext = new RegistrationContext<>(themesById, owningModById, json.has("overrides"), "Theme");
            registerWithCollisionCheck(id, new WidgetColorTheme(id, colorsByKey), modId, themeContext);
        } catch (ModLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new ModLoadException("Failed to load theme from file: " + file, e);
        }
    }

    private static Map<String, Color> readThemeColors(JsonObject colorsJson, String themeId, Path file) {
        Map<String, Color> colorsByKey = new LinkedHashMap<>();
        for (String key : WidgetColorTheme.REQUIRED_KEYS) {
            if (colorsJson == null || !colorsJson.has(key)) {
                throw new ModLoadException("Theme '" + themeId + "' is missing required color key '"
                        + key + "' in file: " + file);
            }
            colorsByKey.put(key, readColor(colorsJson.getAsJsonObject(key)));
        }
        return colorsByKey;
    }

    private record ModManifest(String id, List<String> dependsOn) {
    }
}
