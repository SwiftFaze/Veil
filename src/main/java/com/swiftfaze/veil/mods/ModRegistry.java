package com.swiftfaze.veil.mods;

import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.entities.player.classes.PlayerClass;
import com.swiftfaze.veil.entities.quests.Quest;
import com.swiftfaze.veil.world.Tile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModRegistry {
    public record RegistryMaps(
            Map<String, Building> buildingsById,
            Map<String, Tile> tilesById,
            Map<String, PlayerClass> classesById,
            Map<String, Item> itemsById,
            Map<String, Quest> questsById,
            Map<String, WidgetColorTheme> themesById
    ) {
    }
    private final Map<String, Building> buildingsById = new LinkedHashMap<>();
    private final Map<String, Tile> tilesById = new LinkedHashMap<>();
    private final Map<String, PlayerClass> classesById = new LinkedHashMap<>();
    private final Map<String, Item> itemsById = new LinkedHashMap<>();
    private final Map<String, Quest> questsById = new LinkedHashMap<>();
    private final Map<String, WidgetColorTheme> themesById = new LinkedHashMap<>();
    private final List<String> modLoadOrder;

    ModRegistry(RegistryMaps maps, List<String> modLoadOrder) {
        this.buildingsById.putAll(maps.buildingsById());
        this.tilesById.putAll(maps.tilesById());
        this.classesById.putAll(maps.classesById());
        this.itemsById.putAll(maps.itemsById());
        this.questsById.putAll(maps.questsById());
        this.themesById.putAll(maps.themesById());
        this.modLoadOrder = List.copyOf(modLoadOrder);
    }

    public Building getBuilding(String id) {
        return buildingsById.get(id);
    }

    public Tile getTile(String id) {
        return tilesById.get(id);
    }

    public List<Tile> getAllTiles() {
        return List.copyOf(tilesById.values());
    }

    public PlayerClass getPlayerClass(String id) {
        return classesById.get(id);
    }

    public List<PlayerClass> getAllPlayerClasses() {
        return List.copyOf(classesById.values());
    }

    public Item getItem(String id) {
        return itemsById.get(id);
    }

    public List<Item> getAllItems() {
        return List.copyOf(itemsById.values());
    }

    public Quest getQuest(String id) {
        return questsById.get(id);
    }

    public List<Quest> getAllQuests() {
        return List.copyOf(questsById.values());
    }

    public WidgetColorTheme getTheme(String id) {
        return themesById.get(id);
    }

    public List<WidgetColorTheme> getAllThemes() {
        return List.copyOf(themesById.values());
    }

    public List<String> getModLoadOrder() {
        return modLoadOrder;
    }
}
