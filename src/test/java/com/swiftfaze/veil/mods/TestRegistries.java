package com.swiftfaze.veil.mods;

import com.swiftfaze.veil.entities.buildings.Building;

import java.util.List;
import java.util.Map;

/** Test-only registries holding hand-made content, built through the package-private constructor. */
public final class TestRegistries {

    private TestRegistries() {
    }

    /** A registry with exactly these buildings (by id) and no tiles, classes, items, quests or themes. */
    public static ModRegistry withBuildings(Map<String, Building> buildingsById) {
        ModRegistry.RegistryMaps maps = new ModRegistry.RegistryMaps(
                buildingsById, Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        return new ModRegistry(maps, List.of());
    }
}
